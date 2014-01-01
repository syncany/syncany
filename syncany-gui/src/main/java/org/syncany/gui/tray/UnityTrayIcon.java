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
package org.syncany.gui.tray;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.syncany.gui.MainGUI;
import org.syncany.gui.messaging.WSClient;
import org.syncany.util.JsonHelper;

/**
 * @author pheckel
 *
 */
public class UnityTrayIcon implements TrayIcon {
	private static final Logger logger = Logger.getLogger(WSClient.class.getSimpleName());	
	private WebSocketClient webSocketClient;

	public UnityTrayIcon() {
		try {
			Map<String, String> map = new HashMap<>();
			map.put("client_id", MainGUI.getClientIdentification());
			
			this.webSocketClient = new WebSocketClient(new URI(WSClient.DEFAULT_WS_SERVER), new Draft_17(), map, 3000) {
				@Override
				public void onOpen(ServerHandshake handshakedata) {
					logger.fine("Connection to syncany daemon server: " + getURI());
				}

				@Override
				public void onMessage(String message) {
					logger.fine("Received by UnityTrayIcon: " + message);
				}

				@Override
				public void onClose(int code, String reason, boolean remote) {
					logger.fine(String.format("You have been disconnected from {0} for reaseon {1}", getURI(), reason));
				}

				@Override
				public void onError(Exception ex) {
					logger.fine("Exception occured ..." + ex.getMessage());
				}
			};
			
			webSocketClient.connect();

			new Thread(new Runnable() {
				@Override
				public void run() {				
					try {
						ProcessBuilder processBuilder = new ProcessBuilder("src/main/python/unitytray.py", "src/main/resources/images", "All folders in sync");
						processBuilder.start();					
					}
					catch (IOException e) {
						throw new RuntimeException("Unable to determine Linux desktop environment.", e);
					}
				}			
			}).start();		
		}
		catch (Exception e) {
			throw new RuntimeException("Cannot instantiate Unity tray icon.", e);
		}
	}

	@Override
	public void updateFolders(Map<String, Map<String, String>> folders) {
		webSocketClient.send(JsonHelper.fromMapToString(folders));		
	}

	@Override
	public void updateStatusText(String statusText) {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("action", "update_tray_status_text");
		parameters.put("text", statusText);
		
		webSocketClient.send(JsonHelper.fromMapToString(parameters));		
	}
}
