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

import java.util.logging.Level;
import java.util.logging.Logger;

public class DaemonWebSocketHandler {
	private static final Logger logger = Logger.getLogger(DaemonWebSocketHandler.class.getSimpleName());
	private DaemonWebSocketServer webSocketServer;

	public DaemonWebSocketHandler(DaemonWebSocketServer webSocketServer) {
		this.webSocketServer = webSocketServer;
	}

	public void handle(String message) {
		logger.log(Level.INFO, "Web socket message received: " + message);
		logger.log(Level.WARNING, "--> NO MESSAGE HANDLING IMPLEMENTED YET.");
		
		webSocketServer.sendToAll("{code: 400, message: \"Not supported\"}");
	}

}
