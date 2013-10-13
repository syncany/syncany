package org.syncany.operations;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
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
import org.syncany.database.ChunkEntry.ChunkEntryId;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.FileVersionHelper;
import org.syncany.database.FileVersionHelper.FileProperties;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

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
			// Ignore this file history if it has been update in this database version before (file probably renamed!)
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
			
			// If file has VANISHED, mark as DELETED 
			if (!lastLocalVersionOnDisk.exists()) {
				PartialFileHistory deletedFileHistory = new PartialFileHistory(fileHistory.getFileId());
				FileVersion deletedVersion = (FileVersion) lastLocalVersion.clone();
				deletedVersion.setStatus(FileStatus.DELETED);
				deletedVersion.setVersion(fileHistory.getLastVersion().getVersion()+1);
				
				logger.log(Level.FINER, "  + Deleted: {0}, adding deleted version {1}", new Object[] { lastLocalVersion, deletedVersion });
				newDatabaseVersion.addFileHistory(deletedFileHistory);
				newDatabaseVersion.addFileVersionToHistory(fileHistory.getFileId(), deletedVersion);
			}
		}				
	}

	private class IndexerDeduperListener implements DeduperListener {
		private FileVersionHelper fileVersionHelper;
		private SecureRandom secureRandom;
		private DatabaseVersion newDatabaseVersion;
		private ChunkEntry chunkEntry;		
		private MultiChunkEntry multiChunkEntry;	
		private FileContent fileContent;
		
		private FileProperties startFileProperties;
		private FileProperties endFileProperties;		
		
		public IndexerDeduperListener(DatabaseVersion newDatabaseVersion) {
			this.fileVersionHelper = new FileVersionHelper(config);
			this.secureRandom = new SecureRandom();
			this.newDatabaseVersion = newDatabaseVersion;
		}				

		@Override
		public boolean onFileStart(File file) {
			logger.log(Level.FINER, "- +File {0}", file); 
			
			startFileProperties = fileVersionHelper.captureFileProperties(file, null, false);
			
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
		public boolean onFileStartDeduplicate(File file) {			
			return startFileProperties.getType() == FileType.FILE; // Ignore directories and symlinks!
		}

		@Override
		public void onFileEnd(File file, byte[] checksum) {
			// Get file attributes (get them while file exists)
			// Note: Do NOT move any File-methods (file.anything()) below the file.exists()-part, 
			//       because the file could vanish! 
			endFileProperties = fileVersionHelper.captureFileProperties(file, checksum, false);
			
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
				logger.log(Level.FINER, "- /File: {0} (checksum {1})", new Object[] {  fileProperties.getRelativePath(), StringUtil.toHex(fileProperties.getChecksum()) });
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
				fileHistory = new PartialFileHistory();
				
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
			
			// Determine status
			if (lastFileVersion != null) {
				if (fileVersion.getType() == FileType.FILE && !Arrays.equals(fileVersion.getChecksum(), lastFileVersion.getChecksum())) {
					fileVersion.setStatus(FileStatus.CHANGED);
				}
				else if (!fileVersion.getPath().equals(lastFileVersion.getPath())) {
					fileVersion.setStatus(FileStatus.RENAMED);
				}
				else {
					fileVersion.setStatus(FileStatus.UNKNOWN); // TODO [low] Add more states, ATTRS_CHANGED, or so.
				}						
			}	
			
			// Overwrite permissions/attributes only on the system we are on
			boolean hasIdenticalPermsAndAttributes = true;
			
			if (FileUtil.isWindows()) {
				fileVersion.setDosAttributes(fileProperties.getDosAttributes());
				
				hasIdenticalPermsAndAttributes = 
					   lastFileVersion != null
					&& fileVersion.getPosixPermissions() != null
					&& fileVersion.getPosixPermissions().equals(lastFileVersion.getPosixPermissions());
			}
			else if (FileUtil.isUnixLikeOperatingSystem()) {
				fileVersion.setPosixPermissions(fileProperties.getPosixPermissions());
				
				hasIdenticalPermsAndAttributes = 
					   lastFileVersion != null
					&& fileVersion.getDosAttributes() != null
					&& fileVersion.getDosAttributes().equals(lastFileVersion.getDosAttributes());							
			}			
			
			// Check identical link target
			boolean hasIdenticalLinkTarget = true;
			
			if (lastFileVersion != null && lastFileVersion.getType() == FileType.SYMLINK
				&& fileVersion.getType() == FileType.SYMLINK) {
				
				hasIdenticalLinkTarget = fileVersion.getLinkTarget().equals(lastFileVersion.getLinkTarget());
			}
											
			// Only add if not identical
			boolean isIdenticalToLastVersion = 
				   lastFileVersion != null 
				&& lastFileVersion.getPath().equals(fileVersion.getPath())
				&& lastFileVersion.getType().equals(fileVersion.getType())
				&& lastFileVersion.getSize().equals(fileVersion.getSize())
				&& Arrays.equals(lastFileVersion.getChecksum(), fileVersion.getChecksum())
				&& lastFileVersion.getLastModified().equals(fileVersion.getLastModified())
				&& hasIdenticalPermsAndAttributes
				&& hasIdenticalLinkTarget;
			
			if (!isIdenticalToLastVersion) {
				newDatabaseVersion.addFileHistory(fileHistory);
				newDatabaseVersion.addFileVersionToHistory(fileHistory.getFileId(), fileVersion);
				
				logger.log(Level.INFO, "   * Added file version:    "+fileVersion);
				logger.log(Level.INFO, "     based on file version: "+lastFileVersion);
			}
			else {
				logger.log(Level.INFO, "   * NOT ADDING file version (identical to previous!): "+fileVersion+" IDENTICAL TO "+lastFileVersion);
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
				logger.log(Level.FINER, "   * No old file history found, starting new history (path: "+fileProperties.getRelativePath()+", FOLDER)");
			}
			else {
				FileVersion lastFileVersion = lastFileHistory.getLastVersion();
				
				if (lastFileVersion.getStatus() != FileStatus.DELETED) {
					logger.log(Level.FINER, "   * Found old file history "+lastFileHistory.getFileId()+" (by path: "+fileProperties.getRelativePath()+"), appending new version.");					
				}
				else {
					logger.log(Level.FINER, "   * No old file history found, starting new history (path: "+fileProperties.getRelativePath()+", FOLDER)");
				}
			}
			
			return lastFileHistory;
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
							
							if (lastVersion.getStatus() != FileStatus.DELETED && !lastVersionOnLocalDisk.exists()) {
								lastFileHistory = fileHistoryWithSameChecksum;
								break;
							}
						}
					}
				}
				
				if (lastFileHistory == null) {
					logger.log(Level.FINER, "   * No old file history found, starting new history (path: "+fileProperties.getRelativePath()+", checksum: "+StringUtil.toHex(fileProperties.getChecksum())+")");
				}
				else {
					logger.log(Level.FINER, "   * Found old file history "+lastFileHistory.getFileId()+" (by checksum: "+StringUtil.toHex(fileProperties.getChecksum())+"), appending new version.");
				}
			}
			else {
				logger.log(Level.FINER, "   * Found old file history "+lastFileHistory.getFileId()+" (by path: "+fileProperties.getRelativePath()+"), appending new version.");
			}
			
			return lastFileHistory;
		}
		
		@Override
		public void onOpenMultiChunk(MultiChunk multiChunk) {
			logger.log(Level.FINER, "- +MultiChunk {0}", StringUtil.toHex(multiChunk.getId()));
			multiChunkEntry = new MultiChunkEntry(multiChunk.getId());
		}

		@Override
		public void onWriteMultiChunk(MultiChunk multiChunk, Chunk chunk) {
			logger.log(Level.FINER, "- Chunk > MultiChunk: {0} > {1}", new Object[] { StringUtil.toHex(chunk.getChecksum()), StringUtil.toHex(multiChunk.getId()) });		
			multiChunkEntry.addChunk(new ChunkEntryId(chunkEntry.getChecksum()));				
		}
		
		@Override
		public void onCloseMultiChunk(MultiChunk multiChunk) {
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
			fileContent.addChunk(new ChunkEntryId(chunk.getChecksum()));				
		}		

		/*
		 * Checks if chunk already exists in all database versions (db)
		 * Afterwards checks if chunk exists in new introduced databaseversion. 
		 * (non-Javadoc)
		 * @see org.syncany.chunk.DeduperListener#onChunk(org.syncany.chunk.Chunk)
		 */
		@Override
		public boolean onChunk(Chunk chunk) {
			chunkEntry = database.getChunk(chunk.getChecksum());

			if (chunkEntry == null) {
				chunkEntry = newDatabaseVersion.getChunk(chunk.getChecksum());
				
				if (chunkEntry == null) {
					logger.log(Level.FINER, "- Chunk new: {0}", StringUtil.toHex(chunk.getChecksum()));
					
					chunkEntry = new ChunkEntry(chunk.getChecksum(), chunk.getSize());
					newDatabaseVersion.addChunk(chunkEntry);
					
					return true;	
				}
			}
			
			logger.log(Level.FINER, "- Chunk exists: {0}", StringUtil.toHex(chunk.getChecksum()));
			return false;
		}
	}		
}
