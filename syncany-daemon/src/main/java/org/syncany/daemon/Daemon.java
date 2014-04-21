package org.syncany.daemon;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.syncany.daemon.command.Command;
import org.syncany.daemon.command.CommandStatus;
import org.syncany.daemon.command.WatchCommand;
import org.syncany.daemon.exception.ServiceAlreadyStartedException;
import org.syncany.daemon.util.SocketLock;
import org.syncany.daemon.util.WatchEvent;
import org.syncany.daemon.util.WatchEventAction;
import org.syncany.operations.daemon.AbstractService;
import org.syncany.operations.daemon.DaemonWebSocketServer;

import com.google.common.eventbus.Subscribe;

public class Daemon extends AbstractService {
	private static final Logger log = Logger.getLogger(Daemon.class.getSimpleName());

	private final AtomicBoolean quit = new AtomicBoolean(false);
	private final AtomicBoolean running = new AtomicBoolean(false);
	private final AtomicBoolean quittingInProgress = new AtomicBoolean(false);

	private int lockPort;
	private SocketLock daemonSocketLock;

	private Map<String, Command> runningCommands = new HashMap<>();
	
	/**
	 * @return the commands
	 */
	public Map<String, Command> getRunningCommands() {
		return runningCommands;
	}

	private void killWatchingThreads() {
		log.fine("Killing all watching threads ...");
		for (String key : getRunningCommands().keySet()) {
			Command t = getRunningCommands().get(key);
			t.disposeCommand();
		}
	}

	@Subscribe
	public void update(WatchEvent event) {
		Map<String, Command> map = getRunningCommands();

		String id = event.getId();
		Command cl = map.get(id);

		if (cl instanceof WatchCommand) {
			if (event.getAction().equals(WatchEventAction.START_WATCH)) {
				cl.setStatus(CommandStatus.SYNCING);
			}

			if (event.getAction().equals(WatchEventAction.STOP_WATCH)) {
				cl.setStatus(CommandStatus.STOPPED);
			}
		}

		log.fine("Pushing update to client");

		getWebsocketServer().sendToAll("update");
	}
	
	public DaemonWebSocketServer getWebsocketServer(){
		DaemonWebSocketServer dws = ServiceManager.getService(getIdentifier() + "_websocket", DaemonWebSocketServer.class);
		return dws;
	}
	
	// Service interface
	@Override
	public void start(Map<String, Object> parameters) throws ServiceAlreadyStartedException {
		initLock(parameters);		

		// 2- Starting websocket server
		try {
			ServiceManager.startService(getIdentifier() + "_websocket", "org.syncany.daemon.websocket.DaemonWebSocketServer", null);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		running.set(true);
	}
	
	private void initLock(Map<String, Object> parameters) throws ServiceAlreadyStartedException {
		if (parameters.containsKey("lockPort")) {
			lockPort = (Integer)parameters.get("lockPort");
		}
		else {
			lockPort = 3338;
		}
		
		this.daemonSocketLock = new SocketLock(lockPort);
			
		// 0- determine if gui is already launched
		try {
			daemonSocketLock.lock();
		}
		catch (Exception e) {
			log.info("Daemon already launched");
			throw new ServiceAlreadyStartedException("Daemon Server socket lock failed");
		}
	}

	@Override
	public void stop() {
		if (quittingInProgress.get()) {
			return;
		}

		log.fine("Shutdown DaemonServer");

		quittingInProgress.set(true);
		quit.set(true);

		killWatchingThreads();

		ServiceManager.stopService(getIdentifier() + "_websocket");
	
		daemonSocketLock.free();

		quittingInProgress.set(false);
		running.set(false);
	}

	@Override
	public boolean isRunning() {
		return running.get();
	}
}