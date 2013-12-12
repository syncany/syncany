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
package org.syncany.gui.websocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.logging.Logger;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.syncany.gui.util.JsonHelper;

/**
 * @author vincent
 *
 */
public class WSClient {
	private static final Logger log = Logger.getLogger(WSClient.class.getSimpleName());
	private static WSClient instance;
	
	private WebSocketClient client;

	public WSClient(String defaultlocation) throws URISyntaxException {
		client = new WebSocketClient(new URI(defaultlocation), new Draft_17()) {
			@Override
			public void onOpen(ServerHandshake handshakedata) {
				log.fine("You are connected to ChatServer: " + getURI() + "\n");
			}

			@Override
			public void onMessage(String message) {
				log.fine("got: " + message);
				handleReceivedMessage(message);
			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				log.fine("You have been disconnected from: " + getURI() + "; Code: " + code + " " + reason + "\n");
			}

			@Override
			public void onError(Exception ex) {
				log.fine("Exception occured ..." + ex.getMessage());
			}
		};
	}

	public static void startWebSocketConnection() {
		instance().client.connect();
	}
	
	public static void stop(){
		instance().client.close();
	}

	public void handleReceivedMessage(String message) {
		Map<String, ?> parameters = JsonHelper.fromStringToMap(message);

		String action = (String)parameters.get("action");
	}

	public void handleCommand(Map<String, String> parameters) {
		String text = JsonHelper.fromMapToString(parameters);
		client.send(text);
	}

	public static WSClient instance() {
		if (instance == null)
			try {
				instance = new WSClient("ws://localhost:8887");
			}
			catch (URISyntaxException e) {
				e.printStackTrace();
			}
		return instance;
	}
}
