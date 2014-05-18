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

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.operations.daemon.messages.BadRequestWebSocketResponse;
import org.syncany.operations.daemon.messages.WebSocketRequest;
import org.syncany.operations.daemon.messages.WebSocketRequestFactory;
import org.syncany.operations.daemon.messages.WebSocketResponse;

import com.google.common.eventbus.Subscribe;

public class DaemonWebSocketServer {
	private static final Logger logger = Logger.getLogger(DaemonWebSocketServer.class.getSimpleName());
	private static final String WEBSOCKET_ALLOWED_ORIGIN_HEADER = "localhost";
	private static final int DEFAULT_PORT = 8625;
	
	private final AtomicBoolean running = new AtomicBoolean(false);
	
	private WebSocketServer webSocketServer;
	private Serializer serializer; 
	private DaemonEventBus eventBus;
	
	public DaemonWebSocketServer() {
		this.serializer = new Persister();
		
		initEventBus();
		initWebSocketServer();
	}

	private void initEventBus() {
		eventBus = DaemonEventBus.getInstance();
		eventBus.register(this);
	}

	private void initWebSocketServer() {
		webSocketServer = new WebSocketServer(new InetSocketAddress(DEFAULT_PORT)) {
			@Override
			public void onOpen(WebSocket clientSocket, ClientHandshake handshake) {
				String clientAddress = clientSocket.getRemoteSocketAddress().toString();
				String clientOrigin = handshake.getFieldValue("origin");
				String clientId = handshake.getFieldValue("clientId");
				
				boolean isAllowedClient = clientOrigin == null || "null".equals(clientOrigin) ||
						clientOrigin.equals(WEBSOCKET_ALLOWED_ORIGIN_HEADER);
				
				if (!isAllowedClient) {
					logger.log(Level.WARNING, "Client " + clientAddress + " did not sent correct origin header. Origin: " + clientOrigin + ", ID: " + clientId);
					logger.log(Level.WARNING, "Disconnecting client " + clientAddress + ".");
					
					clientSocket.close();
					return;
				}
				
				logger.log(Level.INFO, "Client " + clientAddress + " connected. Origin: " + clientOrigin + ", ID: " + clientId);
			}
			
			@Override
			public void onMessage(WebSocket conn, String message) {
				logger.log(Level.INFO, "Received from "+conn.getRemoteSocketAddress().toString() + ": " + message);
				handleMessage(message);
			}
			
			@Override
			public void onError(WebSocket conn, Exception ex) {
				logger.log(Level.INFO, "Server error : " + ex.toString());
			}
			
			@Override
			public void onClose(WebSocket clientSocket, int code, String reason, boolean remote) {
				logger.log(Level.INFO, clientSocket.getRemoteSocketAddress().toString() + " disconnected");
			}
		};
	}

	public void handleMessage(String message) {
		logger.log(Level.INFO, "Web socket message received: " + message);
		
		int requestId = -1;
		String requestType = null;
		
		try {
			WebSocketRequest basicRequest = serializer.read(WebSocketRequest.class, message);
			
			requestType = basicRequest.getType();
			requestId = basicRequest.getId();
			
			Class<? extends WebSocketRequest> requestClass = WebSocketRequestFactory.getRequestClass(requestType);
			WebSocketRequest concreteRequest = serializer.read(requestClass, message);
			
			eventBus.post(concreteRequest);
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Invalid request received; cannot serialize to Request.", e);
			eventBus.post(new BadRequestWebSocketResponse(requestId, "Invalid request."));
		}	
	}

	/**
	 * Sends message to all currently connected WebSocket clients.
	 * 
	 * @param message The String to send across the network.
	 * @throws InterruptedException When socket related I/O errors occur.
	 */
	public void sendToAll(String message) {
		Collection<WebSocket> clientSockets = webSocketServer.connections();
		
		synchronized (clientSockets) {
			for (WebSocket clientSocket : clientSockets) {
				sendTo(clientSocket, message);
			}
		}
	}
	
	private void sendTo(WebSocket clientSocket, String message) {
		clientSocket.send(message);
	}

	public void start() throws ServiceAlreadyStartedException {
		webSocketServer.start();
		running.set(true);
	}

	public void stop() {
		try {
			webSocketServer.stop();
			running.set(false);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public boolean isRunning() {
		return running.get();
	}
	
	@Subscribe
	public void onResponse(WebSocketResponse response) {
		try {
			StringWriter responseWriter = new StringWriter();
			serializer.write(response, responseWriter);
			
			String responseMessage = responseWriter.toString();
			logger.log(Level.INFO, "Sending " + responseMessage);
			
			sendToAll(responseMessage);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}
}