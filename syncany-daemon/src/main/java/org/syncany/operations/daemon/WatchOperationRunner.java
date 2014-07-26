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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.config.ConfigException;
import org.syncany.config.ConfigHelper;
import org.syncany.operations.watch.WatchOperation;
import org.syncany.operations.watch.WatchOperationListener;
import org.syncany.operations.watch.WatchOperationOptions;

/**
 * The watch operation thread runs a {@link WatchOperation} in a thread. The 
 * underlying thred can be started using the {@link #start()} method, and stopped
 * gracefully using {@link #stop()}. 
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class WatchOperationRunner {
	private static final Logger logger = Logger.getLogger(WatchOperationRunner.class.getSimpleName());

	private Config config;
	private Thread watchThread;
	private WatchOperation watchOperation;

	public WatchOperationRunner(File localDir, WatchOperationOptions watchOperationOptions, WatchOperationListener listener) throws ConfigException {
		File configFile = ConfigHelper.findLocalDirInPath(localDir);
		
		if (configFile == null) {
			throw new ConfigException("Config file in folder " + localDir + " not found.");
		}
		
		this.config = ConfigHelper.loadConfig(configFile);
		this.watchOperation = new WatchOperation(config, watchOperationOptions, listener);
	}
	
	public void start() throws ServiceAlreadyStartedException {
		watchThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					logger.log(Level.INFO, "STARTING watch at" + config.getLocalDir());
					
					watchOperation.execute();
					
					logger.log(Level.INFO, "STOPPED watch at " + config.getLocalDir());
				}
				catch (Exception e) {
					logger.log(Level.SEVERE, "ERROR while running watch at " + config.getLocalDir(), e);
				}
			}
		}, "WatchOpT/" + config.getLocalDir().getName());
		
		watchThread.start();
	}

	public void stop() {
		watchOperation.stop();
		watchThread = null;
	}
}
