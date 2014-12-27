/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.operations.daemon.handlers;

import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.syncany.config.LocalEventBus;
import org.syncany.operations.daemon.WebServer;
import org.syncany.operations.daemon.messages.BadRequestResponse;
import org.syncany.operations.daemon.messages.api.EventResponse;
import org.syncany.operations.daemon.messages.api.Message;
import org.syncany.operations.daemon.messages.api.MessageFactory;
import org.syncany.operations.daemon.messages.api.Request;

/**
 * InternalWebSocketHandler handles the web socket requests 
 * sent to the daemon.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class InternalWebSocketHandler implements WebSocketConnectionCallback {
	private static final Logger logger = Logger.getLogger(InternalWebSocketHandler.class.getSimpleName());
	private static final Pattern WEBSOCKET_ALLOWED_ORIGIN_HEADER = Pattern.compile("^(https?|wss?)://(localhost|127\\.\\d+\\.\\d+\\.\\d+):\\d+$");

	private WebServer daemonWebServer;
	private LocalEventBus eventBus;
	private String certificateCommonName;
	
	public InternalWebSocketHandler(WebServer daemonWebServer, String certificateCommonName) {
		this.daemonWebServer = daemonWebServer;
		this.eventBus = LocalEventBus.getInstance();
		this.certificateCommonName = certificateCommonName;
		
		this.eventBus.register(this);
	}
	
	@Override
	public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
		logger.log(Level.INFO, "Connecting to websocket server.");
		
		// Validate origin header (security!)
		String originHeader = exchange.getRequestHeader("Origin");
		
		if (!allowedOriginHeader(originHeader)) {
			logger.log(Level.INFO, channel.toString() + " disconnected due to invalid origin header: " + originHeader);
			exchange.close();
		}
		else {
			logger.log(Level.INFO, "Valid origin header, setting up connection.");
			
			channel.getReceiveSetter().set(new AbstractReceiveListener() {
				@Override
				protected void onFullTextMessage(WebSocketChannel clientChannel, BufferedTextMessage message) {
					handleMessage(clientChannel, message.getData());
				}

				@Override
				protected void onError(WebSocketChannel webSocketChannel, Throwable error) {
					logger.log(Level.INFO, "Server error : " + error.toString());
				}

				@Override
				protected void onClose(WebSocketChannel clientChannel, StreamSourceFrameChannel streamSourceChannel) throws IOException {
					logger.log(Level.INFO, clientChannel.toString() + " disconnected");
					daemonWebServer.removeClientChannel(clientChannel);
				}
			});

			daemonWebServer.addClientChannel(channel);

			channel.resumeReceives();
		}
	}
	
	private boolean allowedOriginHeader(String originHeader) {
		// Allow all non-browser clients (no "Origin:" header!)
		if (originHeader == null) {
			return true;
		}
		
		// Allow localhost's hostname
		try {
			if (originHeader.equals(InetAddress.getLocalHost().getHostName())) {
				return true;
			}
		}
		catch (Exception e) {
			// Continue trying.
		}
		
		// Allow by whitelist
		if (WEBSOCKET_ALLOWED_ORIGIN_HEADER.matcher(originHeader).matches()) {
			return true;
		}
		
		// Additional allowed origin header (certificate CN)
		if (certificateCommonName != null && originHeader.startsWith("https://" + certificateCommonName + ":")) {
			return true;
		}
		
		// Otherwise, we fail
		return false;
	}

	private void handleMessage(WebSocketChannel clientSocket, String messageStr) {
		logger.log(Level.INFO, "Web socket message received: " + messageStr);

		try {
			Message message = MessageFactory.toMessage(messageStr);
			
			if (message instanceof Request) {
				handleRequest(clientSocket, (Request) message);				
			}
			else if (message instanceof EventResponse) {
				handleEventResponse(clientSocket, (EventResponse) message);
			}
			else {
				throw new Exception("Invalid message type received: " + message.getClass());
			}
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Invalid message received; cannot serialize to Request.", e);
			eventBus.post(new BadRequestResponse(-1, "Invalid message."));
		}
	}
	

	private void handleRequest(WebSocketChannel clientSocket, Request request) {
		daemonWebServer.putCacheWebSocketRequest(request.getId(), clientSocket);			
		eventBus.post(request);
	}
	
	private void handleEventResponse(WebSocketChannel clientSocket, EventResponse eventResponse) {
		eventBus.post(eventResponse);
	}
}
