package org.syncany.daemon;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.crypto.CipherSpecs;
import org.syncany.daemon.command.Command;
import org.syncany.daemon.command.ConnectCommand;
import org.syncany.daemon.command.InitCommand;
import org.syncany.daemon.command.WatchCommand;
import org.syncany.daemon.websocket.WSServer;
import org.syncany.daemon.websocket.messages.DaemonMessage;
import org.syncany.daemon.websocket.messages.DaemonResultInitMessage;
import org.syncany.daemon.websocket.messages.DaemonResultMessage;
import org.syncany.daemon.websocket.messages.DeamonConnectMessage;
import org.syncany.daemon.websocket.messages.DeamonInitMessage;
import org.syncany.daemon.websocket.messages.DeamonResultConnectMessage;
import org.syncany.daemon.websocket.messages.DeamonWatchMessage;
import org.syncany.daemon.websocket.messages.DeamonWatchResultMessage;
import org.syncany.operations.GenlinkOperation.GenlinkOperationResult;
import org.syncany.util.JsonHelper;


public class DaemonCommandHandler {
	private static Logger logger = Logger.getLogger(DaemonCommandHandler.class.getSimpleName());
		
	private static String handleWatch(DeamonWatchMessage message) {
		Map<String, Command> commands = Daemon.getInstance().getCommands();
		String localDir = message.getLocalFolder();
		int interval = message.getInterval();
		boolean watcher = message.isAutomaticWatcher();
		
		logger.log(Level.INFO, String.format("Watching folder %s", localDir));
		
		for (String key : commands.keySet()){
			Command c = commands.get(key);
			if (c instanceof WatchCommand){
				WatchCommand _wc = (WatchCommand)c;
				if (_wc.getLocalFolder().equals(localDir)) return null;
			}
		}

		WatchCommand wc = new WatchCommand(localDir, interval, watcher);
		commands.put(wc.getId(), wc);
		wc.execute();
		return null;
	}
	
	private static String handlePauseWatch(DaemonMessage message) {
		Map<String, Command> commands = Daemon.getInstance().getCommands();
		String localDir = message.getLocalFolder();
		
		logger.log(Level.INFO, String.format("Pause watching folder %s", localDir));
		
		for (String key : commands.keySet()){
			Command c = commands.get(key);
			if (c instanceof WatchCommand){
				WatchCommand _wc = (WatchCommand)c;
				if (_wc.getLocalFolder().equals(localDir)){
					_wc.pause();
				}
			}
		}
		updateWatchedFolders(message);
		return null;
	}
	
	private static String handleStopWatch(DaemonMessage message) {
		Map<String, Command> commands = Daemon.getInstance().getCommands();
		String localDir = message.getLocalFolder();
		
		logger.log(Level.INFO, String.format("Stop watching folder %s", localDir));
		
		for (String key : commands.keySet()){
			Command c = commands.get(key);
			if (c instanceof WatchCommand){
				WatchCommand _wc = (WatchCommand)c;
				if (_wc.getLocalFolder().equals(localDir)){
					_wc.stop();
				}
			}
		}
		updateWatchedFolders(message);
		return null;
	}
	
	private static String handleResumeWatch(DaemonMessage message) {
		Map<String, Command> commands = Daemon.getInstance().getCommands();
		String localDir = message.getLocalFolder();
		
		logger.log(Level.INFO, String.format("Resume watching folder %s", localDir));
		
		for (String key : commands.keySet()){
			Command c = commands.get(key);
			if (c instanceof WatchCommand){
				WatchCommand _wc = (WatchCommand)c;
				if (_wc.getLocalFolder().equals(localDir)){
					_wc.resume();
				}
			}
		}
		updateWatchedFolders(message);
		return null;
	}

	private static String handleQuit(DaemonMessage message) {
		if (Daemon.getInstance() != null) {
			Daemon.getInstance().shutdown();
		}
		
		return null;
	}

	private static DaemonResultInitMessage handleInit(DeamonInitMessage message) {
		List<String> pluginArgs= new ArrayList<>();
		
		Map<String, String> args = message.getPluginArgs();
		
		for (String key : args.keySet()){
			pluginArgs.add(key + "=" + args.get(key));
		}
		
		String pluginName = message.getPluginId();
		String localDir = message.getLocalFolder();
		String passsword = message.getPassword();
		int chunkSize = message.getChunkSize();
		
		boolean gzip = message.isGzip();
		boolean encrypted = message.isEncryption();
		
		int[] cipherSpec;
		
		String cipherString = message.getCipherSpec();
		if (cipherString != null && cipherString.length() > 0) {
			String[] cipherSpecString = cipherString.split(",");
			cipherSpec = new int[cipherSpecString.length];
		
			for (int i = 0 ; i < cipherSpecString.length ; i ++){
				cipherSpec[i] = Integer.parseInt(cipherSpecString[i]);
			}
		}
		else {
			cipherSpec = CipherSpecs.DEFAULT_CIPHER_SPECS;
		}
		
		// Creation of local Syncany folder
		File localDirFile = new File(localDir);
		if (!localDirFile.exists()){
			localDirFile.mkdir();
		}

		// Creation of local repo ==> TODO[medium]: should'n be handled by plugin directly ?
		if (pluginName.equals("local")){
			File repoPath = new File(args.get("path"));
			
			if (!repoPath.exists()){
				repoPath.mkdir();
			}
		}
		
		InitCommand ic = new InitCommand(pluginName, pluginArgs, localDir, passsword, encrypted, gzip, chunkSize, cipherSpec);
		
		DaemonResultInitMessage ret = new DaemonResultInitMessage(buildReturnObject(message));
		try {
			GenlinkOperationResult result = ic.execute().getGenLinkResult();
			ret.setSuccess(true);
			ret.setShareLink(result.getShareLink());
			ret.setShareLinkEncrypted(result.isShareLinkEncrypted());
		}
		catch (Exception e) {
			logger.warning("Exception " + e);
			ret.setSuccess(false);
		}
		
		return ret;
	}
	
	private static DeamonResultConnectMessage handleConnect(DeamonConnectMessage message) {
		List<String> pluginArgs= new ArrayList<>();
		
		Map<String, String> args = message.getPluginArgs();
		
		for (String key : args.keySet()){
			pluginArgs.add(key + "=" + args.get(key));
		}
		
		String pluginName = message.getPluginId();
		String localDir = message.getLocalFolder();
		String passsword = message.getPassword();
		String url = message.getUrl();
		
		File localDirFile = new File(localDir);
		if (!localDirFile.exists()){
			localDirFile.mkdir();
		}
		
		ConnectCommand ic = new ConnectCommand(url, pluginName, pluginArgs, localDir, passsword);
	
		DeamonResultConnectMessage ret = new DeamonResultConnectMessage(message);
		
		try {
			ic.execute();
			ret.setSuccess(true);
		}
		catch (Exception e) {
			logger.warning("Exception " + e);
			ret.setSuccess(false);
		}
		
		return ret;
	}
	
	private static DaemonResultMessage buildReturnObject(DaemonMessage message) {
		DaemonResultMessage ret = new DaemonResultMessage(message);
		ret.setTimeStamp(System.nanoTime());
		ret.setAction("daemon_command_result");
		ret.setOriginalAction(message.getAction());
		ret.setCommandId(message.getCommandId());
		return ret;
	}

	public static void updateWatchedFolders(DaemonMessage parent) {
		Map<String, Command> commands = Daemon.getInstance().getCommands();
		
		Map<String, Map<String, String>> folders = new HashMap<>();
		DeamonWatchResultMessage ret = new DeamonWatchResultMessage(parent);
		
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
		
		ret.setAction("update_watched_folders");
		ret.setOriginalAction(parent.getAction());
		ret.setFoldersUpdate(folders);
		WSServer.sendToAll(ret);
	}

	public static void handle(String s) {
		DaemonMessage message = JsonHelper.fromStringToObject(s, DaemonMessage.class);
				
		switch (message.getAction()){
			case "watch":
				handleWatch(JsonHelper.fromStringToObject(s, DeamonWatchMessage.class));
				break;
				
			case "pause_watch":
				handlePauseWatch(message);
				break;
				
			case "stop_watch":
				handleStopWatch(message);
				break;	
				
			case "resume_watch":
				handleResumeWatch(message);
				break;
				
			case "create":
				DeamonInitMessage dim = JsonHelper.fromStringToObject(s, DeamonInitMessage.class);
				DaemonMessage retInit = handleInit(dim);
				WSServer.sendToAll(JsonHelper.fromObjectToString(retInit));
				break;
				
			case "connect":
				DaemonMessage retConn = handleConnect(JsonHelper.fromStringToObject(s, DeamonConnectMessage.class));
				WSServer.sendToAll(JsonHelper.fromObjectToString(retConn));
				break;
				
			case "quit":
				handleQuit(message);
				break;
				
			default:
				logger.log(Level.WARNING, "Unknown action received; returning message: "+message.toString());
				break;
		}
	}
}
