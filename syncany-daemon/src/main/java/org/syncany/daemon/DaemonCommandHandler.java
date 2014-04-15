package org.syncany.daemon;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.crypto.CipherSpecs;
import org.syncany.daemon.command.Command;
import org.syncany.daemon.command.ConnectCommand;
import org.syncany.daemon.command.InitCommand;
import org.syncany.daemon.command.WatchCommand;
import org.syncany.daemon.websocket.DaemonWebSocketServer;
import org.syncany.daemon.websocket.messages.DaemonConnectMessage;
import org.syncany.daemon.websocket.messages.DaemonInitMessage;
import org.syncany.daemon.websocket.messages.DaemonMessage;
import org.syncany.daemon.websocket.messages.DaemonResultConnectMessage;
import org.syncany.daemon.websocket.messages.DaemonResultInitMessage;
import org.syncany.daemon.websocket.messages.DaemonResultMessage;
import org.syncany.daemon.websocket.messages.DaemonWatchEvent;
import org.syncany.daemon.websocket.messages.DaemonWatchMessage;
import org.syncany.operations.init.GenlinkOperationResult;
import org.syncany.operations.watch.WatchOperation.WatchOperationListener;
import org.syncany.util.JsonHelper;


public class DaemonCommandHandler {
	private static Logger logger = Logger.getLogger(DaemonCommandHandler.class.getSimpleName());
	
	private DaemonWebSocketServer server;
	
	public DaemonCommandHandler(DaemonWebSocketServer server){
		this.server = server;
	}
	
	private String handleWatch(final DaemonWatchMessage message) {
		final WatchOperationListener wl = new WatchOperationListener() {

			@Override
			public void onUploadStart(int fileCount) {
				
			}

			@Override
			public void onUploadFile(String fileName, int fileNumber) {
				
			}

			@Override
			public void onIndexStart(int fileCount) {
				DaemonWatchEvent dwe = new DaemonWatchEvent();
				dwe.setAction("daemon_watch_event");
				//dwe.setEvent(event);
				DaemonWebSocketServer dws = ServiceManager.getService(message.getDaemonIdentifier(), Daemon.class).getWebsocketServer();
				dws.sendToAll(dwe);
			}

			@Override
			public void onIndexFile(String fileName, int fileNumber) {
				
			}

			@Override
			public void onDownloadStart(int fileCount) {
				
			}

			@Override
			public void onDownloadFile(String fileName, int fileNumber) {
				
			}
		};
		
		Map<String, Command> runningCommande = ServiceManager.getService(message.getDaemonIdentifier(), Daemon.class).getRunningCommands();
		
		String localDir = message.getLocalFolder();
		int interval = message.getInterval();
		boolean watcher = message.isAutomaticWatcher();
		
		logger.log(Level.INFO, String.format("Watching folder %s", localDir));
		
		for (String key : runningCommande.keySet()){
			Command c = runningCommande.get(key);
			if (c instanceof WatchCommand){
				WatchCommand _wc = (WatchCommand)c;
				if (_wc.getLocalFolder().equals(localDir)) return null;
			}
		}

		WatchCommand wc = new WatchCommand(localDir, interval, watcher, wl);
		runningCommande.put(wc.getId(), wc);
		wc.execute();
		return null;
	}
	
	private String handlePauseWatch(DaemonMessage message) {
//		Map<String, Command> commands = Daemon.getInstance().getCommands();
//		String localDir = message.getLocalFolder();
//		
//		logger.log(Level.INFO, String.format("Pause watching folder %s", localDir));
//		
//		for (String key : commands.keySet()){
//			Command c = commands.get(key);
//			if (c instanceof WatchCommand){
//				WatchCommand _wc = (WatchCommand)c;
//				if (_wc.getLocalFolder().equals(localDir)){
//					_wc.pause();
//				}
//			}
//		}
//		updateWatchedFolders(message);
		return null;
	}
	
	private String handleStopWatch(DaemonMessage message) {
//		Map<String, Command> commands = Daemon.getInstance().getCommands();
//		String localDir = message.getLocalFolder();
//		
//		logger.log(Level.INFO, String.format("Stop watching folder %s", localDir));
//		
//		for (String key : commands.keySet()){
//			Command c = commands.get(key);
//			if (c instanceof WatchCommand){
//				WatchCommand _wc = (WatchCommand)c;
//				if (_wc.getLocalFolder().equals(localDir)){
//					_wc.stop();
//				}
//			}
//		}
//		updateWatchedFolders(message);
		return null;
	}
	
	private static String handleResumeWatch(DaemonMessage message) {
//		Map<String, Command> commands = Daemon.getInstance().getCommands();
//		String localDir = message.getLocalFolder();
//		
//		logger.log(Level.INFO, String.format("Resume watching folder %s", localDir));
//		
//		for (String key : commands.keySet()){
//			Command c = commands.get(key);
//			if (c instanceof WatchCommand){
//				WatchCommand _wc = (WatchCommand)c;
//				if (_wc.getLocalFolder().equals(localDir)){
//					_wc.resume();
//				}
//			}
//		}
//		updateWatchedFolders(message);
		return null;
	}

	private String handleQuit(DaemonMessage message) {
//		if (Daemon.getInstance() != null) {
//			Daemon.getInstance().stop();
//		}
//		
		return null;
	}

	private DaemonResultInitMessage handleInit(DaemonInitMessage message) {
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
	
	private DaemonResultConnectMessage handleConnect(DaemonConnectMessage message) {
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
	
		DaemonResultConnectMessage ret = new DaemonResultConnectMessage(message);
		
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

	public void updateWatchedFolders(DaemonMessage parent) {
//		Map<String, Command> commands = Daemon.getInstance().getCommands();
//		
//		Map<String, Map<String, String>> folders = new HashMap<>();
//		DaemonWatchResultMessage ret = new DaemonWatchResultMessage(parent);
//		
//		if (commands.size() > 0){
//			for (String key : commands.keySet()){
//				Command com = commands.get(key);
//				if (com instanceof WatchCommand){
//					Map<String, String> element = new HashMap<>();
//					WatchCommand wc = (WatchCommand)com;
//					element.put("key", key);
//					element.put("folder", wc.getLocalFolder());
//					element.put("status", wc.getStatus().toString());
//					folders.put(key, element);
//				}
//			}
//		}
//		
//		ret.setAction("update_watched_folders");
//		ret.setOriginalAction(parent.getAction());
//		ret.setFoldersUpdate(folders);
//		DaemonWebSocketServer.sendToAll(ret);
	}

	public void handle(String s) {
		DaemonMessage message = JsonHelper.fromStringToObject(s, DaemonMessage.class);
				
		switch (message.getAction()){
			case "watch":
				handleWatch(JsonHelper.fromStringToObject(s, DaemonWatchMessage.class));
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
				DaemonInitMessage dim = JsonHelper.fromStringToObject(s, DaemonInitMessage.class);
				DaemonMessage retInit = handleInit(dim);
				server.sendToAll(JsonHelper.fromObjectToString(retInit));
				break;
				
			case "connect":
				DaemonMessage retConn = handleConnect(JsonHelper.fromStringToObject(s, DaemonConnectMessage.class));
				server.sendToAll(JsonHelper.fromObjectToString(retConn));
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
