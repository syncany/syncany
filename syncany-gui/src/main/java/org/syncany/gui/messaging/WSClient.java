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
package org.syncany.gui.messaging;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.swt.widgets.Display;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;
import org.syncany.gui.Launcher;
import org.syncany.gui.MainGUI;
import org.syncany.util.JsonHelper;

/**
 * @author vincent
 *
 */
public class WSClient {
	private static final Logger log = Logger.getLogger(WSClient.class.getSimpleName());
	public static final String DEFAULT_WS_SERVER = "ws://localhost:8887";

	private String location;
	private WebSocketClient client;

	public WSClient() throws URISyntaxException {
		this(DEFAULT_WS_SERVER);
	}
	
	public WSClient(String location) throws URISyntaxException {
		this.location = location;
		this.client = createClient();
	}
	
	private WebSocketClient createClient() throws URISyntaxException{
		Map<String, String> map = new HashMap<>();
		map.put("client_id", MainGUI.getClientIdentification());
		
		return new WebSocketClient(new URI(location), new Draft_17(), map, 3000) {
			@Override
			public void onOpen(ServerHandshake handshakedata) {
				log.fine("Connection to syncany daemon server: " + getURI());
			}

			@Override
			public void onMessage(String message) {
				log.fine("Received: " + message);
				handleReceivedMessage(message);
			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				log.fine(String.format("You have been disconnected from {0} for reaseon {1}", getURI(), reason));
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
		return client;
	}
	
	public void startWebSocketConnection() {
		getClient().connect();
	}
	
	public void stop(){
		log.info("closing client");
		getClient().close();
	}

	@SuppressWarnings("unchecked")
	public void handleReceivedMessage(String message) {
		Map<String, ?> parameters = JsonHelper.fromStringToMap(message);
		String action = (String)parameters.get("action");
		
		switch (action){
			case "update_watched_folders":
				final Map<String, Map<String, String>> folders = (Map<String, Map<String, String>>)parameters.get("folders");
				
				Display.getDefault().asyncExec(new Runnable() {
			        public void run() {
			        	InterfaceUpdate iu = new InterfaceUpdate(folders);
			        	Launcher.getEventBus().post(iu);
		            }
			    });
				break;
		}
	}

	public void handleCommand(Map<String, ?> parameters) {
		try{
			String text = JsonHelper.fromMapToString(parameters);
			client.send(text);
		}
		catch (NotYetConnectedException e){
			log.warning("Not yet connected " + e.getMessage());
		}
		catch (WebsocketNotConnectedException e){
			log.warning("Websocket not connected");
		}
	}
}