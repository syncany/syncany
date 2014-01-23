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
package org.syncany.gui.messaging.websocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;
import org.syncany.gui.MainGUI;
import org.syncany.gui.messaging.DaemonMessagesHandler;
import org.syncany.util.JsonHelper;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class WebsocketClient {
	// Static fields
	private static final Logger log = Logger.getLogger(WebsocketClient.class.getSimpleName());
	private static final String DEFAULT_WS_SERVER = "ws://localhost:8887";

	// Instance fields
	private String uri;
	private WebSocketClient client;

	public WebsocketClient() throws URISyntaxException {
		this(DEFAULT_WS_SERVER);
	}
	
	public WebsocketClient(String uri) throws URISyntaxException {
		this.uri = uri;
		this.client = createClient();
	}
	
	private WebSocketClient createClient() throws URISyntaxException{
		Map<String, String> map = new HashMap<>();
		map.put("client_id", MainGUI.getClientIdentification());
		
		final DaemonMessagesHandler handler = new DaemonMessagesHandler();
		
		return new WebSocketClient(new URI(uri), new Draft_17(), map, 3000) {
			@Override
			public void onOpen(ServerHandshake handshakedata) {
				log.fine("Connection to syncany daemon server: " + getURI());
			}

			@Override
			public void onMessage(String message) {
				log.fine("Received: " + message);
				handler.handleReceivedMessage(message);
			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				log.fine(String.format("You have been disconnected from [%s] for reason [%s]", getURI(), reason));
			}

			@Override
			public void onError(Exception ex) {
				log.fine("Exception occured ..." + ex.getMessage());
			}
		};
	}

	public void startWebSocketConnection() {
		log.info("Starting Websocket connection");
		client.connect();
	}
	
	public void stop(){
		log.info("Closing Websocket connection");
		client.close();
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