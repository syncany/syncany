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

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class DaemonWatchServer {	
	private static final Logger logger = Logger.getLogger(DaemonWatchServer.class.getSimpleName());
	
	private Map<String, WatchOperationThread> watchOperations;
	
	public DaemonWatchServer() {
		this.watchOperations = new TreeMap<String, WatchOperationThread>();
	}
	
	public void start() {
		logger.log(Level.INFO, "Starting watch server ...  NOT IMPLEMENTED");
		// TODO This should load from ~/.config/syncany/daemon.xml
	}
	
	public void stop() {
		logger.log(Level.INFO, "Stopping watch server ...  NOT IMPLEMENTED");
		
		for (WatchOperationThread watchOperationThread : watchOperations.values()) {
			watchOperationThread.stop();
		}
	}
}
