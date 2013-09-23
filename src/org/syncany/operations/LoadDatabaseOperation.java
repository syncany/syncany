package org.syncany.operations;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.DatabaseDAO;
import org.syncany.database.DatabaseXmlDAO;

public class LoadDatabaseOperation extends Operation {
	private static final Logger logger = Logger.getLogger(LoadDatabaseOperation.class.getSimpleName());
	
	public LoadDatabaseOperation(Config config) {
		super(config);
	}	
	
	public OperationResult execute() throws Exception {
		File localDatabaseFile = config.getDatabaseFile();
		logger.log(Level.INFO, "Loading local database file from "+localDatabaseFile+" ...");
		
		Database db = new Database();
		DatabaseDAO dao = new DatabaseXmlDAO(config.getTransformer());
		
		if (localDatabaseFile.exists()) {
			dao.load(db, localDatabaseFile);
		}
		else {
			logger.log(Level.INFO, "- NOT loading. File does not exist.");
		}
		
		return new LoadDatabaseOperationResult(db);
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
