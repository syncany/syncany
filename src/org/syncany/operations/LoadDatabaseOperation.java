package org.syncany.operations;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.Database;

public class LoadDatabaseOperation extends Operation {
	private static final Logger logger = Logger.getLogger(LoadDatabaseOperation.class.getSimpleName());
	
	public LoadDatabaseOperation(Config config) {
		super(config);
	}	
	
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "Loading local database ...");		
		Database database = loadLocalDatabase(config.getDatabaseFile());		
		
		return new LoadDatabaseOperationResult(database);
	}		
	
	public class LoadDatabaseOperationResult implements OperationResult {
		private Database database;

		public LoadDatabaseOperationResult(Database database) {
			this.database = database;
		}

		public Database getDatabase() {
			return database;
		}
	}
}
