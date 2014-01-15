package org.syncany.gui.messaging;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.gui.MainGUI;
import org.syncany.gui.UserInput;
import org.syncany.util.SyncanyParameters;

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
	
	//Command Methods
	public static void handleCommand(UserInput userInput){
		Map<String, Object> command = buildParameters();
		Map<String, String> pluginArgs = new HashMap<>();
		
		for (SyncanyParameters key : userInput.keySet()){
			if (key.isPluginParameter()){
				pluginArgs.put(key.value(), userInput.get(key));
			}
			else {
				command.put(key.value(), userInput.get(key));
			}
		}
		
		command.put("pluginArgs", pluginArgs);
		
		client.handleCommand(command);
	}
	
	public static void handleWatch(String folder, int interval) {
		Map<String, Object> command = buildParameters();
		command.put("action", "watch");
		command.put("localfolder", folder);
		command.put("interval", ""+interval);
		client.handleCommand(command);
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
