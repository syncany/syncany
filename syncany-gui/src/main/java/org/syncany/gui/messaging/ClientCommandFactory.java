package org.syncany.gui.messaging;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.syncany.daemon.websocket.messages.DaemonMessage;
import org.syncany.daemon.websocket.messages.DeamonConnectMessage;
import org.syncany.daemon.websocket.messages.DeamonInitMessage;
import org.syncany.daemon.websocket.messages.DeamonWatchMessage;
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
		DaemonMessage message  = buildParameters();
		message.setAction("quit");
		
		if (client != null) {
			client.handleCommand(message);
		}
		else{
			logger.warning("Stop daemon command failed. Websocket client is null");
		}
	}
	
	//Command Methods
	public static void handleCommand(UserInput userInput){
		String action = userInput.getCommonParameter(CommonParameters.COMMAND_ACTION);
		Map<String, String> pluginParameters = new HashMap<>();
		pluginParameters.putAll(userInput.getPluginParameters());
		DaemonMessage command = buildParameters();

		switch (action){
			case "create":
				DeamonInitMessage dim = new DeamonInitMessage(command);
				dim.setPluginArgs(pluginParameters);
				dim.setLocalFolder(userInput.getCommonParameter(CommonParameters.LOCAL_FOLDER));
				dim.setPassword(userInput.getCommonParameter(CommonParameters.ENCRYPTION_PASSWORD));
				dim.setCommandId(userInput.getCommonParameter(CommonParameters.COMMAND_ID));
				dim.setChunkSize(userInput.getCommonParameterAsInt(CommonParameters.CHUNK_SIZE, 512));
				dim.setPluginId(userInput.getCommonParameter(CommonParameters.PLUGIN_ID));
				dim.setEncryption(userInput.getCommonParameterAsBoolean(CommonParameters.ENCRYPTION_ENABLED));
				dim.setCipherSpec(userInput.getCommonParameter(CommonParameters.ENCRYPTION_ALGORITHM));
				client.handleCommand(dim);
				break;
				
			case "connect":
				DeamonConnectMessage dcm = new DeamonConnectMessage(command);
				dcm.setPluginArgs(pluginParameters);
				dcm.setLocalFolder(userInput.getCommonParameter(CommonParameters.LOCAL_FOLDER));
				dcm.setCommandId(userInput.getCommonParameter(CommonParameters.COMMAND_ID));
				dcm.setPluginId(userInput.getCommonParameter(CommonParameters.PLUGIN_ID));
				dcm.setPassword(userInput.getCommonParameter(CommonParameters.ENCRYPTION_PASSWORD));
				dcm.setUrl(userInput.getCommonParameter(CommonParameters.URL));
				client.handleCommand(dcm);
				break;
				
			case "watch":
				DeamonWatchMessage dwm = new DeamonWatchMessage(command);
				dwm.setLocalFolder(userInput.getCommonParameter(CommonParameters.LOCAL_FOLDER));
				dwm.setCommandId(UUID.randomUUID().toString());
				dwm.setCommandId(userInput.getCommonParameter(CommonParameters.COMMAND_ID));
				client.handleCommand(dwm);
				break;
		}
		
		
	}
	
	public static void handleWatch(String folder, int interval, boolean automatic) {
		DeamonWatchMessage command = new DeamonWatchMessage(buildParameters());
		command.setAction("watch");
		command.setLocalFolder(folder);
		command.setInterval(interval);
		command.setAutomaticWatcher(automatic);
		client.handleCommand(command);
	}
	
	public static void handlePauseWatch(String folder) {
		DaemonMessage command = buildParameters();
		command.setAction("pause_watch");
		command.setLocalFolder(folder);
		client.handleCommand(command);
	}
	
	public static void handleResumeWatch(String folder) {
		DaemonMessage command = buildParameters();
		command.setAction("resumt_watch");
		command.setLocalFolder(folder);
		client.handleCommand(command);
	}
	
	public static void handleStopWatch(String folder) {
		DaemonMessage command = buildParameters();
		command.setAction("stop_watch");
		command.setLocalFolder(folder);
		client.handleCommand(command);
	}
	
	/**
	 * Created default Map<String, String> parameters
	 * with client_id, client_type and timestamp
	 */
	private static DaemonMessage buildParameters(){
		DaemonMessage command = new DaemonMessage();
		command.setClientId(MainGUI.getClientIdentification());
		command.setClientType("syncany-gui");
		command.setTimeStamp(System.nanoTime());
		return command;
	}
}
