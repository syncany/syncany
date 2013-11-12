/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.operations;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.operations.LoadDatabaseOperation.LoadDatabaseOperationResult;

public class WatchOperation extends Operation {
	private static final Logger logger = Logger.getLogger(WatchOperation.class.getSimpleName());
	private WatchOperationOptions options;
	
	public WatchOperation(Config config, WatchOperationOptions options) {
		super(config);
		this.options = options;
	}	
	
	public OperationResult execute() throws Exception {
		Database database = ((LoadDatabaseOperationResult) new LoadDatabaseOperation(config).execute()).getDatabase();
		
		while (true) {
			logger.log(Level.INFO, "Running sync ...");
			
			try {
				new DownOperation(config, database).execute();
				new UpOperation(config, database).execute();
				
				logger.log(Level.INFO, "Sync done, waiting {0} seconds ...", options.getInterval()/1000);
				Thread.sleep(options.getInterval());				
			}
			catch (Exception e) {
				logger.log(Level.INFO, "Sync FAILED, waiting {0} seconds ...", options.getInterval()/1000);
				Thread.sleep(options.getInterval());			
			}
		}
	}
	
	public static class WatchOperationOptions implements OperationOptions {
		private int interval = 30000;

		public int getInterval() {
			return interval;
		}

		public void setInterval(int interval) {
			this.interval = interval;
		}				
	}
	
	public static class WatchOperationResult implements OperationResult {
		// Fressen
	}
}
