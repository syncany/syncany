package org.syncany.database;

import java.io.File;
import java.io.IOException;

public interface DatabaseDAO {
	public void save(Database db, File destinationFile) throws IOException;	
	public void save(Database db, DatabaseVersion versionFrom, DatabaseVersion versionTo, File destinationFile) throws IOException;
	public void load(Database db, File databaseFile) throws IOException;
	public void load(Database db, File databaseFile, VectorClock fromVersion, VectorClock toVersion) throws IOException;
}
