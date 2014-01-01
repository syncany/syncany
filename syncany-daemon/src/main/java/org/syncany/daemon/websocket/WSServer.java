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
package org.syncany.daemon.websocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.logging.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.syncany.daemon.DaemonCommandHandler;

public class WSServer {
	private static final Logger log = Logger.getLogger(WSServer.class.getSimpleName());
	
	private static int DEFAULT_PORT = 8887;
	private static WSServer instance;
	
	private WebSocketServer delegate;
	
	public WSServer() {
		delegate = new WebSocketServer(new InetSocketAddress(DEFAULT_PORT)) {
			@Override
			public void onOpen(WebSocket conn, ClientHandshake handshake) {
				String id = handshake.getFieldValue("client_id");
				log.fine("Client with id '" + id + "' connected");
			}
			
			@Override
			public void onMessage(WebSocket conn, String message) {
				log.fine("Received from "+conn.getRemoteSocketAddress().toString() + ": " + message);
				DaemonCommandHandler.handle(message);
			}
			
			@Override
			public void onError(WebSocket conn, Exception ex) {
				log.fine("Server error : " + ex.toString());
			}
			
			@Override
			public void onClose(WebSocket conn, int code, String reason, boolean remote) {
				log.fine(conn.getRemoteSocketAddress().toString() + " disconnected");
			}
		};
	}

	/**
	 * Sends <var>text</var> to all currently connected WebSocket clients.
	 * 
	 * @param text
	 *            The String to send across the network.
	 * @throws InterruptedException
	 *             When socket related I/O errors occur.
	 */
	public static void sendToAll(String text) {
		Collection<WebSocket> con = getInstance().delegate.connections();
		synchronized (con) {
			for (WebSocket c : con) {
				sendTo(c, text);
			}
		}
	}
	
	public static void sendTo(WebSocket ws, String text) {
		ws.send(text);
	}

	public static void stop() {
		try {
			getInstance().delegate.stop();
		}
		catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void start() {
		getInstance().delegate.start();
	}
	
	/**
	 * @return the instance
	 */
	public static WSServer getInstance() {
		if (instance == null){
			instance = new WSServer();
		}
		return instance;
	}
}