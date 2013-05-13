package org.syncany.operations;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Profile;
import org.syncany.db.Database;
import org.syncany.db.DatabaseDAO;

public abstract class Operation {
	private static final Logger logger = Logger.getLogger(Operation.class.getSimpleName());
	protected Profile profile;
	
	public Operation(Profile profile) {
		this.profile = profile;
	}	

	protected Database loadLocalDatabase(File localDatabaseFile) throws IOException {
		logger.log(Level.INFO, "Loading local database file from "+localDatabaseFile+" ...");
		
		DatabaseDAO dao = new DatabaseDAO();
		Database db = new Database();

		if (localDatabaseFile.exists() && localDatabaseFile.isFile() && localDatabaseFile.canRead()) {
			dao.load(db, localDatabaseFile);
			System.out.println(db);
		}
		
		return db;
	}		

	protected void saveLocalDatabase(Database db, long fromVersion, long toVersion, File localDatabaseFile) throws IOException {
		DatabaseDAO dao = new DatabaseDAO();
		dao.save(db, fromVersion, toVersion, localDatabaseFile);
	}	
	
	public abstract void execute() throws Exception;
}
