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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.config.ConfigException;
import org.syncany.operations.Operation;
import org.syncany.operations.OperationResult;
import org.syncany.operations.watch.WatchOperation;

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
 *  <li>The {@link DaemonWebSocketServer} starts a websocket and allows clients 
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
public class DaemonOperation extends Operation implements DaemonControlListener {	
	private static final Logger logger = Logger.getLogger(DaemonOperation.class.getSimpleName());

	private DaemonWebSocketServer webSocketServer;
	private DaemonWatchServer watchServer;
	private DaemonControlServer controlServer;

	public DaemonOperation(Config config) {
		super(config);
	}

	@Override
	public OperationResult execute() throws Exception {		
		logger.log(Level.INFO, "Starting daemon operation ...");
		
		startOperation();
		return null;
	}

	private void startOperation() throws ServiceAlreadyStartedException, ConfigException, IOException {
		// startWebSocketServer();
		startWatchServer();
		
		startDaemonControlLoop(); // This blocks until SHUTDOWN is received!
	}
	
	private void stopOperation() {
		// stopWebSocketServer();
		stopWatchServer();
	}

	private void stopWebSocketServer() {
		logger.log(Level.INFO, "Stopping websocket server ...");
		webSocketServer.stop();
	}

	private void stopWatchServer() {
		logger.log(Level.INFO, "Stopping watch server ...");
		watchServer.stop();
	}

	private void startWebSocketServer() throws ServiceAlreadyStartedException {
		logger.log(Level.INFO, "Starting websocket server ...");

		webSocketServer = new DaemonWebSocketServer();
		webSocketServer.start();
	}

	private void startWatchServer() throws ConfigException {
		logger.log(Level.INFO, "Starting websocket server ...");

		watchServer = new DaemonWatchServer();
		watchServer.start();
	}

	private void startDaemonControlLoop() throws IOException, ServiceAlreadyStartedException {
		logger.log(Level.INFO, "Starting daemon control server ...");

		controlServer = new DaemonControlServer(this);
		controlServer.enterLoop(); // This blocks! 
	}

	@Override
	public void onDaemonShutdown() {
		logger.log(Level.INFO, "SHUTDOWN requested.");
		stopOperation();
	}

	@Override
	public void onDaemonReload() {
		logger.log(Level.INFO, "RELOAD requested.");
		watchServer.reload();
	}
}
