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

import org.syncany.config.Config;
import org.syncany.daemon.exception.ServiceAlreadyStartedException;
import org.syncany.operations.Operation;
import org.syncany.operations.OperationResult;
import org.syncany.operations.daemon.websocket.DaemonWebSocketServer;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 */
public class DaemonOperation extends Operation implements ShutdownListener {	
	private DaemonWebSocketServer webSocketServer;
	private DaemonControlServer controlServer;

	public DaemonOperation(Config config) {
		super(config);
	}

	@Override
	public OperationResult execute() throws Exception {		
		startWebSocketServer();
		startDaemonControlLoop();	
		
		return null;
	}
	
	private void startWebSocketServer() throws ServiceAlreadyStartedException {
		webSocketServer = new DaemonWebSocketServer();
		webSocketServer.start(null);
	}

	private void startDaemonControlLoop() throws IOException {
		controlServer = new DaemonControlServer(this);
		controlServer.enterLoop(); // This blocks! 
	}

	@Override
	public void onDaemonShutdown() {
		webSocketServer.stop();		
	}
}
