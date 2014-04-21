package org.syncany.daemon;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.daemon.command.Command;
import org.syncany.daemon.command.WatchCommand;
import org.syncany.operations.daemon.websocket.DaemonRequest;
import org.syncany.operations.daemon.websocket.DaemonResponse;
import org.syncany.operations.daemon.websocket.DaemonWatchEvent;
import org.syncany.operations.daemon.websocket.DaemonWebSocketServer;
import org.syncany.operations.daemon.websocket.WatchDaemonRequest;
import org.syncany.operations.watch.WatchOperation.WatchOperationListener;
import org.syncany.util.JsonHelper;

public class DaemonRequestHandler {
	private static Logger logger = Logger.getLogger(DaemonRequestHandler.class.getSimpleName());
	
	private DaemonWebSocketServer server;
	
	public DaemonRequestHandler(DaemonWebSocketServer server){
		this.server = server;
	}
	
	private String handleWatch(final WatchDaemonRequest message) {
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
	
	
	private static DaemonResponse buildReturnObject(DaemonRequest message) {
		DaemonResponse ret = new DaemonResponse(message);
		ret.setTimeStamp(System.nanoTime());
		ret.setAction("daemon_command_result");
		ret.setOriginalAction(message.getAction());
		ret.setCommandId(message.getCommandId());
		return ret;
	}

	public void handle(String s) {
		DaemonRequest message = JsonHelper.fromStringToObject(s, DaemonRequest.class);
				
		switch (message.getAction()){
			case "watch":
				handleWatch(JsonHelper.fromStringToObject(s, WatchDaemonRequest.class));
				break;				
				
			case "pause_watch":				
			case "stop_watch":
			case "resume_watch":
			default:
				logger.log(Level.WARNING, "Unknown action received; returning message: "+message.toString());
				break;
		}
	}
}
