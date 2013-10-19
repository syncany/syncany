package org.syncany.operations;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.DatabaseDAO;
import org.syncany.database.XmlDatabaseDAO;

public class LoadDatabaseOperation extends Operation {
	private static final Logger logger = Logger.getLogger(LoadDatabaseOperation.class.getSimpleName());
	private File databaseFile;
	
	public LoadDatabaseOperation(Config config) {
		this(config, config.getDatabaseFile());
	}
	
	public LoadDatabaseOperation(Config config, File databaseFile) {
		super(config);
		this.databaseFile = databaseFile;
	}
	
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "Loading database file from "+databaseFile+" ...");
		
		Database db = new Database();
		DatabaseDAO dao = new XmlDatabaseDAO(config.getTransformer());
		
		if (databaseFile.exists()) {
			dao.load(db, databaseFile);
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
