package org.syncany.operations;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.DatabaseDAO;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseXmlDAO;

public abstract class Operation {
	private static final Logger logger = Logger.getLogger(Operation.class.getSimpleName());
	protected Config config;
	
	public Operation(Config config) {
		this.config = config;
	}	

	protected Database loadLocalDatabase(File localDatabaseFile) throws IOException {
		logger.log(Level.INFO, "Loading local database file from "+localDatabaseFile+" ...");
		
		Database db = new Database();

		DatabaseDAO dao = new DatabaseXmlDAO(config.getTransformer());
		
		if (localDatabaseFile.exists()) {
			dao.load(db, localDatabaseFile);
		}
		else {
			logger.log(Level.INFO, "- NOT loading. File does not exist.");
		}
		
		return db;
	}		

	protected void saveLocalDatabase(Database db, File localDatabaseFile) throws IOException {
		saveLocalDatabase(db, null, null, localDatabaseFile);
	}	
	
	protected void saveLocalDatabase(Database db, DatabaseVersion fromVersion, DatabaseVersion toVersion, File localDatabaseFile) throws IOException {
		DatabaseDAO dao = new DatabaseXmlDAO(config.getTransformer());
		dao.save(db, fromVersion, toVersion, localDatabaseFile);
	}	
	
	public abstract OperationResult execute() throws Exception;
	
	public interface OperationOptions {
		// Marker interface for type safety
	}
	
	public interface OperationResult {
		// Marker interface for type safety
	}
}
