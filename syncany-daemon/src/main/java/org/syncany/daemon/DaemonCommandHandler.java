package org.syncany.daemon;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.daemon.command.Command;
import org.syncany.daemon.command.WatchCommand;
import org.syncany.daemon.websocket.DaemonWebSocketServer;
import org.syncany.daemon.websocket.messages.DaemonMessage;
import org.syncany.daemon.websocket.messages.DaemonResultMessage;
import org.syncany.daemon.websocket.messages.DaemonWatchEvent;
import org.syncany.daemon.websocket.messages.DaemonWatchMessage;
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
				/*DaemonWebSocketServer dws = ServiceManager.getService(message.getDaemonIdentifier(), Daemon.class).getWebsocketServer();
				dws.sendToAll(dwe);*/
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
				
			case "quit":
				handleQuit(message);
				break;
				
			default:
				logger.log(Level.WARNING, "Unknown action received; returning message: "+message.toString());
				break;
		}
	}
}
