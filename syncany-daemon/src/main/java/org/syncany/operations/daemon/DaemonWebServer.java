/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.operations.daemon;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.websocket;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.WebInterfacePlugin;
import org.syncany.operations.daemon.messages.BadRequestResponse;
import org.syncany.operations.daemon.messages.MessageFactory;
import org.syncany.operations.daemon.messages.Request;
import org.syncany.operations.daemon.messages.Response;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.Subscribe;

public class DaemonWebServer {
	private static final Logger logger = Logger.getLogger(DaemonWebServer.class.getSimpleName());

	public static final String PLUGIN_FQCN_PREFIX = Plugin.class.getPackage().getName(); // TODO [high] Duplicate code, combine with plugins
	private static final String WEBSOCKET_ALLOWED_ORIGIN_HEADER = "localhost";
	private static final int DEFAULT_PORT = 8080;

	private static final Reflections reflections = new Reflections(PLUGIN_FQCN_PREFIX);

	private Undertow webServer;
	private DaemonEventBus eventBus;

	private Cache<Integer, WebSocketChannel> requestIdWebSocketCache;
	private Cache<Integer, HttpServerExchange> requestIdRestSocketCache;
	
	private List<WebSocketChannel> clientChannels;

	public DaemonWebServer() {
		this.clientChannels = new ArrayList<WebSocketChannel>();

		initCaches();
		initEventBus();
		initServer();
	}

	public void start() throws ServiceAlreadyStartedException {
		webServer.start();
	}

	public void stop() {
		try {
			webServer.stop();
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Could not stop websocket server.", e);
		}
	}

	private void initCaches() {
		requestIdWebSocketCache = CacheBuilder.newBuilder().maximumSize(10000)
				.concurrencyLevel(2).expireAfterAccess(1, TimeUnit.MINUTES).build();
		
		requestIdRestSocketCache = CacheBuilder.newBuilder().maximumSize(10000)
				.concurrencyLevel(2).expireAfterAccess(1, TimeUnit.MINUTES).build();
	}

	private void initEventBus() {
		eventBus = DaemonEventBus.getInstance();
		eventBus.register(this);
	}

	private void initServer() {
		webServer = Undertow
			.builder()
			.addHttpListener(DEFAULT_PORT, "localhost")
			.setHandler(path()
				.addPrefixPath("/api/ws", websocket(new InternalWebSocketHandler()))
				.addPrefixPath("/api/rs", new InternalRestHandler())
				.addPrefixPath("/", new InternalWebInterfaceHandler())
			).build();
	}

	private void handleWebSocketRequest(WebSocketChannel clientSocket, String message) {
		logger.log(Level.INFO, "Web socket message received: " + message);

		try {
			Request request = MessageFactory.createRequest(message);

			synchronized (requestIdWebSocketCache) {
				requestIdWebSocketCache.put(request.getId(), clientSocket);	
			}
			
			eventBus.post(request);
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Invalid request received; cannot serialize to Request.", e);
			eventBus.post(new BadRequestResponse(-1, "Invalid request."));
		}
	}
	
	private void handleRestRequest(HttpServerExchange exchange) throws IOException {
		exchange.startBlocking();			

		String message = IOUtils.toString(exchange.getInputStream());
		logger.log(Level.INFO, "REST message received: " + message);

		try {
			Request request = MessageFactory.createRequest(message);

			synchronized (requestIdRestSocketCache) {
				requestIdRestSocketCache.put(request.getId(), exchange);	
			}
			
			eventBus.post(request);
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Invalid request received; cannot serialize to Request.", e);
			eventBus.post(new BadRequestResponse(-1, "Invalid request."));
		}
	}

	private void sendBroadcast(String message) {
		synchronized (clientChannels) {
			for (WebSocketChannel clientChannel : clientChannels) {
				sendTo(clientChannel, message);
			}
		}
	}

	private void sendTo(WebSocketChannel clientChannel, String message) {
		logger.log(Level.INFO, "Sending message to " + clientChannel + ": " + message);
		WebSockets.sendText(message, clientChannel, null);
	}
	
	private void sendTo(HttpServerExchange serverExchange, String message) {
		logger.log(Level.INFO, "Sending message to " + serverExchange.getHostAndPort() + ": " + message);
		
		serverExchange.getResponseSender().send(message);
		serverExchange.endExchange();
	}

	@Subscribe
	public void onResponse(Response response) {
		try {
			// Serialize response
			String responseMessage = MessageFactory.toResponse(response);

			// Send to one or many receivers
			boolean responseWithoutRequest = response.getRequestId() == null || response.getRequestId() <= 0;

			if (responseWithoutRequest) {
				sendBroadcast(responseMessage);
			}
			else {
				HttpServerExchange responseServerExchange = requestIdRestSocketCache.asMap().get(response.getRequestId());
				WebSocketChannel responseToClientSocket = requestIdWebSocketCache.asMap().get(response.getRequestId());

				if (responseServerExchange != null) {
					sendTo(responseServerExchange, responseMessage);
				}				
				else if (responseToClientSocket != null) {
					sendTo(responseToClientSocket, responseMessage);
				}
				else {
					logger.log(Level.WARNING, "Cannot send message, because request ID in response is unknown or timed out." + responseMessage);
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private class InternalWebSocketHandler implements WebSocketConnectionCallback {
		@Override
		public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
			channel.getReceiveSetter().set(new AbstractReceiveListener() {
				@Override
				protected void onFullTextMessage(WebSocketChannel clientChannel, BufferedTextMessage message) {
					handleWebSocketRequest(clientChannel, message.getData());
				}

				@Override
				protected void onError(WebSocketChannel webSocketChannel, Throwable error) {
					logger.log(Level.INFO, "Server error : " + error.toString());
				}

				@Override
				protected void onClose(WebSocketChannel clientChannel, StreamSourceFrameChannel streamSourceChannel) throws IOException {
					logger.log(Level.INFO, clientChannel.toString() + " disconnected");

					synchronized (clientChannels) {
						clientChannels.remove(clientChannel);
					}
				}
			});

			synchronized (clientChannels) {
				clientChannels.add(channel);
			}

			channel.resumeReceives();
		}
	}

	private class InternalRestHandler implements HttpHandler {
		@Override
		public void handleRequest(final HttpServerExchange exchange) throws Exception {
			handleRestRequest(exchange);
		}
	}
	
	public class InternalWebInterfaceHandler implements HttpHandler {
		private Set<Class<? extends WebInterfacePlugin>> webInterfacePluginClasses;
		private WebInterfacePlugin webInterfacePlugin;
		private HttpHandler requestHandler;
		
		public InternalWebInterfaceHandler() {
			webInterfacePluginClasses = reflections.getSubTypesOf(WebInterfacePlugin.class);
			
			if (webInterfacePluginClasses.size() == 1) {
				initWebInterfacePlugin();
			}
		}

		private void initWebInterfacePlugin() {
			try {
				Class<? extends WebInterfacePlugin> webInterfacePluginClass = webInterfacePluginClasses.iterator().next();
				
				webInterfacePlugin = (WebInterfacePlugin) webInterfacePluginClass.newInstance();
				requestHandler = webInterfacePlugin.createRequestHandler();
				
				webInterfacePlugin.start();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void handleRequest(HttpServerExchange exchange) throws Exception {
			if (requestHandler != null) {
				handleRequestWithResourceHandler(exchange);
			}
			else {
				handleRequestNoHandler(exchange);
			}
		}

		private void handleRequestWithResourceHandler(HttpServerExchange exchange) throws Exception {
			requestHandler.handleRequest(exchange);
		}

		private void handleRequestNoHandler(HttpServerExchange exchange) {
			if (webInterfacePluginClasses.size() == 0) {
				exchange.getResponseSender().send("No web interface configured.");
			}
			else {
				exchange.getResponseSender().send("Only one web interface can be loaded.");
			}
		}
	}
}