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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.swt.widgets.Shell;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.syncany.gui.messaging.webserver.StaticResourcesWebServer;
import org.syncany.gui.messaging.webserver.StaticResourcesWebServer.ServerStartedListener;
import org.syncany.gui.messaging.websocket.WebsocketClient;

import com.google.common.reflect.TypeToken;

/**
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class UnityTrayIcon extends TrayIcon {
	private static final Logger logger = Logger.getLogger(WebsocketClient.class.getSimpleName());
	private static int WEBSOCKET_SERVER_PORT = 51600;

	private WebSocketServer webSocketClient;
	private StaticResourcesWebServer staticWebServer;
	private static Process unityProcess;

	public UnityTrayIcon(Shell shell) {
		super(shell);
		
		startWebSocketServer();
		startWebServer();
	}

	private void startWebSocketServer() {
		try {
			this.webSocketClient = new WebSocketServer(new InetSocketAddress(WEBSOCKET_SERVER_PORT)) {
				@Override
				public void onOpen(WebSocket conn, ClientHandshake handshake) {
					String id = handshake.getFieldValue("client_id");
					logger.fine("Client with id '" + id + "' connected");
				}

				@Override
				public void onMessage(WebSocket conn, String message) {
					logger.fine("Unity Received from " + conn.getRemoteSocketAddress().toString() + ": " + message);
					Type type = new TypeToken<Map<String, Object>>() {}.getType();
					//handleCommand((Map<String, Object>)e));
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
		}
		catch (Exception e) {
			throw new RuntimeException("Cannot instantiate Unity tray icon.", e);
		}
	}

	private void startWebServer() {
		staticWebServer = new StaticResourcesWebServer();
		staticWebServer.startService(new ServerStartedListener() {
			@Override
			public void serverStarted() {
				startTray();
			}
		});
	}

	private void startTray() {
		try {
			startUnityProcess();
		}
		catch (IOException e) {
			throw new RuntimeException("Cannot start Python process for Unity Tray Icon.", e);
		}
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
		String command = (String) map.get("action");

		switch (command) {
		case "tray_menu_clicked_new":
			showWizard();
			break;
		case "tray_menu_clicked_preferences":
			showSettings();
			break;
		case "tray_menu_clicked_folder":
			showFolder(new File((String) map.get("folder")));
			break;
		case "tray_menu_clicked_donate":
			showDonate();
			break;
		case "tray_menu_clicked_website":
			showWebsite();
			break;
		case "tray_menu_clicked_quit":
			quit();
			break;
		}
	}

	public void sendToAll(String message) {		
		Collection<WebSocket> webSocketConnections = webSocketClient.connections();
		
		synchronized (webSocketConnections) {
			for (WebSocket webSocket : webSocketConnections) {
				sendTo(webSocket, message);
			}
		}
	}

	public void sendTo(WebSocket webSocket, String message) {
		try {
			webSocket.send(message);
		}
		catch (Exception e) {
			logger.warning("Exception " + e);
		}
	}

	private void launchLoggerThread(final BufferedReader stdinReader, final String prefix) {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String line;

					while ((line = stdinReader.readLine()) != null) {
						logger.info(prefix + line);
					}
				}
				catch (Exception e) {
					logger.warning("Exception " + e);
				}
			}
		});
		t.start();
	}

	private void startUnityProcess() throws IOException {
		String baseUrl = "http://127.0.0.1:" + StaticResourcesWebServer.WEBSERVER_POR;
		String scriptUrl = baseUrl + "/scripts/unitytray.py";
		String webSocketUri = "ws://127.0.0.1:" + WEBSOCKET_SERVER_PORT;

		Object[] args = new Object[] {
			baseUrl,
			webSocketUri, 
			messages.toString(),
			scriptUrl
		};
		
		String startScript = String.format(
			"import urllib2 ; " + 
			"baseUrl = '%s' ; " + 
			"wsUrl   = '%s' ; " +
			"i18n    = '%s' ; " +
			"exec urllib2.urlopen('%s').read()", args);

		String[] command = new String[] { "/usr/bin/python", "-c", startScript };
		ProcessBuilder processBuilder = new ProcessBuilder(command);

		unityProcess = processBuilder.start();

		BufferedReader is = new BufferedReader(new InputStreamReader(unityProcess.getInputStream()));
		BufferedReader es = new BufferedReader(new InputStreamReader(unityProcess.getErrorStream()));

		launchLoggerThread(is, "Python Input Stream : ");
		launchLoggerThread(es, "Python Error Stream : ");
	}

	@Override
	public void updateFolders(Map<String, Map<String, String>> folders) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("action", "update_tray_menu");
		parameters.put("folders", folders);
		sendToAll(parameters.toString());
	}

	@Override
	public void updateStatusText(String statusText) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("action", "update_tray_status_text");
		parameters.put("text", statusText);
		sendToAll(parameters.toString());
	}

	@Override
	protected void setTrayImage(TrayIcons image) {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("action", "update_tray_icon");
		parameters.put("imageFileName", image.getFileName());
		sendToAll(parameters.toString());
	}
}
