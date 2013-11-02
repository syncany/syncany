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
	public OperationResult execute() throws Exception {
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
	
}
