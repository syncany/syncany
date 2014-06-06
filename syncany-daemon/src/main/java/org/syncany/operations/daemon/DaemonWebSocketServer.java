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

import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.InvalidFrameException;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.operations.daemon.messages.BadRequestResponse;
import org.syncany.operations.daemon.messages.BinaryResponse;
import org.syncany.operations.daemon.messages.Request;
import org.syncany.operations.daemon.messages.RequestFactory;
import org.syncany.operations.daemon.messages.Response;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.Subscribe;

public class DaemonWebSocketServer {
	private static final Logger logger = Logger.getLogger(DaemonWebSocketServer.class.getSimpleName());
	private static final String WEBSOCKET_ALLOWED_ORIGIN_HEADER = "localhost";
	private static final int DEFAULT_PORT = 8625;
	
	private WebSocketServer webSocketServer;
	private Serializer serializer; 
	private DaemonEventBus eventBus;
	private Cache<Integer, WebSocket> requestIdCache;
	
	public DaemonWebSocketServer() {
		this.serializer = new Persister();
		
		initCache();
		initEventBus();
		initWebSocketServer();
	}

	public void start() throws ServiceAlreadyStartedException {
		webSocketServer.start();
	}

	public void stop() {
		try {
			webSocketServer.stop();
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Could not stop websocket server.", e);
		}
	}

	private void initCache() {
		requestIdCache = CacheBuilder.newBuilder()
			.maximumSize(10000)
			.concurrencyLevel(2)
			.expireAfterAccess(1, TimeUnit.MINUTES)
			.build();
	}
	
	private void initEventBus() {
		eventBus = DaemonEventBus.getInstance();
		eventBus.register(this);
	}

	private void initWebSocketServer() {
		webSocketServer = new InternalWebSocketServer(new InetSocketAddress(DEFAULT_PORT));
	}

	private void handleMessage(WebSocket clientSocket, String message) {
		logger.log(Level.INFO, "Web socket message received: " + message);

		try {
			Request request = RequestFactory.createRequest(message);
			
			requestIdCache.put(request.getId(), clientSocket);
			eventBus.post(request);
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Invalid request received; cannot serialize to Request.", e);
			eventBus.post(new BadRequestResponse(-1, "Invalid request."));
		}	
	}

	private void sendBroadcast(String message) {
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
	
	@Subscribe
	public void onResponse(Response response) {
		try {			
			// Serialize response
			StringWriter responseWriter = new StringWriter();
			serializer.write(response, responseWriter);
			
			String responseMessage = responseWriter.toString();
			
			// Send to one or many receivers
			boolean responseWithoutRequest = response.getRequestId() == null || response.getRequestId() <= 0;

			if (responseWithoutRequest) {
				sendBroadcast(responseMessage);
			}
			else {
				WebSocket responseToClientSocket = requestIdCache.asMap().get(response.getRequestId());
				
				if (responseToClientSocket != null) {
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
	
	@Subscribe
	public void onResponse(final BinaryResponse response) {
		WebSocket responseToClientSocket = requestIdCache.asMap().get(response.getRequestId());
		
		if (responseToClientSocket != null) {
			logger.log(Level.INFO, "Sending binary frame to " + responseToClientSocket + "...");
			
			responseToClientSocket.sendFrame(new Framedata() {
				@Override
				public boolean isFin() {
					return true;
				}
				
				@Override
				public boolean getTransfereMasked() {
					return false;
				}
				
				@Override
				public ByteBuffer getPayloadData() {
					return response.getData();
				}
				
				@Override
				public Opcode getOpcode() {
					return Opcode.BINARY;
				}
				
				@Override
				public void append(Framedata nextframe) throws InvalidFrameException {
					// Nothing.
				}
			});
		}
		else {
			logger.log(Level.WARNING, "Cannot send BINARY message, because request ID in response is unknown or timed out.");
		}

	}
	
	private class InternalWebSocketServer extends WebSocketServer {
		public InternalWebSocketServer(InetSocketAddress address) {
			super(address);
		}

		@Override
		public void onOpen(WebSocket clientSocket, ClientHandshake handshake) {
			String clientAddress = clientSocket.getRemoteSocketAddress().toString();
			String clientOrigin = handshake.getFieldValue("origin");
			
			boolean isAllowedClient = clientOrigin == null || "null".equals(clientOrigin) ||
					clientOrigin.equals(WEBSOCKET_ALLOWED_ORIGIN_HEADER);
			
			if (!isAllowedClient) {
				logger.log(Level.WARNING, "Client " + clientAddress + " did not sent correct origin header. Origin: " + clientOrigin);
				logger.log(Level.WARNING, "Disconnecting client " + clientAddress + ".");
				
				clientSocket.close();
				return;
			}
			
			logger.log(Level.INFO, "Client " + clientAddress + " connected. Origin: " + clientOrigin);
		}
		
		@Override
		public void onMessage(WebSocket clientSocket, String message) {
			logger.log(Level.INFO, "Received from " + clientSocket.getRemoteSocketAddress().toString() + ": " + message);
			handleMessage(clientSocket, message);
		}
		
		@Override
		public void onError(WebSocket conn, Exception ex) {
			logger.log(Level.INFO, "Server error : " + ex.toString());
		}
		
		@Override
		public void onClose(WebSocket clientSocket, int code, String reason, boolean remote) {
			logger.log(Level.INFO, clientSocket.getRemoteSocketAddress().toString() + " disconnected");
		}
	}
}