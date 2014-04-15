package org.syncany.daemon;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.syncany.daemon.command.Command;
import org.syncany.daemon.command.CommandStatus;
import org.syncany.daemon.command.WatchCommand;
import org.syncany.daemon.config.DaemonConfigurationTO;
import org.syncany.daemon.config.DeamonConfiguration;
import org.syncany.daemon.exception.ServiceAlreadyStartedException;
import org.syncany.daemon.util.SocketLock;
import org.syncany.daemon.util.WatchEvent;
import org.syncany.daemon.util.WatchEventAction;
import org.syncany.daemon.websocket.DaemonWebSocketServer;
import org.syncany.daemon.websocket.messages.DaemonMessage;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class Daemon implements Service {
	private static final Logger log = Logger.getLogger(Daemon.class.getSimpleName());

	private final AtomicBoolean quit = new AtomicBoolean(false);
	private final AtomicBoolean quittingInProgress = new AtomicBoolean(false);

	private int lockPort;
	private SocketLock daemonSocketLock;
	private EventBus eventBus;

	private boolean startedWithGui = false;
	private DeamonConfiguration daemonConfiguration;

	private Map<String, Command> commands = new HashMap<>();
	
	/**
	 * @return the commands
	 */
	public Map<String, Command> getCommands() {
		return commands;
	}

	@Subscribe
	public void update(DaemonEvent event) {
		DaemonCommandHandler.updateWatchedFolders(new DaemonMessage(null));
	}

	private void killWatchingThreads() {
		log.fine("Killing all watching threads ...");
		for (String key : getCommands().keySet()) {
			Command t = getCommands().get(key);
			t.disposeCommand();
		}
	}

	@Subscribe
	public void update(WatchEvent event) {
		Map<String, Command> map = getCommands();

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

		DaemonWebSocketServer.sendToAll("update");
	}

	private DaemonConfigurationTO loadDaemonConfiguration() throws Exception {
		File appConfigDir = new File(System.getProperty("user.home") + File.separator + ".syncany");
		File daemonConfigFile = new File(appConfigDir, "syncany-daemon-config.xml");

		if (!daemonConfigFile.exists()) {
			if (!appConfigDir.exists()) {
				appConfigDir.mkdir();
			}
			
			DaemonConfigurationTO.store(DaemonConfigurationTO.getDefault(), daemonConfigFile);
			log.info("Syncany daemon configuration file created");
		}

		return DaemonConfigurationTO.load(daemonConfigFile);
	}
	
	// Service interface
	@Override
	public void start(Map<String, Object> parameters) throws ServiceAlreadyStartedException {
		if (parameters.containsKey("startedWithGui")) {
			startedWithGui = (Boolean)parameters.get("startedWithGui");
		}
		else {
			startedWithGui = false;
		}
		
		if (parameters.containsKey("lockPort")) {
			lockPort = (Integer)parameters.get("lockPort");
		}
		else {
			lockPort = 3338;
		}
		
		this.daemonSocketLock = new SocketLock(lockPort);
		this.eventBus = new EventBus("syncany-daemon-"+lockPort);
		eventBus.register(this);
		
		try {
			DaemonConfigurationTO acto = loadDaemonConfiguration();
			daemonConfiguration = DeamonConfiguration.from(acto);
		}
		catch (Exception e) {
			log.severe("Unable to load application configuration File : " + e);
			return;
		}

		// 0- determine if gui is already launched
		try {
			daemonSocketLock.lock();
		}
		catch (Exception e) {
			log.info("Daemon already launched");
			throw new ServiceAlreadyStartedException("Daemon Server socket lock failed");
		}

		// 2- Starting websocket server
		DaemonWebSocketServer.start();
	}
	
	@Override
	public void stop() {
		if (quittingInProgress.get() || !startedWithGui) {
			return;
		}

		log.fine("Shutdown DaemonServer");

		quittingInProgress.set(true);
		quit.set(true);

		killWatchingThreads();

		DaemonWebSocketServer.stop();
	
		daemonSocketLock.free();

		quittingInProgress.set(false);
	}
}