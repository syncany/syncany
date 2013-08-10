package org.syncany.database;

import java.io.File;
import java.io.IOException;

import org.syncany.operations.DatabaseFile;

public interface DatabaseDAO {
	public void save(Database db, File destinationFile) throws IOException;	
	public void save(Database db, DatabaseVersion versionFrom, DatabaseVersion versionTo, File destinationFile) throws IOException;
	public void load(Database db, DatabaseFile databaseFile) throws IOException;
	public void load(Database db, DatabaseFile databaseFile, VectorClock fromVersion, VectorClock toVersion) throws IOException;
}
