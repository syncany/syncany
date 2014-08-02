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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.operations.daemon.LocalEventBus;
import org.syncany.operations.daemon.WebServer;
import org.syncany.operations.daemon.messages.BadRequestResponse;
import org.syncany.operations.daemon.messages.MessageFactory;
import org.syncany.operations.daemon.messages.Request;

/**
 * InternalWebSocketHandler handles the web socket requests 
 * sent to the daemon.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class InternalWebSocketHandler implements WebSocketConnectionCallback {
	private static final Logger logger = Logger.getLogger(InternalWebSocketHandler.class.getSimpleName());
	private WebServer daemonWebServer;
	private LocalEventBus eventBus;
	
	public InternalWebSocketHandler(WebServer daemonWebServer) {
		this.daemonWebServer = daemonWebServer;
		this.eventBus = LocalEventBus.getInstance();
		
		this.eventBus.register(this);
	}
	
	@Override
	public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
		logger.log(Level.INFO, "Connecting to websocket server.");
		
		// Validate origin header (security!)
		String originHeader = exchange.getRequestHeader("Origin");
		boolean allowedOriginHeader = (originHeader == null || 
				WebServer.WEBSOCKET_ALLOWED_ORIGIN_HEADER.matcher(originHeader).matches());
		
		if (!allowedOriginHeader) {
			logger.log(Level.INFO, channel.toString() + " disconnected due to invalid origin header: " + originHeader);
			exchange.close();
		}
		else {
			logger.log(Level.INFO, "Valid origin header, setting up connection.");
			
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
					daemonWebServer.removeClientChannel(clientChannel);
				}
			});

			daemonWebServer.addClientChannel(channel);

			channel.resumeReceives();
		}
	}
	
	private void handleWebSocketRequest(WebSocketChannel clientSocket, String message) {
		logger.log(Level.INFO, "Web socket message received: " + message);

		try {
			Request request = MessageFactory.createRequest(message);

			daemonWebServer.putCacheWebSocketRequest(request.getId(), clientSocket);
			
			eventBus.post(request);
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Invalid request received; cannot serialize to Request.", e);
			eventBus.post(new BadRequestResponse(-1, "Invalid request."));
		}
	}
}
