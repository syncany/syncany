package org.syncany.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.gui.websocket.WSClient;

public class ClientCommandFactory {
	private static final Logger log = Logger.getLogger(ClientCommandFactory.class.getSimpleName());
	
	public static void watch(String folder) {
		Map<String, String> parameters = buildParameters();
		parameters.put("action", "watch");
		parameters.put("folder", folder);
		WSClient.instance().handleCommand(parameters);
	}
	
	public static void stopWatch(String clientId){
		Map<String, String> parameters = buildParameters();
		parameters.put("action", "stop");
		parameters.put("id", clientId);
		WSClient.instance().handleCommand(parameters);
	}
	
	public static void stopDaemon(){
		Map<String, String> parameters = buildParameters();
		parameters.put("action", "quit");
		WSClient.instance().handleCommand(parameters);
	}
	
	public static void list() {
		Map<String, String> parameters = buildParameters();
		parameters.put("action", "get_watched");
		WSClient.instance().handleCommand(parameters);
	}

	public static void connect(String url, String folder) {
		Map<String, String> parameters = buildParameters();
		parameters.put("action", "connect");
		parameters.put("folder", folder);
		parameters.put("url", url);
		WSClient.instance().handleCommand(parameters);
	}
	
	private static Map<String, String> buildParameters(){
		Map<String, String> parameters = new HashMap<>();
		parameters.put("sourceId", "syncany_client");
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
		
		WSClient.instance().handleCommand(parameters);
	}
}
