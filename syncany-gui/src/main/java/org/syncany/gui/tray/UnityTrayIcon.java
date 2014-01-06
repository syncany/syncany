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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.syncany.gui.MainGUI;
import org.syncany.gui.messaging.WSClient;
import org.syncany.gui.util.StaticResourcesWebServer;
import org.syncany.util.JsonHelper;

/**
 * @author pheckel
 *
 */
public class UnityTrayIcon extends TrayIcon {
	private static final Logger logger = Logger.getLogger(WSClient.class.getSimpleName());
	private WebSocketServer webSocketClient;

	public UnityTrayIcon() {
		new StaticResourcesWebServer().startService();
		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		try {
			Map<String, String> map = new HashMap<>();
			map.put("client_id", MainGUI.getClientIdentification());

			this.webSocketClient = new WebSocketServer(new InetSocketAddress(8882)) {
				@Override
				public void onOpen(WebSocket conn, ClientHandshake handshake) {
					String id = handshake.getFieldValue("client_id");
					logger.fine("Client with id '" + id + "' connected");
				}

				@Override
				public void onMessage(WebSocket conn, String message) {
					logger.fine("Unity Received from " + conn.getRemoteSocketAddress().toString() + ": " + message);
					handleCommand(JsonHelper.fromStringToMap(message));
				}

				@Override
				public void onError(WebSocket conn, Exception ex) {
					logger.fine("Server error : " + ex.toString());
				}

				@Override
				public void onClose(WebSocket conn, int code, String reason, boolean remote) {
					logger.fine(conn.getRemoteSocketAddress().toString() + " disconnected");
				}
			};

			webSocketClient.start();

			Thread unityPythonProcess = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						startUnityProcess();
					}
					catch (IOException e) {
						throw new RuntimeException("Unable to determine Linux desktop environment.", e);
					}
				}
			});
			unityPythonProcess.start();
		}
		catch (Exception e) {
			throw new RuntimeException("Cannot instantiate Unity tray icon.", e);
		}
	}

	protected void handleCommand(Map<String, Object> map) {
		String command = (String)map.get("command");
		
		switch (command){
			case "DONATE":
				showDonate();
				break;
		}
	}

	public void sendToAll(String text) {
		Collection<WebSocket> con = webSocketClient.connections();
		synchronized (con) {
			for (WebSocket c : con) {
				sendTo(c, text);
			}
		}
	}
	
	private static void startUnityProcess() throws IOException{
		String scriptUrl = "http://127.0.0.1:" + StaticResourcesWebServer.port + "/unitytray.py";
		String[] command1 = new String[]{"python", "src/main/resources/scripts/unitytray.py", "/src/main/resources/images", "coucou"};
		String[] command2 = new String[]{"python", "-c", "import urllib2;exec urllib2.urlopen('" + scriptUrl + "').read()"};
		
		ProcessBuilder processBuilder = new ProcessBuilder(command2);

		Process process = processBuilder.start();
		
		BufferedReader is = new BufferedReader(new InputStreamReader(process.getInputStream()));
		BufferedReader es = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		
		String ligne;

        while ((ligne = is.readLine()) != null) {
            System.out.println(ligne);
        }
        while ((ligne = es.readLine()) != null) {
            System.out.println(ligne);
        }
	}
	
	public static void main(String[] args) throws IOException {
		startUnityProcess();
	}
	
	public void sendTo(WebSocket ws, String text) {
		ws.send(text);
	}

	@Override
	public void updateFolders(Map<String, Map<String, String>> folders) {
		sendToAll(JsonHelper.fromMapToString(folders));
	}

	@Override
	public void updateStatusText(String statusText) {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("action", "update_tray_status_text");
		parameters.put("text", statusText);

		sendToAll(JsonHelper.fromMapToString(parameters));
	}

	@Override
	public void makeSystemTrayStartSync() {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("action", "start_syncing");

		sendToAll(JsonHelper.fromMapToString(parameters));
	}

	@Override
	public void makeSystemTrayStopSync() {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("action", "stop_syncing");

		sendToAll(JsonHelper.fromMapToString(parameters));
	}
}
