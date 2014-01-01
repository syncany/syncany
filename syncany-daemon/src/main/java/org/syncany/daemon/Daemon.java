package org.syncany.daemon;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Logging;
import org.syncany.daemon.command.Command;
import org.syncany.daemon.command.CommandStatus;
import org.syncany.daemon.command.WatchCommand;
import org.syncany.daemon.config.DaemonConfigurationTO;
import org.syncany.daemon.config.DeamonConfiguration;
import org.syncany.daemon.util.SocketLock;
import org.syncany.daemon.util.WatchEvent;
import org.syncany.daemon.util.WatchEventAction;
import org.syncany.daemon.websocket.WSServer;

import com.google.common.eventbus.Subscribe;

public class Daemon {
	private static final Logger log = Logger.getLogger(Daemon.class.getSimpleName());

	private static final SocketLock daemonSocketLock = new SocketLock();
	private static final AtomicBoolean quit = new AtomicBoolean(false);
	private static boolean quittingInProgress = false;
	private static Daemon instance = null;
	private boolean startedWithGui = false;
	private DeamonConfiguration daemonConfiguration;
	
	private Map<String, Command> commands = new HashMap<>(); 
	
	static{
		Logging.init();
	}

	public Daemon(){
		try {
			DaemonConfigurationTO acto = loadApplicationConfiguration();
			daemonConfiguration = DeamonConfiguration.from(acto);
		}
		catch (Exception e) {
			log.severe("Unable to load application configuration File");
			return;
		}
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
		if (quittingInProgress || !startedWithGui) 
			return;
		
		log.fine("Shutdown DaemonServer");
		
		quittingInProgress = true;
		quit.set(true);
		
		killWatchingThreads();

		WSServer.stop();
		
		daemonSocketLock.free();
		instance = null;
		
		//[TODO high: daemon should exit gracefully by stoping watching threads

		quittingInProgress = false;
		System.exit(1);
	}
	
	public void start(boolean startedWithGui) {
		this.startedWithGui = startedWithGui;
		
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
		
	}

	public static Daemon getInstance() {
		if (instance == null) instance = new Daemon();
		return instance;
	}
	
	@Subscribe 
	public void update(WatchEvent event){
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
			Daemon.getInstance().start(false);
		}
		catch (Exception e) {
			log.warning("Cannot launch daemon " + e);
		}
	}
	
	private static DaemonConfigurationTO loadApplicationConfiguration() throws Exception {
		String userHome = System.getProperty("user.home");
		File f = new File(userHome + File.separator + ".syncany" + File.separator + "syncany-daemon-config.xml");
		
		if (!f.exists()){ /** creates an empty ApplicationConfigurationTO file **/
			Serializer serializer = new Persister();
			DaemonConfigurationTO acto = new DaemonConfigurationTO();
			serializer.write(acto, f);
		}
		
		DaemonConfigurationTO acto = DaemonConfigurationTO.load(f);
		return acto;
	}
}