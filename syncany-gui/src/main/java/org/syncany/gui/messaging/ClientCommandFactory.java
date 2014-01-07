package org.syncany.gui.messaging;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.gui.MainGUI;
import org.syncany.gui.UserInput;

public class ClientCommandFactory {
	private static final Logger log = Logger.getLogger(ClientCommandFactory.class.getSimpleName());
	
	private static WSClient client;
	
	public static void startWebSocketClient(){
		try {
			log.info("Starting websocket server");
			client = new WSClient();
			client.startWebSocketConnection();
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	public static void stopWebSocketClient(){
		log.info("Stopping websocket server");
		client.stop();
		client = null;
	}
	
	public static void stopDaemon(){
		Map<String, Object> parameters = buildParameters();
		parameters.put("action", "quit");
		client.handleCommand(parameters);
	}
	
	private static void list() {
		Map<String, Object> parameters = buildParameters();
		parameters.put("action", "get_watched");
		client.handleCommand(parameters);
	}

	private static void connect(String url, String folder) {
		Map<String, Object> parameters = buildParameters();
		parameters.put("action", "connect");
		parameters.put("folder", folder);
		parameters.put("url", url);
		client.handleCommand(parameters);
	}

	private static void create(Map<String, Object> wizardParameters) {
		Map<String, Object> parameters = buildParameters();
		parameters.putAll(wizardParameters);
		client.handleCommand(parameters);
	}
	
	public static void handleCommand(UserInput userInput){
	}
	
	/**
	 * Created default Map<String, String> parameters
	 * with client_id, client_type and timestamp
	 */
	private static Map<String, Object> buildParameters(){
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("client_id", MainGUI.getClientIdentification());
		parameters.put("client_type", "syncany-gui");
		parameters.put("timestamp", ""+System.nanoTime());
		return parameters;
	}
}
