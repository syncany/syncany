package org.syncany.daemon;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.syncany.config.Logging;
import org.syncany.daemon.command.Command;
import org.syncany.daemon.command.CommandStatus;
import org.syncany.daemon.command.WatchCommand;
import org.syncany.daemon.config.DaemonConfigurationTO;
import org.syncany.daemon.config.DeamonConfiguration;
import org.syncany.daemon.exception.DaemonAlreadyStartedException;
import org.syncany.daemon.util.SocketLock;
import org.syncany.daemon.util.WatchEvent;
import org.syncany.daemon.util.WatchEventAction;
import org.syncany.daemon.websocket.WSServer;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class Daemon {
	private static final Logger log = Logger.getLogger(Daemon.class.getSimpleName());

	private static final SocketLock daemonSocketLock = new SocketLock();
	private static EventBus eventBus = new EventBus("syncany-daemon");

	private static final AtomicBoolean quit = new AtomicBoolean(false);
	private static boolean quittingInProgress = false;
	private static Daemon instance = null;
	private boolean startedWithGui = false;
	private DeamonConfiguration daemonConfiguration;

	private Map<String, Command> commands = new HashMap<>();

	static {
		Logging.init();
	}

	public static void main(String[] args) {
		try {
			Daemon.getInstance().start(false);
		}
		catch (Exception e) {
			throw new RuntimeException("Cannot launch daemon.", e);
		}
	}
	
	public static EventBus getEventBus() {
		return eventBus;
	}

	public Daemon() {
		try {
			DaemonConfigurationTO acto = loadDaemonConfiguration();
			daemonConfiguration = DeamonConfiguration.from(acto);
		}
		catch (Exception e) {
			log.severe("Unable to load application configuration File : " + e);
			return;
		}
	}

	/**
	 * @return the commands
	 */
	public Map<String, Command> getCommands() {
		return commands;
	}

	@Subscribe
	public void update(DaemonEvent event) {
		DaemonCommandHandler.updateWatchedFolders();
	}

	private void killWatchingThreads() {
		log.fine("Killing all watching threads ...");
		for (String key : getCommands().keySet()) {
			Command t = getCommands().get(key);
			t.disposeCommand();
		}
	}

	public void shutdown() {
		if (quittingInProgress || !startedWithGui) {
			return;
		}

		log.fine("Shutdown DaemonServer");

		quittingInProgress = true;
		quit.set(true);

		killWatchingThreads();

		WSServer.stop();

		daemonSocketLock.free();
		instance = null;

		quittingInProgress = false;
	}

	public void start(boolean startedWithGui) throws DaemonAlreadyStartedException {
		this.startedWithGui = startedWithGui;

		// 0- determine if gui is already launched
		try {
			daemonSocketLock.lock();
		}
		catch (Exception e) {
			log.info("Daemon already launched");
			throw new DaemonAlreadyStartedException("Daemon Server socket lock failed");
		}

		// 2- Starting websocket server
		WSServer.start();
	}

	public static Daemon getInstance() {
		if (instance == null) {
			instance = new Daemon();
			getEventBus().register(instance);
		}
		return instance;
	}

	@Subscribe
	public void update(WatchEvent event) {
		Map<String, Command> map = getCommands();

		String id = event.getId();
		Command cl = map.get(id);

		if (cl instanceof WatchCommand) {
			if (event.getAction().equals(WatchEventAction.START_WATCH)) {
				cl.setStatus(CommandStatus.STARTED);
			}

			if (event.getAction().equals(WatchEventAction.STOP_WATCH)) {
				cl.setStatus(CommandStatus.STOPPED);
			}
		}

		log.fine("Pushing update to client");

		WSServer.sendToAll("update");
	}

	private static DaemonConfigurationTO loadDaemonConfiguration() throws Exception {
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
	
	public DeamonConfiguration getDaemonConfiguration() {
		return daemonConfiguration;
	}
}