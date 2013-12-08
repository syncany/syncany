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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileId;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.PartialFileHistory;
import org.syncany.util.ByteArray;

import com.google.common.io.FileBackedOutputStream;

/**
 * TODO [low] Cleanup is not implemented. This class is a stub.
 */
public class CleanupOperation extends Operation {
	private static final Logger logger = Logger.getLogger(CleanupOperation.class.getSimpleName());

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
		if (options.getStrategy() == CleanupStrategy.DAYRANGE) {
			cleanupByExpirationDate();
		}
		else {
			throw new Exception("The given strategy is not supported: " + options.getStrategy());
		}
		// 3. Cleanup every FileVersion older than x days which is not used locally
		// 4. if DatabaseVersion is empty -> delete
		// 5.

		return null;
	}

	private void cleanupByExpirationDate() throws Exception {
		// Expiration date
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -1 * options.getCleanUpOlderThanDays());

		Date expirationDate = calendar.getTime();
	
		// 1. Identify DatabaseVersions older than x days
		List<DatabaseVersion> olderDatabaseVersions = identifyOlderDatabaseVersions(expirationDate);

		if (olderDatabaseVersions.size() > 0) {
			logger.log(Level.INFO, "Identified {0} potentially outdated database version(s).", olderDatabaseVersions.size());

			List<PartialFileHistory> outdatedFileHistories = identifyOutdatedFileHistories(expirationDate, olderDatabaseVersions);
			
			
			
		}	
		else {
			logger.log(Level.INFO, "Nothing to cleanup (none of the database versions are old enough).");
			return;
		}

		// 2. if > 1 -> Write Lockfile to repository


	}

	private List<PartialFileHistory> identifyOutdatedFileHistories(Date expiration, List<DatabaseVersion> olderDatabaseVersions) {
		Map<FileId, PartialFileHistory> outdatedFileHistories = new HashMap<FileId, PartialFileHistory>();
		
		for (DatabaseVersion databaseVersion : olderDatabaseVersions) {
			for (PartialFileHistory fileHistory : databaseVersion.getFileHistories()) {
				Iterator<Long> descendingVersionIterator = fileHistory.getDescendingVersionNumber();
				
				while (descendingVersionIterator.hasNext()) {
					Long fileVersionNumber = descendingVersionIterator.next();					
					FileVersion fileVersion = fileHistory.getFileVersion(fileVersionNumber);
					
					boolean outdatedFileVersion = false;
					boolean expiredFileVersion = fileVersion.getUpdated().before(expiration);
					
					if (expiredFileVersion) {
						boolean lastFileVersion = fileHistory.getLastVersion().equals(fileVersion);
						
						if (lastFileVersion) {
							boolean deletedFileVersion = fileVersion.getStatus() == FileStatus.DELETED;

							if (deletedFileVersion) {
								outdatedFileVersion = true;							
							}
						}
						else {
							outdatedFileVersion = true;							
						}

						// Add to outdated list
						if (outdatedFileVersion) {
							PartialFileHistory outdatedFileHistory = outdatedFileHistories.get(fileHistory.getFileId());

							if (outdatedFileHistory == null) {
								outdatedFileHistory = new PartialFileHistory(fileHistory.getFileId());
								outdatedFileHistories.put(fileHistory.getFileId(), fileHistory);
							}
							
							logger.log(Level.FINE, "- File "+fileHistory.getFileId()+", version "+fileVersion.getVersion()+" outdated: "+fileVersion);
							outdatedFileHistory.addFileVersion(fileVersion);
						}
					}					
				}								
			}
		}
		
		return new ArrayList<PartialFileHistory>(outdatedFileHistories.values());
	}

	public List<DatabaseVersion> identifyOlderDatabaseVersions(Date expiration) throws Exception {
		List<DatabaseVersion> identifiedDatabaseVersions = new ArrayList<DatabaseVersion>();

		// TODO [medium] Performance: inefficient
		for (DatabaseVersion databaseVersion : database.getDatabaseVersions()) {
			if (databaseVersion.getTimestamp().before(expiration)) {
				identifiedDatabaseVersions.add(databaseVersion);
			}
		}

		return identifiedDatabaseVersions;
	}

	public static enum CleanupStrategy {
		DAYRANGE, FILE_VERSION
	}

	public static class CleanupOperationOptions implements OperationOptions {
		private Integer cleanUpOlderThanDays;
		private CleanupStrategy strategy;

		public Integer getCleanUpOlderThanDays() {
			return cleanUpOlderThanDays;
		}

		public void setCleanUpOlderThanDays(Integer cleanUpOlderThanDays) {
			this.cleanUpOlderThanDays = cleanUpOlderThanDays;
		}

		public CleanupStrategy getStrategy() {
			return strategy;
		}

		public void setStrategy(CleanupStrategy strategy) {
			this.strategy = strategy;
		}

	}

	public static class CleanupOperationResult implements OperationResult {
		// Nothing
	}

}
