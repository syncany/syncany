package org.syncany.gui.messaging;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.gui.MainGUI;

public class ClientCommandFactory {
	private static final Logger log = Logger.getLogger(ClientCommandFactory.class.getSimpleName());
	private static WSClient client;
	
	static {
		try {
			log.info("Starting websocket server");
			client = new WSClient();
			client.startWebSocketConnection();
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	public static void closeWebSocketClient(){
		client.stop();
	}
	
	public static void watch(String folder) {
		Map<String, Object> parameters = buildParameters();
		parameters.put("action", "watch");
		parameters.put("folder", folder);
		client.handleCommand(parameters);
	}
	
	public static void stopWatch(String clientId){
		Map<String, Object> parameters = buildParameters();
		parameters.put("action", "stop");
		parameters.put("id", clientId);
		client.handleCommand(parameters);
	}
	
	public static void stopDaemon(){
		Map<String, Object> parameters = buildParameters();
		parameters.put("action", "quit");
		client.handleCommand(parameters);
	}
	
	public static void list() {
		Map<String, Object> parameters = buildParameters();
		parameters.put("action", "get_watched");
		client.handleCommand(parameters);
	}

	public static void connect(String url, String folder) {
		Map<String, Object> parameters = buildParameters();
		parameters.put("action", "connect");
		parameters.put("folder", folder);
		parameters.put("url", url);
		client.handleCommand(parameters);
	}

	public static void create(Map<String, Object> wizardParameters) {
		Map<String, Object> parameters = buildParameters();
		parameters.putAll(wizardParameters);
		client.handleCommand(parameters);
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
