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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.VectorClock;
import org.syncany.database.XmlDatabaseDAO;

/**
 * WARNING:
 * This class is a stub. It's used for experimental purposes to figure out what queries are required for a DB backend.
 * 
 * TODO [low] Cleanup is not implemented. 
 */
public class CleanupOperation extends Operation {
	private static final Logger logger = Logger.getLogger(CleanupOperation.class.getSimpleName());

	private CleanupOperationOptions options;
	private CleanupOperationResult result;
	private Database localDatabase;

	public CleanupOperation(Config config, Database database, CleanupOperationOptions options) {
		super(config);

		this.options = options;
		this.localDatabase = database;
	}

	@Override
	public CleanupOperationResult execute() throws Exception {
		localDatabase = (localDatabase != null) ? localDatabase : loadLocalDatabase();

		if (options.getStrategy() == CleanupStrategy.EXPIRATION_DATE) {
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
		Date expirationDate = new Date(System.currentTimeMillis() - options.getCleanUpOlderThanSeconds()*1000);
		
		// 1. Identify DatabaseVersions older than x days
		List<DatabaseVersion> olderDatabaseVersions = identifyOlderDatabaseVersions(expirationDate);

		if (olderDatabaseVersions.size() > 0) {
			logger.log(Level.INFO, "Identified {0} potentially outdated database version(s).", olderDatabaseVersions.size());

			DatabaseVersion purgeDatabaseVersion = new DatabaseVersion();
			addOutdatedFileHistories(expirationDate, olderDatabaseVersions, purgeDatabaseVersion);
			
			if (purgeDatabaseVersion.getFileHistories().size() > 0) {
				logger.log(Level.INFO, "Identified {0} file histories with outdated file versions.", purgeDatabaseVersion.getFileHistories().size());
				
				addUnusedFileContents(purgeDatabaseVersion);
				addUnusedChunks(purgeDatabaseVersion);
				// identify stale file contents
				// identify if chunks in stale file contents can be removed 
				//     by checking if they appear in any other contents which are used in file versions that are not outdated
				
				//identifyStaleChunks(purgeDatabaseVersion);
				// identify chunks
				// identify multichunks
				VectorClock vectorClock = new VectorClock();
				vectorClock.setClock("test", 1);
				purgeDatabaseVersion.setTimestamp(new Date());
				purgeDatabaseVersion.setClient(config.getMachineName());
				purgeDatabaseVersion.setVectorClock(vectorClock);
				
				Database purgeDatabase = new Database();
				purgeDatabase.addDatabaseVersion(purgeDatabaseVersion);
				
				new XmlDatabaseDAO().save(purgeDatabase, new File("/tmp/test"));
			}
		}	
		else {
			logger.log(Level.INFO, "Nothing to cleanup (none of the database versions are old enough).");
			return;
		}

		// 2. if > 1 -> Write Lockfile to repository


	}

	private void addUnusedChunks(DatabaseVersion purgeDatabaseVersion) {
		for (FileContent fileContent : purgeDatabaseVersion.getFileContents()) {
			for (ChunkChecksum chunkChecksum : fileContent.getChunks()) {
				// THIS IS NOT IMPLEMENTED. BECAUSE THIS WOULD BE HORRIBLY, HORRIBLY INEFFICIENT				
			}			
		}
		
	}		

	private void addUnusedFileContents(DatabaseVersion purgeDatabaseVersion) {
		for (PartialFileHistory outdatedPartialFileHistory : purgeDatabaseVersion.getFileHistories()) {
			for (FileVersion outdatedFileVersion : outdatedPartialFileHistory.getFileVersions().values()) {
				boolean isFileWithContent = outdatedFileVersion.getChecksum() != null;
				
				if (isFileWithContent) {
					boolean contentOutdated = true;
					
					FileContent potentiallyOutdatedFileContent = localDatabase.getContent(outdatedFileVersion.getChecksum());
					List<PartialFileHistory> fileHistoriesWithSameChecksum = localDatabase.getFileHistories(outdatedFileVersion.getChecksum());
					
					if (fileHistoriesWithSameChecksum != null) {
						for (PartialFileHistory fileHistoryWithSameChecksum : fileHistoriesWithSameChecksum) {
							boolean isSameFileHistory = fileHistoryWithSameChecksum.getFileId().equals(outdatedPartialFileHistory.getFileId()); 
							
							if (!isSameFileHistory) {
								boolean isFileHistoryStillUsed = null == purgeDatabaseVersion.getFileHistory(fileHistoryWithSameChecksum.getFileId());
								
								if (isFileHistoryStillUsed) {
									contentOutdated = false;
									break;
								}
							}
						}
					}
					
					if (contentOutdated) {
						logger.log(Level.INFO, "- File content {0} is not used by any active file version, marked for deletion.", potentiallyOutdatedFileContent.getChecksum());
						purgeDatabaseVersion.addFileContent(potentiallyOutdatedFileContent);
					}
				}
			}
		}
	}

	private void identifyStaleChunks(DatabaseVersion purgeDatabaseVersion) {
		for (PartialFileHistory outdatedPartialFileHistory : purgeDatabaseVersion.getFileHistories()) {
			for (FileVersion outdatedFileVersion : outdatedPartialFileHistory.getFileVersions().values()) {
				if (outdatedFileVersion.getChecksum() != null) {
					FileContent fileContent = localDatabase.getContent(outdatedFileVersion.getChecksum());
				
					for (ChunkChecksum potentiallyStaleChunkChecksum : fileContent.getChunks()) {
						//localDatabase.get
						
						// get all file contents the chunk is used in
						// check if the file contents will be removed by this cleanup
					}
				}
				
			}
		}
		
	}

	private void addOutdatedFileHistories(Date expirationDate, List<DatabaseVersion> olderDatabaseVersions, DatabaseVersion purgeDatabaseVersion) {
		for (DatabaseVersion databaseVersion : olderDatabaseVersions) {
			for (PartialFileHistory partialFileHistory : databaseVersion.getFileHistories()) {
				PartialFileHistory fullFileHistory = localDatabase.getFileHistory(partialFileHistory.getFileId());
				Iterator<Long> descendingVersionIterator = partialFileHistory.getDescendingVersionNumber();
				
				while (descendingVersionIterator.hasNext()) {
					Long fileVersionNumber = descendingVersionIterator.next();					
					FileVersion fileVersion = partialFileHistory.getFileVersion(fileVersionNumber);
					
					boolean outdatedFileVersion = false;
					boolean expiredFileVersion = fileVersion.getUpdated().before(expirationDate);
					
					if (expiredFileVersion) {
						boolean lastFileVersion = fullFileHistory.getLastVersion().equals(fileVersion);
						
						if (lastFileVersion) {
							boolean deletedFileVersion = fileVersion.getStatus() == FileStatus.DELETED;

							if (deletedFileVersion) {
								logger.log(Level.INFO, "- Outdated (last, but deleted!): File "+fileVersion.getPath()+", "+partialFileHistory.getFileId()+", version "+fileVersion.getVersion());
								outdatedFileVersion = true;							
							}
							else {
								logger.log(Level.INFO, "- Up-to-date (outdated, but not deleted): File "+fileVersion.getPath()+", "+partialFileHistory.getFileId()+", version "+fileVersion.getVersion());								
							}
						}
						else {
							logger.log(Level.INFO, "- Outdated (not last): File "+fileVersion.getPath()+", "+partialFileHistory.getFileId()+", version "+fileVersion.getVersion());
							outdatedFileVersion = true;							
						}

						// Add to outdated list
						if (outdatedFileVersion) {
							PartialFileHistory outdatedFileHistory = purgeDatabaseVersion.getFileHistory(partialFileHistory.getFileId());

							if (outdatedFileHistory == null) {
								outdatedFileHistory = new PartialFileHistory(partialFileHistory.getFileId());
								purgeDatabaseVersion.addFileHistory(partialFileHistory);
							}
							
							outdatedFileHistory.addFileVersion(fileVersion);
						}
					}
					else {
						logger.log(Level.INFO, "- Up-to-date (not outdated): File "+fileVersion.getPath()+", "+partialFileHistory.getFileId()+", version "+fileVersion.getVersion());								
					}
				}								
			}
		}
	}

	public List<DatabaseVersion> identifyOlderDatabaseVersions(Date expiration) throws Exception {
		List<DatabaseVersion> identifiedDatabaseVersions = new ArrayList<DatabaseVersion>();

		// TODO [medium] Performance: inefficient
		for (DatabaseVersion databaseVersion : localDatabase.getDatabaseVersions()) {
			if (databaseVersion.getTimestamp().before(expiration)) {
				identifiedDatabaseVersions.add(databaseVersion);
			}
		}

		return identifiedDatabaseVersions;
	}

	public static enum CleanupStrategy {
		EXPIRATION_DATE, FILE_VERSION
	}

	public static class CleanupOperationOptions implements OperationOptions {
		private Integer cleanUpOlderThanSeconds;
		private CleanupStrategy strategy;

		public Integer getCleanUpOlderThanSeconds() {
			return cleanUpOlderThanSeconds;
		}

		public void setCleanUpOlderThanSeconds(Integer cleanUpOlderThanSeconds) {
			this.cleanUpOlderThanSeconds = cleanUpOlderThanSeconds;
		}

		public CleanupStrategy getStrategy() {
			return strategy;
		}

		public void setStrategy(CleanupStrategy strategy) {
			this.strategy = strategy;
		}

	}

	public static class CleanupOperationResult implements OperationResult {
		private List<PartialFileHistory> deletedOutdatedFileHistories;

		public List<PartialFileHistory> getDeletedOutdatedFileHistories() {
			return deletedOutdatedFileHistories;
		}

		public void setDeletedOutdatedFileHistories(List<PartialFileHistory> deletedOutdatedFileHistories) {
			this.deletedOutdatedFileHistories = deletedOutdatedFileHistories;
		}				
	}
}
