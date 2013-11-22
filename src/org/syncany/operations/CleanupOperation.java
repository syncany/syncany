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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;

public class CleanupOperation extends Operation {

	private CleanupOperationOptions options;
	private Database database; 
	
	public CleanupOperation(Config config) {
		this(config, null);
	}
	
	public CleanupOperation(Config config, Database database) {
		this(config, database, new CleanupOperationOptions());
	}
	
	public CleanupOperation(Config config, Database database, CleanupOperationOptions options) {
		super(config);
		this.options = options;
		this.database = database;
	}

	@Override
	public CleanupOperationResult execute() throws Exception {
		//1. Identify DatabaseVersions older than x days
		List<DatabaseVersion> identifiedDatabaseVersions = identifyDatabaseVersions(options);
		
		if(!identifiedDatabaseVersions.isEmpty()) {
			//2. if > 1 -> Write Lockfile to repository
			
		} 
		else {
			//Nothing to do
			return null;
		}
		//3. Cleanup every FileVersion older than x days which is not used locally
		//4. if DatabaseVersion is empty -> delete
		//5. 
		
		return null;
	}
	
	public List<DatabaseVersion> identifyDatabaseVersions(CleanupOperationOptions options) {
		List<DatabaseVersion> identifiedDatabaseVersions = new ArrayList<DatabaseVersion>();
		
		switch(options.getStrategy()) {
		case DAYRANGE:
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, -options.getCleanUpOlderThanDays());  			

			Date expiration = calendar.getTime();
			List<DatabaseVersion> existingDatabaseVersions = this.database.getDatabaseVersions();
			
			// TODO [medium] Performance: inefficient
			for (DatabaseVersion databaseVersion : existingDatabaseVersions) {
				if(databaseVersion.getTimestamp().before(expiration)) {
					identifiedDatabaseVersions.add(databaseVersion);
				}
			}
			break;
		case FILE_VERSION:
			break;	
		}
		
		return identifiedDatabaseVersions;
	}

	public static enum CleanupOperationStrategy {
		DAYRANGE,
		FILE_VERSION
	}
	
	public static class CleanupOperationOptions implements OperationOptions {
		private Integer cleanUpOlderThanDays;
		private CleanupOperationStrategy strategy; 
		
		public Integer getCleanUpOlderThanDays() {
			return cleanUpOlderThanDays;
		}

		public void setCleanUpOlderThanDays(Integer cleanUpOlderThanDays) {
			this.cleanUpOlderThanDays = cleanUpOlderThanDays;
		}

		public CleanupOperationStrategy getStrategy() {
			return strategy;
		}

		public void setStrategy(CleanupOperationStrategy strategy) {
			this.strategy = strategy;
		}
		
	}
	
	public static class CleanupOperationResult implements OperationResult {
		// Nothing
	}
	
}
