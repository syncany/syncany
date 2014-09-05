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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.config.ConfigException;
import org.syncany.config.UserConfig;
import org.syncany.config.to.DaemonConfigTO;
import org.syncany.config.to.PortTO;
import org.syncany.config.to.UserTO;
import org.syncany.crypto.CipherUtil;
import org.syncany.operations.Operation;
import org.syncany.operations.OperationResult;
import org.syncany.operations.daemon.ControlServer.ControlCommand;
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
 *  <li>The {@link WatchServer} starts a {@link WatchOperation} for every 
 *      folder registered in the <tt>daemon.xml</tt> file. It can be reloaded via
 *      the <tt>syd reload</tt> command.</li>
 *  <li>The {@link WebServer} starts a websocket and allows clients 
 *      (e.g. GUI, Web) to control the daemon (if authenticated). 
 *      TODO [medium] This is not yet implemented!</li>
 *  <li>The {@link ControlServer} creates and watches the daemon control file
 *      which allows the <tt>syd</tt> shell/batch script to write reload/shutdown
 *      commands.</li>  
 * </ul>
 * 
 * @author Vincent Wiencek <vwiencek@gmail.com>
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Pim Otte 
 */
public class DaemonOperation extends Operation {	
	private static final Logger logger = Logger.getLogger(DaemonOperation.class.getSimpleName());	
	private static final String PID_FILE = "daemon.pid";

	private File pidFile;
	
	private WebServer webServer;
	private WatchServer watchServer;
	private ControlServer controlServer;
	private LocalEventBus eventBus;
	private DaemonConfigTO daemonConfig;
	private PortTO portTO;

	public DaemonOperation(Config config) {
		super(config);		
		this.pidFile = new File(UserConfig.getUserConfigDir(), PID_FILE);		
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
	
	// General initialization functions. These create the EventBus and control loop.	
	
	private void initEventBus() {
		eventBus = LocalEventBus.getInstance();
		eventBus.register(this);
	}

	private void enterControlLoop() throws IOException, ServiceAlreadyStartedException {
		logger.log(Level.INFO, "Starting daemon control server ...");

		controlServer = new ControlServer();
		controlServer.enterLoop(); // This blocks! 
	}

	// General stopping and reloading functions

	private void stopOperation() {
		stopWebServer();
		stopWatchServer();
	}
	
	private void reloadOperation() {
		loadOrCreateConfig();		
		watchServer.reload(daemonConfig);
	}
	
	// Config related functions. Used on starting and reloading.
	
	private void loadOrCreateConfig() {
		try {
			File daemonConfigFile = new File(UserConfig.getUserConfigDir(), UserConfig.DAEMON_FILE);
			File daemonConfigFileExample = new File(UserConfig.getUserConfigDir(), UserConfig.DAEMON_EXAMPLE_FILE);
			
			if (daemonConfigFile.exists()) {
				daemonConfig = DaemonConfigTO.load(daemonConfigFile);
			}
			else {
				// Write example config to daemon-example.xml, and default config to daemon.xml
				UserConfig.createAndWriteExampleDaemonConfig(daemonConfigFileExample);								
				daemonConfig = UserConfig.createAndWriteDefaultDaemonConfig(daemonConfigFile);
			}
			
			// Add user and password for access from the CLI
			if (daemonConfig.getPortTO() == null && portTO == null) {
				// Access info has not been created yet, generate new user-password pair
				String accessToken = CipherUtil.createRandomAlphabeticString(20);
				
				UserTO cliUser = new UserTO();
				cliUser.setUsername(UserConfig.USER_CLI);
				cliUser.setPassword(accessToken);
				
				portTO = new PortTO();
				
				portTO.setPort(daemonConfig.getWebServer().getBindPort());
				portTO.setUser(cliUser);
				
				daemonConfig.setPortTO(portTO);
			}
			else if (daemonConfig.getPortTO() == null) {
				// Access info is not included in the daemon config, but exists. Happens when reloading.
				// We reload the information about the port, but keep the access token the same.
				
				portTO.setPort(daemonConfig.getWebServer().getBindPort());
				daemonConfig.setPortTO(portTO);
			}
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Cannot (re-)load config. Exception thrown.", e);
		}
	}		

	// Web server starting and stopping functions
	
	private void startWebServer() throws Exception {
		if (daemonConfig.getWebServer().isEnabled()) {
			logger.log(Level.INFO, "Starting web server ...");

			webServer = new WebServer(daemonConfig);
			webServer.start();
		}
		else {
			logger.log(Level.INFO, "Not starting web server (disabled in confi)");
		}
	}
	
	private void stopWebServer() {
		if (webServer != null) {
			logger.log(Level.INFO, "Stopping web server ...");
			webServer.stop();
		}
		else {
			logger.log(Level.INFO, "Not stopping web server (not running)");			
		}
	}
	
	// Watch server starting and stopping functions
	
	private void startWatchServer() throws ConfigException {
		logger.log(Level.INFO, "Starting watch server ...");

		watchServer = new WatchServer();
		watchServer.start(daemonConfig);
	}

	private void stopWatchServer() {
		logger.log(Level.INFO, "Stopping watch server ...");
		watchServer.stop();
	}
}
