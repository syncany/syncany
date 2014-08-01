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
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.syncany.config.to.DaemonConfigTO;
import org.syncany.operations.daemon.handlers.InternalRestHandler;
import org.syncany.operations.daemon.handlers.InternalWebSocketHandler;
import org.syncany.operations.daemon.messages.BadRequestResponse;
import org.syncany.operations.daemon.messages.GetFileResponse;
import org.syncany.operations.daemon.messages.GetFileResponseInternal;
import org.syncany.operations.daemon.messages.MessageFactory;
import org.syncany.operations.daemon.messages.Request;
import org.syncany.operations.daemon.messages.Response;
import org.syncany.plugins.Plugins;
import org.syncany.plugins.web.WebInterfacePlugin;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.Subscribe;

public class DaemonWebServer {
	private static final Logger logger = Logger.getLogger(DaemonWebServer.class.getSimpleName());
	public static final Pattern WEBSOCKET_ALLOWED_ORIGIN_HEADER = Pattern.compile("^https?://(localhost|127\\.\\d+\\.\\d+\\.\\d+):\\d+$");

	private Undertow webServer;
	private DaemonEventBus eventBus;

	private Cache<Integer, WebSocketChannel> requestIdWebSocketCache;
	private Cache<Integer, HttpServerExchange> requestIdRestSocketCache;
	private Cache<String, File> fileTokenTempFileCache;
	
	private List<WebSocketChannel> clientChannels;

	public DaemonWebServer(DaemonConfigTO daemonConfig) {
		this.clientChannels = new ArrayList<WebSocketChannel>();

		initCaches();
		initEventBus();
		initServer(daemonConfig.getWebServer().getHost(), daemonConfig.getWebServer().getPort());
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
	
	/**
	 * Initialization functions
	 */

	private void initCaches() {
		requestIdWebSocketCache = CacheBuilder.newBuilder().maximumSize(10000)
				.concurrencyLevel(2).expireAfterAccess(1, TimeUnit.MINUTES).build();
		
		requestIdRestSocketCache = CacheBuilder.newBuilder().maximumSize(10000)
				.concurrencyLevel(2).expireAfterAccess(1, TimeUnit.MINUTES).build();
		
		fileTokenTempFileCache = CacheBuilder.newBuilder().maximumSize(10000)
				.concurrencyLevel(2).expireAfterAccess(1, TimeUnit.MINUTES).build();
	}

	private void initEventBus() {
		eventBus = DaemonEventBus.getInstance();
		eventBus.register(this);
	}

	private void initServer(String host, int port) {
		webServer = Undertow
			.builder()
			.addHttpListener(port, host)
			.setHandler(path()
				.addPrefixPath("/api/ws", websocket(new InternalWebSocketHandler(this)))
				.addPrefixPath("/api/rs", new InternalRestHandler(this))
				.addPrefixPath("/", new InternalWebInterfaceHandler())
			).build();
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
	public void onGetFileResponseInternal(GetFileResponseInternal fileResponseInternal) {
		File tempFile = fileResponseInternal.getTempFile();
		GetFileResponse fileResponse = fileResponseInternal.getFileResponse();
		
		fileTokenTempFileCache.asMap().put(fileResponse.getTempToken(), tempFile);
		eventBus.post(fileResponse);
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

	/**
	 * ClientChannel access methods
	 */
	public void addClientChannel(WebSocketChannel clientChannel) {
		synchronized (clientChannels) {
			clientChannels.add(clientChannel);
		}
	}
	
	public void removeClientChannel(WebSocketChannel clientChannel) {
		synchronized (clientChannels) {
			clientChannels.remove(clientChannel);
		}
	}
	
	/**
	 * Cache access methods
	 */
	
	public void cacheRestRequest(int id, HttpServerExchange exchange) {
		synchronized (requestIdRestSocketCache) {
			requestIdRestSocketCache.put(id, exchange);	
		}
	}
	
	public void cacheWebSocketRequest(int id, WebSocketChannel clientSocket) {
		synchronized (requestIdWebSocketCache) {
			requestIdWebSocketCache.put(id, clientSocket);	
		}
	}
	
	public File fileTokenTempFileCacheGet(String fileToken) {
		 return fileTokenTempFileCache.asMap().get(fileToken);
	}


	
	
	public class InternalWebInterfaceHandler implements HttpHandler {
		private List<WebInterfacePlugin> webInterfacePlugins;
		private WebInterfacePlugin webInterfacePlugin;
		private HttpHandler requestHandler;
		
		public InternalWebInterfaceHandler() {
			webInterfacePlugins = Plugins.list(WebInterfacePlugin.class);
			
			if (webInterfacePlugins.size() == 1) {
				initWebInterfacePlugin();
			}
		}

		private void initWebInterfacePlugin() {
			try {				
				webInterfacePlugin = webInterfacePlugins.iterator().next();
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
			if (webInterfacePlugins.size() == 0) {
				exchange.getResponseSender().send("No web interface configured.");
			}
			else {
				exchange.getResponseSender().send("Only one web interface can be loaded.");
			}
		}
	}
}