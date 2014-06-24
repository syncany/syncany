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
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.IOUtils;
import org.syncany.config.UserConfig;
import org.syncany.config.to.DaemonConfigTO;
import org.syncany.operations.daemon.auth.MapIdentityManager;
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
	private static final Pattern WEBSOCKET_ALLOWED_ORIGIN_HEADER = Pattern.compile("^https?://(localhost|127\\.\\d+\\.\\d+\\.\\d+):\\d+$");

	private Undertow webServer;
	private DaemonEventBus eventBus;

	private Cache<Integer, WebSocketChannel> requestIdWebSocketCache;
	private Cache<Integer, HttpServerExchange> requestIdRestSocketCache;
	private Cache<String, File> fileTokenTempFileCache;
	
	private List<WebSocketChannel> clientChannels;

	public DaemonWebServer(DaemonConfigTO daemonConfig) throws Exception {
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

	private void initServer(String host, int port) throws Exception {

        final Map<String, char[]> users = new HashMap<>(2);
        users.put("userOne", "passwordOne".toCharArray());
        users.put("userTwo", "passwordTwo".toCharArray());

        final IdentityManager identityManager = new MapIdentityManager(users);
        
        HttpHandler pathHttpHandler = path()
			.addPrefixPath("/api/ws", websocket(new InternalWebSocketHandler()))
			.addPrefixPath("/api/rs", new InternalRestHandler())
			.addPrefixPath("/", new InternalWebInterfaceHandler());
        
        HttpHandler securityPathHttpHandler = addSecurity(pathHttpHandler, identityManager);
        
        KeyStore userTrustStore = UserConfig.getUserTrustStore();
        KeyStore userKeyStore = getKeyStore();
        SSLContext sslContext = createSSLContext(userKeyStore, userTrustStore);
        
		webServer = Undertow
			.builder()
			.addHttpsListener(port, host, sslContext)
			.setHandler(securityPathHttpHandler)
			.build();
	}
	
	private KeyStore getKeyStore() {
		try {				
			File userTrustStoreFile = new File(UserConfig.getUserConfigDir(), "keystore.jks");
			KeyStore userKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
								
			if (userTrustStoreFile.exists()) {
				FileInputStream trustStoreInputStream = new FileInputStream(userTrustStoreFile); 		 		
				userKeyStore.load(trustStoreInputStream, new char[0]);
				
				trustStoreInputStream.close();
			}	
			else {
				userKeyStore.load(null, new char[0]); // Initialize empty store						
			}
			
			return userKeyStore;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static HttpHandler addSecurity(final HttpHandler toWrap, final IdentityManager identityManager) {
		HttpHandler handler = toWrap;
		handler = new AuthenticationCallHandler(handler);
		handler = new AuthenticationConstraintHandler(handler);
		final List<AuthenticationMechanism> mechanisms = Collections.<AuthenticationMechanism> singletonList(new BasicAuthenticationMechanism(
				"My Realm"));
		handler = new AuthenticationMechanismsHandler(handler, mechanisms);
		handler = new SecurityInitialHandler(AuthenticationMode.PRO_ACTIVE, identityManager, handler);
		return handler;
	}
	
	private static SSLContext createSSLContext(KeyStore keyStore, KeyStore trustStore) throws Exception {
		try {
			// Server key and certificate
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, new char[0]);

			KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

			// Trusted certificates
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(trustStore);
			
			TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

			// Create SSL context
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagers, trustManagers, null);

			return sslContext;
		}
		catch (Exception e) {
			throw new Exception("Unable to initialize SSL context", e);
		}
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
		logger.log(Level.INFO, "HTTP request received:" + exchange.getRelativePath());

		exchange.startBlocking();			

		if (exchange.getRelativePath().startsWith("/file/")) {	
			String tempFileToken = exchange.getRelativePath().substring("/file/".length());
			File tempFile = fileTokenTempFileCache.asMap().get(tempFileToken);
			
			logger.log(Level.INFO, "- Temp file: " + tempFileToken);
			
			IOUtils.copy(new FileInputStream(tempFile), exchange.getOutputStream());
			
			exchange.endExchange();
		}
		else {	
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

	private class InternalWebSocketHandler implements WebSocketConnectionCallback {
		@Override
		public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
			// Validate origin header (security!)
			String originHeader = exchange.getRequestHeader("Origin");
			boolean allowedOriginHeader = originHeader == null || WEBSOCKET_ALLOWED_ORIGIN_HEADER.matcher(originHeader).matches();
			
			if (!allowedOriginHeader) {
				logger.log(Level.INFO, channel.toString() + " disconnected due to invalid origin header: " + originHeader);
				exchange.close();
			}
			else {
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
	}

	private class InternalRestHandler implements HttpHandler {
		@Override
		public void handleRequest(final HttpServerExchange exchange) throws Exception {
			handleRestRequest(exchange);
		}
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