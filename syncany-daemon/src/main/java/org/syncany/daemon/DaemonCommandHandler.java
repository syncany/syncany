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
	private static Logger logger = Logger.getLogger(DaemonCommandHandler.class.getSimpleName());
		
//	private static String handleStopWatch(Map<String, Object> parameters) {
//		Map<String, Command> commands = Daemon.getInstance().getCommands();
//		String id = (String)parameters.get("id");
//		logger.log(Level.INFO, "Stop watching folder with id {1}", new Object[]{id});
//		Command cl = commands.get(id);
//		if (cl instanceof WatchCommand){
//			WatchCommand wc = (WatchCommand)cl;
//			wc.pause();
//		}
//		return null;
//	}

	private static String handleWatch(Map<String, Object> parameters) {
		Map<String, Command> commands = Daemon.getInstance().getCommands();
		String localDir = (String)parameters.get("localfolder");
		logger.log(Level.INFO, "Watching folder {0}", localDir);
		
		for (String key : commands.keySet()){
			Command c = commands.get(key);
			if (c instanceof WatchCommand){
				WatchCommand _wc = (WatchCommand)c;
				if (_wc.getLocalFolder().equals(localDir)) return null;
			}
		}

		WatchCommand wc = new WatchCommand(localDir, 3);
		commands.put(wc.getId(), wc);
		wc.execute();
		updateWatchedFolders();
		return null;
	}

	private static String handleQuit(Map<String, Object> parameters) {
		if (Daemon.getInstance() != null)
			Daemon.getInstance().shutdown();
		
		return null;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> handleInit(Map<String, Object> parameters) {
		Map<String, String> ret;
		
		List<String> pluginArgs= new ArrayList<>();
		
		Map<String, String> args = (Map<String, String>)parameters.get("pluginArgs");
		
		for (String key : args.keySet()){
			pluginArgs.add(key + "=" + args.get(key));
		}
		
		String pluginName = (String)parameters.get("pluginId");
		String localDir = (String)parameters.get("localFolder");
		String passsword =(String) parameters.get("password");
		boolean encrypted = "yes".equals((String)parameters.get("encryption"));
		
		InitCommand ic = new InitCommand(pluginName, pluginArgs, localDir, passsword, false, encrypted, false);
		
		try {
			ic.execute();
			ret = buildReturnObject(parameters);
			ret.put("result", "succeed");
		}
		catch (Exception e) {
			logger.warning("Exception " + e);
			ret = buildReturnObject(parameters);
			ret.put("result", "failed");
		}
		
		return ret;
	}
	
	private static Map<String, String> buildReturnObject(Map<String, Object> parameters) {
		Map<String, String> ret = new HashMap<>();
		ret.put("client_id", (String)parameters.get("client_id"));
		ret.put("client_type", (String)parameters.get("client_type"));
		ret.put("timestamp", ""+System.nanoTime());
		ret.put("action", "daemon_update");
		ret.put("client_action", (String)parameters.get("action"));
		return ret;
	}

	private static void updateWatchedFolders() {
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
		String action = ((String) params.get("action")).toLowerCase();
		
		switch (action){
//			case "get_syncing_state":
//				notifySyncingState(params);
//				break;
//			case "get_watched":
//				handleGetWatchedFolders();
//				break;
			case "watch":
				handleWatch(params);
				break;
			case "create":
				Map<String, String> ret = handleInit(params);
				WSServer.sendToAll(JsonHelper.fromMapToString(ret));
				break;
//			case "connect":
//				handleConnect(params);
//				break;
//			case "pause":
//				handleStopWatch(params);
//				break;
			case "quit":
				handleQuit(params);
				break;
			default:
				logger.log(Level.WARNING, "Unknown action received; returning message: "+action);
				WSServer.sendToAll(JsonHelper.fromMapToString(params));
				break;
		}
	}
}
