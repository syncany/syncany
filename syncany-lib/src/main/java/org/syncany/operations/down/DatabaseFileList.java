/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.operations.down;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.database.DatabaseVersionHeader;

/**
 * Helper class to help map a database version header to its corresponding
 * downloaded remote database. This class is used by the {@link DownOperation}
 * to read the new/unknown remote databases. 
 *  
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class DatabaseFileList {
	private TreeMap<File, DatabaseRemoteFile> newRemoteDatabases;
	private Map<String, File> shortFilenameToFileMap;
	
	public DatabaseFileList(TreeMap<File, DatabaseRemoteFile> newRemoteDatabases) {
		this.newRemoteDatabases = newRemoteDatabases;
		this.shortFilenameToFileMap = initLookupMap();
	}

	private Map<String, File> initLookupMap() {
		// Make map 'short filename' -> 'full filename'
		Map<String, File> shortFilenameToFileMap = new HashMap<String, File>();

		for (File remoteDatabase : newRemoteDatabases.keySet()) {
			shortFilenameToFileMap.put(remoteDatabase.getName(), remoteDatabase);
		}
		
		return shortFilenameToFileMap;
	}

	/**
	 * Returns the database file for a given database version header, or <tt>null</tt> 
	 * if for this database version header no file has been downloaded.
	 * 
	 * <p>Unlike {@link #getNextDatabaseVersionFile(DatabaseVersionHeader, Map) getNextDatabaseVersionFile()},
	 * this method does <b>not</b> try to find a database file by counting up the local version. It returns
	 * null if the exact version has not been found!
	 * 
	 * <p><b>Example:</b> given database version header is A/(A3,B2)/T=..
	 * <pre>
	 *   - Does db-A-0003 exist? No, return null.
	 * </pre>
	 */
	public File getExactDatabaseVersionFile(DatabaseVersionHeader databaseVersionHeader) throws StorageException {
		String clientName = databaseVersionHeader.getClient();
		long clientFileClock = databaseVersionHeader.getVectorClock().getClock(clientName);
		
		DatabaseRemoteFile potentialDatabaseRemoteFileForRange = new DatabaseRemoteFile(clientName, clientFileClock);				
		return shortFilenameToFileMap.get(potentialDatabaseRemoteFileForRange.getName());
	}

	/**
	 * Returns a database file for a given database version header, or throws an error if
	 * no file has been found.
	 * 
	 * <p><b>Note:</b> Unlike {@link #getExactDatabaseVersionFile(DatabaseVersionHeader, Map) getExactDatabaseVersionFile()},
	 * this method tries to find a database file by counting up the local version, i.e. if the exact version cannot be found,
	 * it increases the local client version by one until a matching version is found.
	 * 
	 * <p><b>Example:</b> given database version header is A/(A3,B2)/T=..
	 * <pre>
	 *   - Does db-A-0003 exist? No, continue.
	 *   - Does db-A-0004 exist? No, continue.
	 *   - Does db-A-0005 exist. Yes, return db-A-0005.
	 * </pre>
	 */
	public File getNextDatabaseVersionFile(DatabaseVersionHeader databaseVersionHeader) throws StorageException {
		String clientName = databaseVersionHeader.getClient();
		long clientFileClock = databaseVersionHeader.getVectorClock().getClock(clientName);
		
		DatabaseRemoteFile potentialDatabaseRemoteFileForRange = null;
		File databaseFileForRange = null;
		
		int maxRounds = 100000; // TODO [medium] This is ugly and potentially dangerous. Can this lead to incorrect results?
		boolean isLoadableDatabaseFile = false;
		
		while (!isLoadableDatabaseFile && maxRounds > 0) {
			potentialDatabaseRemoteFileForRange = new DatabaseRemoteFile(clientName, clientFileClock);
			
			databaseFileForRange = shortFilenameToFileMap.get(potentialDatabaseRemoteFileForRange.getName());
			isLoadableDatabaseFile = databaseFileForRange != null;	
			
			maxRounds--;
			clientFileClock++;
		}
		
		if (!isLoadableDatabaseFile) {
			throw new StorageException("Cannot find suitable database remote file to load range.");
		}
		
		return databaseFileForRange;
	}
}
