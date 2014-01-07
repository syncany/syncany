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

import org.eclipse.swt.widgets.Shell;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.syncany.gui.MainGUI;
import org.syncany.gui.messaging.WSClient;
import org.syncany.gui.util.Listener;
import org.syncany.gui.util.StaticResourcesWebServer;
import org.syncany.util.JsonHelper;

/**
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class UnityTrayIcon extends TrayIcon {
	private static final Logger logger = Logger.getLogger(WSClient.class.getSimpleName());
	private static int PORT = 8882;
	
	private WebSocketServer webSocketClient;
	private StaticResourcesWebServer staticWebServer;
	private static Process unityProcess;
	
	public UnityTrayIcon(Shell shell) {
		super(shell);
		startWebServer();
	}
		
	private void startWebServer(){
		staticWebServer = new StaticResourcesWebServer();
		staticWebServer.startService(new Listener() {
			@Override
			public void update() {
				startTray();
			}
		});
	}
	
	private void startTray(){
		try {
			Map<String, String> map = new HashMap<>();
			map.put("client_id", MainGUI.getClientIdentification());

			this.webSocketClient = new WebSocketServer(new InetSocketAddress(PORT)) {
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
			startUnityProcess();
		}
		catch (Exception e) {
			throw new RuntimeException("Cannot instantiate Unity tray icon.", e);
		}
		
		makeSystemTrayStartSync();
	}

	@Override
	protected void quit() {
		try {
			staticWebServer.stopService();
			webSocketClient.stop();
		}
		catch (Exception e) {
			logger.warning("Exception while quitting application " + e);
		}

		super.quit();
	}
	
	protected void handleCommand(Map<String, Object> map) {
		String command = (String)map.get("command");
		
		switch (command){
			case "DONATE":
				showDonate();
				break;
			case "WEBSITE":
				showWebsite();
				break;
			case "QUIT":
				quit();
				break;
			case "PREFERENCES":
				showSettings();
				break;
			case "NEW":
				showWizard();
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
	
	private static void launchLoggerThread(final BufferedReader bf, final String prefix){
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				String line;
				try{
					while ((line = bf.readLine()) != null) {
			        	logger.info(prefix + line);
			        }
				}
				catch (Exception e){
					logger.warning("Exception " + e);
				}
			}
		});
		t.start();
	}
	
	private static void startUnityProcess() throws IOException{
		String baseUrl = "http://127.0.0.1:" + StaticResourcesWebServer.port + "/";
		String scriptUrl =  baseUrl + "scripts/unitytray.py";
		String[] command = new String[]{
			"python", 
			"-c", 
			"import urllib2;"
			+ "baseUrl = '" + baseUrl + "';"
			+ "wsUrl = 'ws://127.0.0.1:" + PORT + "';"
			+ "exec urllib2.urlopen('" + scriptUrl + "').read()" 
		};
		
		ProcessBuilder processBuilder = new ProcessBuilder(command);

		unityProcess = processBuilder.start();

		BufferedReader is = new BufferedReader(new InputStreamReader(unityProcess.getInputStream()));
		BufferedReader es = new BufferedReader(new InputStreamReader(unityProcess.getErrorStream()));
		
		launchLoggerThread(is, "PYTHON INPUT STREAM : ");
		launchLoggerThread(es, "PYTHON ERROR STREAM : ");
	}
	
	public static void main(String[] args) throws IOException {
		startUnityProcess();
	}
	
	public void sendTo(WebSocket ws, String text) {
		try{
			ws.send(text);
		}
		catch (Exception e){
			logger.warning("Exception " + e);
		}
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
	protected void setTrayImage(SyncanyTrayIcons image) {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("action", "update_tray_icon");
		parameters.put("imageFileName", image.getFileName());

		sendToAll(JsonHelper.fromMapToString(parameters));
	}
}
