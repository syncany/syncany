package org.syncany.gui.messaging;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.gui.CommonParameters;
import org.syncany.gui.MainGUI;
import org.syncany.gui.UserInput;
import org.syncany.gui.messaging.websocket.WebsocketClient;

public class ClientCommandFactory {
	private static final Logger logger = Logger.getLogger(ClientCommandFactory.class.getSimpleName());
	private static WebsocketClient client;
	
	public static void startWebSocketClient(){
		try {
			client = new WebsocketClient();
			client.startWebSocketConnection();
		}
		catch (URISyntaxException e) {
			logger.warning("URI Syntax problem : " + e);
		}
	}
	
	public static void stopWebSocketClient(){
		if (client != null) {
			client.stop();
			client = null;
		}
	}
	
	public static void stopDaemon(){
		Map<String, Object> parameters = buildParameters();
		parameters.put("action", "quit");
		
		if (client != null) {
			client.handleCommand(parameters);
		}
		else{
			logger.warning("Stop daemon command failed. Websocket client is null");
		}
	}
	
	//Command Methods
	public static void handleCommand(UserInput userInput){
		Map<String, Object> commonParameters = buildParameters();
		Map<String, String> pluginParameters = new HashMap<>();
		
		for (CommonParameters key : userInput.getCommonParameters().keySet()) {
			commonParameters.put(key.value(), userInput.getCommonParameters().get(key));
		}

		pluginParameters.putAll(userInput.getPluginParameters());

		commonParameters.put("pluginArgs", pluginParameters);
		
		client.handleCommand(commonParameters);
	}
	
	public static void handleWatch(String folder, int interval) {
		Map<String, Object> command = buildParameters();
		command.put("action", "watch");
		command.put("localfolder", folder);
		command.put("interval", ""+interval);
		client.handleCommand(command);
	}
	
	public static void handlePauseWatch(String folder) {
		Map<String, Object> command = buildParameters();
		command.put("action", "pause_watch");
		command.put("localfolder", folder);
		client.handleCommand(command);
	}
	
	public static void handleResumeWatch(String folder) {
		Map<String, Object> command = buildParameters();
		command.put("action", "resume_watch");
		command.put("localfolder", folder);
		client.handleCommand(command);
	}
	
	public static void handleStopWatch(String folder) {
		Map<String, Object> command = buildParameters();
		command.put("action", "stop_watch");
		command.put("localfolder", folder);
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
