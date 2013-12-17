package org.syncany.gui.command;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.gui.main.MainGUI;
import org.syncany.gui.websocket.WSClient;

public class ClientCommandFactory {
	private static final Logger log = Logger.getLogger(ClientCommandFactory.class.getSimpleName());
	private static WSClient client;
	
	public static void setClient(WSClient client) {
		ClientCommandFactory.client = client;
	}
	
	public static void watch(String folder) {
		Map<String, String> parameters = buildParameters();
		parameters.put("action", "watch");
		parameters.put("folder", folder);
		client.handleCommand(parameters);
	}
	
	public static void stopWatch(String clientId){
		Map<String, String> parameters = buildParameters();
		parameters.put("action", "stop");
		parameters.put("id", clientId);
		client.handleCommand(parameters);
	}
	
	public static void stopDaemon(){
		Map<String, String> parameters = buildParameters();
		parameters.put("action", "quit");
		client.handleCommand(parameters);
	}
	
	public static void list() {
		Map<String, String> parameters = buildParameters();
		parameters.put("action", "get_watched");
		client.handleCommand(parameters);
	}

	public static void connect(String url, String folder) {
		Map<String, String> parameters = buildParameters();
		parameters.put("action", "connect");
		parameters.put("folder", folder);
		parameters.put("url", url);
		client.handleCommand(parameters);
	}
	
	private static Map<String, String> buildParameters(){
		Map<String, String> parameters = new HashMap<>();
		parameters.put("client_id", MainGUI.getClientIdentification());
		parameters.put("client_type", "syncany-gui");
		parameters.put("timestamp", ""+System.nanoTime());
		return parameters;
	}

	public static void init(String folder, String plugin, String password, Map<String, String> params) {
		Map<String, String> parameters = buildParameters();
		parameters.put("action", "init");
		parameters.put("folder", folder);
		parameters.put("plugin", plugin);
		
		if (password != null){
			parameters.put("password", password);
		}
		
		if (params != null){
			String s = params.toString();
			parameters.put("parameters", s.substring(1, s.length()-1));
		}
		
		client.handleCommand(parameters);
	}
}
