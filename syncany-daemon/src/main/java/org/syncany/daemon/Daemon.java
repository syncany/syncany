package org.syncany.daemon;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Logging;
import org.syncany.daemon.command.Command;
import org.syncany.daemon.command.CommandStatus;
import org.syncany.daemon.command.WatchCommand;
import org.syncany.daemon.util.PropertiesUtil;
import org.syncany.daemon.util.SocketLock;
import org.syncany.daemon.util.WatchEvent;
import org.syncany.daemon.util.WatchEventAction;
import org.syncany.daemon.websocket.WSServer;

import com.google.common.eventbus.Subscribe;

public class Daemon {
	private static final Logger log = Logger.getLogger(Daemon.class.getSimpleName());

	private static final SocketLock daemonSocketLock = new SocketLock();
	private static final AtomicBoolean quit = new AtomicBoolean(false);
	private static boolean quitting = false;
	private static Daemon instance = null;
	
	private Map<String, Command> commands = new HashMap<>(); 
	
	static{
		Logging.init();
	}

	/**
	 * @return the commands
	 */
	public Map<String, Command> getCommands() {
		return commands;
	}
	
	private void killWatchingThreads(){
		log.fine("Killing all watching threads ...");
		for (String key : getCommands().keySet()){
			Command t = getCommands().get(key);
			t.disposeCommand();
		}
	}
	
	public void shutdown(){
		if (quitting) return;
		
		log.fine("Shutdown DaemonServer");
		
		quitting = true;
		quit.set(true);
		
		killWatchingThreads();

		WSServer.stop();
		
		daemonSocketLock.free();
		quitting = false;
		instance = null;
		
		//[TODO high: daemon should exit gracefully by stoping watching threads
		System.exit(1);
	}
	
	public void start() throws Exception{
		//0- determine if gui is already launched
		try{
			daemonSocketLock.lock();
		}
		catch (Exception e){
			log.info("Daemon already launched");
			return;
		}

		//1- Restore last watched directories
		restoreLastState();

		//2- Starting websocket server
		WSServer.start();
	}
	
	private void restoreLastState() {
		log.fine("Restoring last state of DaemonServer");
		try {
			String syncAnyFolder = System.getProperty("user.home") + File.separator + ".syncany";
			File saf = new File(syncAnyFolder);
			
			if (!saf.exists()){
				saf.mkdir();
			}
			File f = new File(syncAnyFolder + File.separator + "properties.txt");
			
			if (!f.exists()){
				f.createNewFile();
			}
			
			Properties p = PropertiesUtil.load(syncAnyFolder + File.separator + "properties.txt");
			String[] folders = ((String)p.get("watched_folders")).split(";");
			
			for (final String folder : folders){
				Map<String, String> parameters = new HashMap<>();
				parameters.put("localfolder", folder);
				parameters.put("action", "watch");
				DaemonCommandHandler.handle(parameters);
			}
			
			Map<String, String> parameters = new HashMap<>();
			parameters.put("action", "get_watched");
			
			DaemonCommandHandler.handle(parameters);
		} 
		catch (Exception e) {
			log.log(Level.WARNING, "errors in loading properties file");
		}
	}

	public static Daemon getInstance() {
		if (instance == null) instance = new Daemon();
		return instance;
	}
	
	@Subscribe public void update(WatchEvent event){
		Map<String, Command> map = getCommands();
		
		String id = event.getId();
		Command cl = map.get(id);
		
		if (cl instanceof WatchCommand){
			if (event.getAction().equals(WatchEventAction.START_WATCH)){
				cl.setStatus(CommandStatus.STARTED);
			}
			
			if (event.getAction().equals(WatchEventAction.STOP_WATCH)){
				cl.setStatus(CommandStatus.STOPPED);
			}
		}
		
		log.fine("Pushing update to client");
		
		WSServer.sendToAll("update");
	}
	
	public static void main(String[] args) {
		try {
			Daemon.getInstance().start();
		}
		catch (Exception e) {
			log.warning("Cannot launch daemon " + e);
		}
	}
}