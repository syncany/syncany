package org.syncany.daemon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.daemon.command.Command;
import org.syncany.daemon.command.InitCommand;
import org.syncany.daemon.command.WatchCommand;
import org.syncany.daemon.websocket.WSServer;
import org.syncany.util.JsonHelper;


public class DaemonCommandHandler {
	private static Logger log = Logger.getLogger(DaemonCommandHandler.class.getSimpleName());
		
	private static String handleStopWatch(Map<String, Object> parameters) {
		Map<String, Command> commands = Daemon.getInstance().getCommands();
		String id = (String)parameters.get("id");
		log.log(Level.INFO, "Stop watching folder with id {1}", new Object[]{id});
		Command cl = commands.get(id);
		if (cl instanceof WatchCommand){
			WatchCommand wc = (WatchCommand)cl;
			wc.pause();
		}
		return null;
	}

	private static String handleWatch(Map<String, Object> parameters) {
		Map<String, Command> commands = Daemon.getInstance().getCommands();
		String localDir = (String)parameters.get("localfolder");
		log.log(Level.INFO, "Watching folder {0}", localDir);
		
		WatchCommand wc = new WatchCommand(localDir, 3);
		commands.put(wc.getId(), wc);
		wc.execute();
		return null;
	}

	private static String handleQuit(Map<String, Object> parameters) {
		if (Daemon.getInstance() != null)
			Daemon.getInstance().shutdown();
		
		return null;
	}

	private static String handleConnect(Map<String, Object> parameters) {
		return null;
	}
	
	private static String handleInit(Map<String, Object> parameters) {
		List<String> pluginArgs= new ArrayList<>();
		String pluginName = (String)parameters.get("pluginName");
		String localDir = (String)parameters.get("localdir");
		String passsword =(String) parameters.get("passsword");
		boolean advanced = Boolean.parseBoolean((String)parameters.get("localdir"));
		boolean encrypted = Boolean.parseBoolean((String)parameters.get("localdir"));
		boolean gzip = Boolean.parseBoolean((String)parameters.get("localdir"));
		
		InitCommand ic = new InitCommand(
				pluginName, pluginArgs, localDir, passsword, 
				advanced, encrypted, gzip);
		try {
			ic.execute();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private static void handleGetWatchedFolders() {
		Map<String, Command> commands = Daemon.getInstance().getCommands();
		
		Map<String, Map<String, String>> folders = new HashMap<>();
		Map<String, Object> res = new HashMap<>();
		
		if (commands.size() > 0){
			for (String key : commands.keySet()){
				Command com = commands.get(key);
				if (com instanceof WatchCommand){
					Map<String, String> element = new HashMap<>();
					WatchCommand wc = (WatchCommand)com;
					element.put("key", key);
					element.put("folder", wc.getLocalFolder());
					element.put("status", wc.getStatus().toString());
					folders.put(key, element);
				}
			}
		}
		res.put("folders", folders);
		res.put("action", "update_watched_folders");
		String a = JsonHelper.fromMapToString(res);
		WSServer.sendToAll(a);
	}

	public static void handle(String message) {
		Map<String, Object> params = JsonHelper.fromStringToMap(message);
		handle(params);
	}
	
	public static void handle(Map<String, Object> params) {
		String action = ((String)params.get("action")).toLowerCase();
		
		switch (action){
			case "get_watched":
				handleGetWatchedFolders();
				break;
			case "watch":
				handleWatch(params);
				break;
			case "init":
				handleInit(params);
				break;
			case "connect":
				handleConnect(params);
				break;
			case "pause":
				handleStopWatch(params);
				break;
			case "quit":
				handleQuit(params);
				break;
		}
	}
}
