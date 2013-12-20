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
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.chunk.Chunk;
import org.syncany.chunk.Deduper;
import org.syncany.chunk.DeduperListener;
import org.syncany.chunk.MultiChunk;
import org.syncany.config.Config;
import org.syncany.database.ChunkEntry;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileContent;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.FileVersionComparator;
import org.syncany.database.FileVersionComparator.FileProperties;
import org.syncany.database.FileVersionComparator.FileVersionComparison;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

/**
 * The indexer combines the chunking process with the corresponding database
 * lookups for the resulting chunks. It implements the deduplication mechanism 
 * of Syncany.
 * 
 * <p>The class takes a list of files as input and uses the {@link Deduper} to
 * break these files into individual chunks. By implementing the {@link DeduperListener},
 * it reacts on chunking events and creates a new database version (with the newly
 * added/changed/removed files. This functionality is entirely implemented by the
 * {@link #index(List) index()} method.
 * 
 * <p>The class uses the currently loaded {@link Database} as well as a potential  
 * dirty database into account. Lookups for chunks and file histories are performed 
 * on both databases.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Indexer {
	private static final Logger logger = Logger.getLogger(Indexer.class.getSimpleName());
	
	private Config config;
	private Deduper deduper;
	private Database database;
	private Database dirtyDatabase;
	
	public Indexer(Config config, Deduper deduper, Database database, Database dirtyDatabase) {
		this.config = config;
		this.deduper = deduper;
		this.database = database;
		this.dirtyDatabase = dirtyDatabase;
	}
	
	/**
	 * This method implements the index/deduplication functionality of Syncany. It uses a {@link Deduper}
	 * to break files down, compares them to the local database and creates a new {@link DatabaseVersion}
	 * as a result. 
	 * 
	 * <p>Depending on what has changed, the new database version will contain new instances of 
	 * {@link PartialFileHistory}, {@link FileVersion}, {@link FileContent}, {@link ChunkEntry} and 
	 * {@link MultiChunkEntry}.
	 * 
	 * @param files List of files to be deduplicated
	 * @return New database version containing new/changed/deleted entities
	 * @throws IOException If the chunking/deduplication cannot read/process any of the files
	 */
	// TODO [medium] Performance: To avoid having to parse the checksum twice (status and indexer), the status operation should pass over the FileProperties object in the ChangeSet
	public DatabaseVersion index(List<File> files) throws IOException {
		DatabaseVersion newDatabaseVersion = new DatabaseVersion();		
		
		// Add dirty database's chunks/multichunks/file contents
		if (dirtyDatabase != null) {
			addDirtyChunkData(newDatabaseVersion);
		}
		
		// Find and index new files
		deduper.deduplicate(files, new IndexerDeduperListener(newDatabaseVersion));			
		
		// Find and remove deleted files
		removeDeletedFiles(newDatabaseVersion);
		
		return newDatabaseVersion;
	}
	
	private void addDirtyChunkData(DatabaseVersion newDatabaseVersion) {
		logger.log(Level.INFO, "- Adding dirty chunks/multichunks/file contents (from dirty database) ...");
		for (DatabaseVersion dirtyDatabaseVersion : dirtyDatabase.getDatabaseVersions()) {
			logger.log(Level.FINER, "   + Adding "+dirtyDatabaseVersion.getChunks().size()+" chunks ...");
			for (ChunkEntry dirtyChunk : dirtyDatabaseVersion.getChunks()) {
				newDatabaseVersion.addChunk(dirtyChunk);
			}
			
			logger.log(Level.FINER, "   + Adding "+dirtyDatabaseVersion.getMultiChunks().size()+" multichunks ...");
			for (MultiChunkEntry dirtyMultiChunk : dirtyDatabaseVersion.getMultiChunks()) {
				newDatabaseVersion.addMultiChunk(dirtyMultiChunk);
			}
			
			logger.log(Level.FINER, "   + Adding "+dirtyDatabaseVersion.getFileContents().size()+" file contents ...");
			for (FileContent dirtyFileContent : dirtyDatabaseVersion.getFileContents()) {
				newDatabaseVersion.addFileContent(dirtyFileContent);
			}
		}
	}

	private void removeDeletedFiles(DatabaseVersion newDatabaseVersion) {
		logger.log(Level.FINER, "- Looking for deleted files ...");		

		for (PartialFileHistory fileHistory : database.getFileHistories()) {
			// Ignore this file history if it has been updated in this database version before (file probably renamed!)
			if (newDatabaseVersion.getFileHistory(fileHistory.getFileId()) != null) {
				continue;
			}
						
			// Check if file exists, remove if it doesn't
			FileVersion lastLocalVersion = fileHistory.getLastVersion();
			File lastLocalVersionOnDisk = new File(config.getLocalDir()+File.separator+lastLocalVersion.getPath());
			
			// Ignore this file history if the last version is marked "DELETED"
			if (lastLocalVersion.getStatus() == FileStatus.DELETED) {
				continue;
			}
			
			// Add this file history if a new file with this name has been added (file type change)
			PartialFileHistory newFileWithSameName = getFileHistoryByPathFromDatabaseVersion(newDatabaseVersion, fileHistory.getLastVersion().getPath());
			
			// If file has VANISHED, mark as DELETED			
			if (!FileUtil.exists(lastLocalVersionOnDisk) || newFileWithSameName != null) {
				PartialFileHistory deletedFileHistory = new PartialFileHistory(fileHistory.getFileId());
				FileVersion deletedVersion = (FileVersion) lastLocalVersion.clone();
				deletedVersion.setStatus(FileStatus.DELETED);
				deletedVersion.setVersion(fileHistory.getLastVersion().getVersion()+1);
				
				logger.log(Level.FINER, "  + Deleted: {0}, adding deleted version {1}", new Object[] { lastLocalVersion, deletedVersion });
				deletedFileHistory.addFileVersion(deletedVersion);
				newDatabaseVersion.addFileHistory(deletedFileHistory);			
			}
		}				
	}
	
	private PartialFileHistory getFileHistoryByPathFromDatabaseVersion(DatabaseVersion databaseVersion, String path) {
		// TODO [high] Extremely performance intensive, because this is called inside a loop above. Implement better caching for database version!!!
		for (PartialFileHistory fileHistory : databaseVersion.getFileHistories()) {
			FileVersion lastVersion = fileHistory.getLastVersion();
				
			if (lastVersion.getStatus() != FileStatus.DELETED && lastVersion.getPath().equals(path)) {
				return fileHistory;
			}
		}
		
		return null;
	}

	public static class IndexerException extends RuntimeException {
		private static final long serialVersionUID = 5247751938336036877L;

		public IndexerException(String message) {
			super(message);
		}
	}
	private class IndexerDeduperListener implements DeduperListener {
		private FileVersionComparator fileVersionComparator;
		private SecureRandom secureRandom;
		private DatabaseVersion newDatabaseVersion;
		private ChunkEntry chunkEntry;		
		private MultiChunkEntry multiChunkEntry;	
		private FileContent fileContent;
		
		private FileProperties startFileProperties;
		private FileProperties endFileProperties;		

		public IndexerDeduperListener(DatabaseVersion newDatabaseVersion) {
			this.fileVersionComparator = new FileVersionComparator(config.getLocalDir(), config.getChunker().getChecksumAlgorithm());
			this.secureRandom = new SecureRandom();
			this.newDatabaseVersion = newDatabaseVersion;
		}				

		@Override
		public boolean onFileFilter(File file) {
			logger.log(Level.FINER, "- +File {0}", file); 
			
			startFileProperties = fileVersionComparator.captureFileProperties(file, null, false);
			
			// Check if file has vanished
			if (!startFileProperties.exists() || startFileProperties.isLocked()) {
				logger.log(Level.FINER, "- /File: {0}", file);				
				logger.log(Level.INFO, "   * NOT ADDING because file has VANISHED (exists = {0}) or is LOCKED (locked = {1}).", new Object[] { startFileProperties.exists(), startFileProperties.isLocked() });
				
				resetFileEnd();
				return false;
			}
			
			// Content
			if (startFileProperties.getType() == FileType.FILE) {
				logger.log(Level.FINER, "- +FileContent: {0}", file);			
				fileContent = new FileContent();				
			}				
			
			return true;
		}
		
		@Override
		public boolean onFileStart(File file) {			
			return startFileProperties.getType() == FileType.FILE; // Ignore directories and symlinks!
		}

		@Override
		public void onFileEnd(File file, byte[] rawFileChecksum) {
			// Get file attributes (get them while file exists)
			// Note: Do NOT move any File-methods (file.anything()) below the file.exists()-part, 
			//       because the file could vanish!
			FileChecksum fileChecksum = (rawFileChecksum != null) ? new FileChecksum(rawFileChecksum) : null; 
			endFileProperties = fileVersionComparator.captureFileProperties(file, fileChecksum, false);
			
			// Check if file has vanished			
			boolean fileIsLocked = endFileProperties.isLocked();
			boolean fileVanished = !endFileProperties.exists();
			boolean fileHasChanged = startFileProperties.getSize() != endFileProperties.getSize() 
					|| startFileProperties.getLastModified() != endFileProperties.getLastModified();			
			
			if (fileVanished || fileIsLocked || fileHasChanged) {
				logger.log(Level.FINER, "- /File: {0}", file);				
				logger.log(Level.INFO, "   * NOT ADDING because file has VANISHED ("+!endFileProperties.exists()+"), is LOCKED ("+endFileProperties.isLocked()+"), or has CHANGED ("+fileHasChanged+")");
				
				resetFileEnd();
				return;
			}			
			
			// If it's still there, add it to the database
			addFileVersion(endFileProperties);						
			
			// Reset
			resetFileEnd();		
		}
		
		private void addFileVersion(FileProperties fileProperties) {
			if (fileProperties.getChecksum() != null) {
				logger.log(Level.FINER, "- /File: {0} (checksum {1})", new Object[] {  fileProperties.getRelativePath(), fileProperties.getChecksum() });
			}
			else {
				logger.log(Level.FINER, "- /File: {0} (directory/symlink/0-byte-file)", fileProperties.getRelativePath());
			}
			
			// 1. Determine if file already exists in database 
			PartialFileHistory lastFileHistory = guessLastFileHistory(fileProperties);						
			FileVersion lastFileVersion = (lastFileHistory != null) ? lastFileHistory.getLastVersion() : null;
			
			// 2. Add new file version
			PartialFileHistory fileHistory = null;
			FileVersion fileVersion = null;			
			
			if (lastFileVersion == null) {				
				// TODO [low] move this generation to a better place. Where?
				FileHistoryId newFileHistoryId = generateNewFileHistoryId();
				
				fileHistory = new PartialFileHistory(newFileHistoryId);
				
				fileVersion = new FileVersion();
				fileVersion.setVersion(1L);
				fileVersion.setStatus(FileStatus.NEW);
			} 
			else {
				fileHistory = new PartialFileHistory(lastFileHistory.getFileId());
				
				fileVersion = lastFileVersion.clone();
				fileVersion.setVersion(lastFileVersion.getVersion()+1);	
			}			

			fileVersion.setPath(fileProperties.getRelativePath());
			fileVersion.setLinkTarget(fileProperties.getLinkTarget());
			fileVersion.setType(fileProperties.getType());
			fileVersion.setSize(fileProperties.getSize());
			fileVersion.setChecksum(fileProperties.getChecksum());
			fileVersion.setLastModified(new Date(fileProperties.getLastModified()));
			fileVersion.setUpdated(new Date());
			fileVersion.setCreatedBy(config.getMachineName());
			
			if (FileUtil.isWindows()) {
				fileVersion.setDosAttributes(fileProperties.getDosAttributes());
			}
			else if (FileUtil.isUnixLikeOperatingSystem()) {
				fileVersion.setPosixPermissions(fileProperties.getPosixPermissions());
			}

			// Determine status
			if (lastFileVersion != null) {
				if (fileVersion.getType() == FileType.FILE && FileChecksum.fileChecksumEquals(fileVersion.getChecksum(), lastFileVersion.getChecksum())) {
					fileVersion.setStatus(FileStatus.CHANGED);
				}
				else if (!fileVersion.getPath().equals(lastFileVersion.getPath())) {
					fileVersion.setStatus(FileStatus.RENAMED);
				}
				else {
					fileVersion.setStatus(FileStatus.CHANGED); 
				}						
			}	
			
			// Compare new and last version 
			FileProperties lastFileVersionProperties = fileVersionComparator.captureFileProperties(lastFileVersion);
			FileVersionComparison lastToNewFileVersionComparison = fileVersionComparator.compare(fileProperties, lastFileVersionProperties, true);
			
			boolean newVersionDiffersFromToLastVersion = !lastToNewFileVersionComparison.equals();
			
			// Only add new version if it differs!			
			if (newVersionDiffersFromToLastVersion) {
				fileHistory.addFileVersion(fileVersion);
				newDatabaseVersion.addFileHistory(fileHistory);
				
				logger.log(Level.INFO, "   * Added file version:    "+fileVersion);
				logger.log(Level.INFO, "     based on file version: "+lastFileVersion);
			}
			else {
				logger.log(Level.INFO, "   * NOT ADDING file version: "+fileVersion);
				logger.log(Level.INFO, "         b/c IDENTICAL prev.: "+lastFileVersion);
			}
			
			// 3. Add file content (if not a directory)			
			if (fileProperties.getChecksum() != null && fileContent != null) {
				fileContent.setSize(fileProperties.getSize());
				fileContent.setChecksum(fileProperties.getChecksum());

				// Check if content already exists, throw gathered content away if it does!
				FileContent existingContent = database.getContent(fileProperties.getChecksum());
				
				if (existingContent == null) { 
					newDatabaseVersion.addFileContent(fileContent);
				}
			}						
		}	

		private FileHistoryId generateNewFileHistoryId() {
			int newFileHistoryIdAttempts = 0;
			FileHistoryId newFileHistoryId = null;
			
			// TODO[low]: this is probably useless as the collision probability should be super tiny
			do {
				newFileHistoryId = FileHistoryId.secureRandomFileId();
				
				if (database.getFileHistory(newFileHistoryId) == null 
					&& newDatabaseVersion.getFileHistory(newFileHistoryId) == null) {
					
					break;
				}
				
				newFileHistoryIdAttempts++;
			} while (newFileHistoryIdAttempts < 10);
			
			if (newFileHistoryIdAttempts >= 10) {
				throw new IndexerException("Cannot generate a unique file id, aborting");
			}
			
			return newFileHistoryId;
		}

		private void resetFileEnd() {
			fileContent = null;	
			startFileProperties = null;
			endFileProperties = null;
		}

		private PartialFileHistory guessLastFileHistory(FileProperties fileProperties) {
			if (fileProperties.getType() == FileType.FILE) {
				return guessLastFileHistoryForFile(fileProperties);
			} 
			else if (fileProperties.getType() == FileType.SYMLINK) {
				return guessLastFileHistoryForSymlink(fileProperties);
			} 
			else if (fileProperties.getType() == FileType.FOLDER) {
				return guessLastFileHistoryForFolder(fileProperties);
			}
			else {
				throw new RuntimeException("This should not happen.");
			}
		}
		
		private PartialFileHistory guessLastFileHistoryForSymlink(FileProperties fileProperties) {
			return guessLastFileHistoryForFolderOrSymlink(fileProperties);
		}
		
		private PartialFileHistory guessLastFileHistoryForFolder(FileProperties fileProperties) {
			return guessLastFileHistoryForFolderOrSymlink(fileProperties);
		}

		private PartialFileHistory guessLastFileHistoryForFolderOrSymlink(FileProperties fileProperties) {
			PartialFileHistory lastFileHistory = database.getFileHistory(fileProperties.getRelativePath());

			if (lastFileHistory == null) {
				logger.log(Level.FINER, "   * No old file history found, starting new history (path: "+fileProperties.getRelativePath()+", "+fileProperties.getType()+")");
				return null;
			}
			else {
				FileVersion lastFileVersion = lastFileHistory.getLastVersion();
				
				if (lastFileVersion.getStatus() != FileStatus.DELETED && lastFileVersion.getType() == fileProperties.getType()) {
					logger.log(Level.FINER, "   * Found old file history "+lastFileHistory.getFileId()+" (by path: "+fileProperties.getRelativePath()+"), "+fileProperties.getType()+", appending new version.");
					return lastFileHistory;
				}
				else {
					logger.log(Level.FINER, "   * No old file history found, starting new history (path: "+fileProperties.getRelativePath()+", "+fileProperties.getType()+")");
					return null;
				}
			}
		}
		
		private PartialFileHistory guessLastFileHistoryForFile(FileProperties fileProperties) {
			PartialFileHistory lastFileHistory = null;
			
			// 1a. by path
			lastFileHistory = database.getFileHistory(fileProperties.getRelativePath());

			if (lastFileHistory == null) {
				// 1b. by checksum
				if (fileProperties.getChecksum() != null) {
					Collection<PartialFileHistory> fileHistoriesWithSameChecksum = database.getFileHistories(fileProperties.getChecksum());
					
					if (fileHistoriesWithSameChecksum != null) {
						// check if they do not exist anymore --> assume it has moved!
						// TODO [low] choose a more appropriate file history, this takes the first best version with the same checksum
						for (PartialFileHistory fileHistoryWithSameChecksum : fileHistoriesWithSameChecksum) {
							FileVersion lastVersion = fileHistoryWithSameChecksum.getLastVersion();
							
							if (fileProperties.getLastModified() != lastVersion.getLastModified().getTime() || fileProperties.getSize() != lastVersion.getSize()) {
								continue;
							}
							
							File lastVersionOnLocalDisk = new File(config.getLocalDir()+File.separator+lastVersion.getPath());
							
							if (lastVersion.getStatus() != FileStatus.DELETED && !FileUtil.exists(lastVersionOnLocalDisk)) {
								lastFileHistory = fileHistoryWithSameChecksum;
								break;
							}
						}
					}
				}
				
				if (lastFileHistory == null) {
					logger.log(Level.FINER, "   * No old file history found, starting new history (path: "+fileProperties.getRelativePath()+", checksum: "+fileProperties.getChecksum()+")");
					return null;
				}
				else {
					logger.log(Level.FINER, "   * Found old file history "+lastFileHistory.getFileId()+" (by checksum: "+fileProperties.getChecksum()+"), appending new version.");
					return lastFileHistory;
				}
			}
			else {
				if (fileProperties.getType() != lastFileHistory.getLastVersion().getType()) {
					logger.log(Level.FINER, "   * No old file history found, starting new history (path: "+fileProperties.getRelativePath()+", checksum: "+fileProperties.getChecksum()+")");
					return null;
				}
				else {
					logger.log(Level.FINER, "   * Found old file history "+lastFileHistory.getFileId()+" (by path: "+fileProperties.getRelativePath()+"), appending new version.");
					return lastFileHistory;
				}
			}			
		}
		
		@Override
		public void onMultiChunkOpen(MultiChunk multiChunk) {
			logger.log(Level.FINER, "- +MultiChunk {0}", StringUtil.toHex(multiChunk.getId()));
			multiChunkEntry = new MultiChunkEntry(new MultiChunkId(multiChunk.getId()));
		}

		@Override
		public void onMultiChunkWrite(MultiChunk multiChunk, Chunk chunk) {
			logger.log(Level.FINER, "- Chunk > MultiChunk: {0} > {1}", new Object[] { StringUtil.toHex(chunk.getChecksum()), StringUtil.toHex(multiChunk.getId()) });		
			multiChunkEntry.addChunk(chunkEntry.getChecksum());				
		}
		
		@Override
		public void onMultiChunkClose(MultiChunk multiChunk) {
			logger.log(Level.FINER, "- /MultiChunk {0}", StringUtil.toHex(multiChunk.getId()));
			
			newDatabaseVersion.addMultiChunk(multiChunkEntry);
			multiChunkEntry = null;
		}

		@Override
		public File getMultiChunkFile(byte[] multiChunkId) {
			return config.getCache().getEncryptedMultiChunkFile(multiChunkId);
		}
		
		@Override
		public byte[] createNewMultiChunkId(Chunk firstChunk) {
			byte[] newMultiChunkId = new byte[firstChunk.getChecksum().length];
			secureRandom.nextBytes(newMultiChunkId);
			
			return newMultiChunkId;
		}

		@Override
		public void onFileAddChunk(File file, Chunk chunk) {			
			logger.log(Level.FINER, "- Chunk > FileContent: {0} > {1}", new Object[] { StringUtil.toHex(chunk.getChecksum()), file });
			fileContent.addChunk(new ChunkChecksum(chunk.getChecksum()));				
		}		

		/*
		 * Checks if chunk already exists in all database versions (db)
		 * Afterwards checks if chunk exists in new introduced databaseversion. 
		 * (non-Javadoc)
		 * @see org.syncany.chunk.DeduperListener#onChunk(org.syncany.chunk.Chunk)
		 */
		@Override
		public boolean onChunk(Chunk chunk) {
			ChunkChecksum chunkChecksum = new ChunkChecksum(chunk.getChecksum());
			chunkEntry = database.getChunk(chunkChecksum);

			if (chunkEntry == null) {
				chunkEntry = newDatabaseVersion.getChunk(chunkChecksum);
				
				if (chunkEntry == null) {
					logger.log(Level.FINER, "- Chunk new: {0}", chunkChecksum.toString());
					
					chunkEntry = new ChunkEntry(chunkChecksum, chunk.getSize());
					newDatabaseVersion.addChunk(chunkEntry);
					
					return true;	
				}
			}
			
			logger.log(Level.FINER, "- Chunk exists: {0}", StringUtil.toHex(chunk.getChecksum()));
			return false;
		}
	}		
}
