/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.operations.daemon;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.config.ConfigException;
import org.syncany.config.UserConfig;
import org.syncany.config.to.DaemonConfigTO;
import org.syncany.config.to.FolderTO;
import org.syncany.operations.Operation;
import org.syncany.operations.OperationResult;
import org.syncany.operations.daemon.DaemonControlServer.ControlCommand;
import org.syncany.operations.watch.WatchOperation;
import org.syncany.util.PidFileUtil;

import com.google.common.eventbus.Subscribe;

/**
 * This operation is the central part of the daemon. It can manage many different
 * {@link WatchOperation}s and exposes a web socket server to control and query the 
 * daemon. It furthermore offers a file-based control server to stop and reload the
 * daemon.
 * 
 * <p>When started via {@link #execute()}, the operation starts the following core
 * components:
 * 
 * <ul>
 *  <li>The {@link DaemonWatchServer} starts a {@link WatchOperation} for every 
 *      folder registered in the <tt>daemon.xml</tt> file. It can be reloaded via
 *      the <tt>syd reload</tt> command.</li>
 *  <li>The {@link DaemonWebServer} starts a websocket and allows clients 
 *      (e.g. GUI, Web) to control the daemon (if authenticated). 
 *      TODO [medium] This is not yet implemented!</li>
 *  <li>The {@link DaemonControlServer} creates and watches the daemon control file
 *      which allows the <tt>syd</tt> shell/batch script to write reload/shutdown
 *      commands.</li>  
 * </ul>
 * 
 * @author Vincent Wiencek <vwiencek@gmail.com>
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class DaemonOperation extends Operation {	
	private static final Logger logger = Logger.getLogger(DaemonOperation.class.getSimpleName());
	private static final String PID_FILE = "daemon.pid";
	private static final String DAEMON_FILE = "daemon.xml";
	private static final String DEFAULT_FOLDER = "Syncany";

	private File pidFile;
	private File daemonConfigFile;
	
	private DaemonWebServer webServer;
	private DaemonWatchServer watchServer;
	private DaemonControlServer controlServer;
	private DaemonEventBus eventBus;
	private DaemonConfigTO daemonConfig;

	public DaemonOperation(Config config) {
		super(config);
		
		this.pidFile = new File(UserConfig.getUserConfigDir(), PID_FILE);
		this.daemonConfigFile = new File(UserConfig.getUserConfigDir(), DAEMON_FILE);
	}

	@Override
	public OperationResult execute() throws Exception {		
		logger.log(Level.INFO, "Starting daemon operation ...");
		
		startOperation();
		return null;
	}

	private void startOperation() throws Exception {
		if (PidFileUtil.isProcessRunning(pidFile)) {
			throw new ServiceAlreadyStartedException("Syncany daemon already running.");
		}
		
		PidFileUtil.createPidFile(pidFile);
		
		initEventBus();		
		loadOrCreateConfig();
		
		startWebServer();
		startWatchServer();
		
		enterControlLoop(); // This blocks until SHUTDOWN is received!
	}
	
	@Subscribe
	public void onControlCommand(ControlCommand controlCommand) {
		switch (controlCommand) {
		case SHUTDOWN:
			logger.log(Level.INFO, "SHUTDOWN requested.");
			stopOperation();
			break;
			
		case RELOAD:
			logger.log(Level.INFO, "RELOAD requested.");
			reloadOperation();
			break;
		}
	}
	
	private void initEventBus() {
		eventBus = DaemonEventBus.getInstance();
		eventBus.register(this);
	}

	private void stopOperation() {
		stopWebSocketServer();
		stopWatchServer();
	}

	private void stopWebSocketServer() {
		if (webServer != null) {
			logger.log(Level.INFO, "Stopping web server ...");
			webServer.stop();
		}
		else {
			logger.log(Level.INFO, "Not stopping web server (not running)");			
		}
	}

	private void stopWatchServer() {
		logger.log(Level.INFO, "Stopping watch server ...");
		watchServer.stop();
	}

	private void startWebServer() throws Exception {
		if (daemonConfig.getWebServer().isEnabled()) {
			logger.log(Level.INFO, "Starting web server ...");

			webServer = new DaemonWebServer(daemonConfig);
			webServer.start();
		}
		else {
			logger.log(Level.INFO, "Not starting web server (disabled in confi)");
		}
	}

	private void startWatchServer() throws ConfigException {
		logger.log(Level.INFO, "Starting watch server ...");

		watchServer = new DaemonWatchServer();
		watchServer.start(daemonConfig);
	}

	private void enterControlLoop() throws IOException, ServiceAlreadyStartedException {
		logger.log(Level.INFO, "Starting daemon control server ...");

		controlServer = new DaemonControlServer();
		controlServer.enterLoop(); // This blocks! 
	}
	
	private void loadOrCreateConfig() {
		try {
			if (daemonConfigFile.exists()) {
				daemonConfig = DaemonConfigTO.load(daemonConfigFile);
			}
			else {
				daemonConfig = createAndWriteDefaultConfig(daemonConfigFile);
			}
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Cannot (re-)load config. Exception thrown.", e);
		}
	}
	
	private void reloadOperation() {
		loadOrCreateConfig();
		
		watchServer.reload(daemonConfig);
		// webServer.reload(daemonConfig); << Implement this
	}

	private DaemonConfigTO createAndWriteDefaultConfig(File configFile) {
		File defaultFolder = new File(System.getProperty("user.home"), DEFAULT_FOLDER);
		
		FolderTO defaultFolderTO = new FolderTO();
		defaultFolderTO.setPath(defaultFolder.getAbsolutePath());
		
		ArrayList<FolderTO> folders = new ArrayList<>();
		folders.add(defaultFolderTO);
		
		DaemonConfigTO defaultDaemonConfigTO = new DaemonConfigTO();
		defaultDaemonConfigTO.setFolders(folders);
		
		try {
			DaemonConfigTO.save(defaultDaemonConfigTO, configFile);
		}
		catch (Exception e) {
			// Don't care!
		}
		
		return defaultDaemonConfigTO;
	}
}
