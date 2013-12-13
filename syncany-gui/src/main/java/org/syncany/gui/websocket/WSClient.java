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
import java.nio.channels.NotYetConnectedException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.syncany.gui.MainGUI;
import org.syncany.gui.util.JsonHelper;

/**
 * @author vincent
 *
 */
public class WSClient {
	private static final Logger log = Logger.getLogger(WSClient.class.getSimpleName());
	private static final String DEFAULT_WS_SERVER = "ws://localhost:8887";

	private static WSClient instance;

	private String location;
	private WebSocketClient client;

	public WSClient() throws URISyntaxException {
		this.location = DEFAULT_WS_SERVER;
	}
	
	public WSClient(String location) throws URISyntaxException {
		this.location = location;
	}
	
	private WebSocketClient createClient(String defaultlocation) throws URISyntaxException{
		Map<String, String> map = new HashMap<>();
		map.put("client_id", MainGUI.clientIdentification);
		
		return new WebSocketClient(new URI(defaultlocation), new Draft_17(), map, 3000) {
			@Override
			public void onOpen(ServerHandshake handshakedata) {
				log.fine("You are connected to ChatServer: " + getURI());
			}

			@Override
			public void onMessage(String message) {
				log.fine("got: " + message);
				handleReceivedMessage(message);
			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				log.fine("You have been disconnected from: " + getURI() + "; Code: " + code + " " + reason);
			}

			@Override
			public void onError(Exception ex) {
				log.fine("Exception occured ..." + ex.getMessage());
			}
		};
	}

	/**
	 * @return the client
	 */
	private WebSocketClient getClient() {
		if (client == null || !client.getConnection().isOpen()){
			try {
				client = createClient(this.location);
			}
			catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		return client;
	}
	
	public static void startWebSocketConnection() {
		instance().getClient().connect();
	}
	
	public static void stop(){
		instance().getClient().close();
	}

	public void handleReceivedMessage(String message) {
		Map<String, ?> parameters = JsonHelper.fromStringToMap(message);
		@SuppressWarnings("unused")
		String action = (String)parameters.get("action");
	}

	public void handleCommand(Map<String, String> parameters) {
		if (!getClient().getConnection().isOpen()){
			startWebSocketConnection();
		}
		
		try{
			String text = JsonHelper.fromMapToString(parameters);
			client.send(text);
		}
		catch (NotYetConnectedException e){
			log.warning("Not yet connected " + e.getMessage());
		}
		catch (Exception e){
			log.warning("Exception " + e.toString());
		}
	}

	public static WSClient instance() {
		if (instance == null)
			try {
				instance = new WSClient();
			}
			catch (URISyntaxException e) {
				e.printStackTrace();
			}
		return instance;
	}
}