package org.syncany.operations;

import java.io.File;
import java.io.IOException;
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
import org.syncany.config.Constants;
import org.syncany.database.ChunkEntry;
import org.syncany.database.ChunkEntry.ChunkEntryId;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class Indexer {
	private static final Logger logger = Logger.getLogger(Indexer.class.getSimpleName());
	
	private Config config;
	private Deduper deduper;
	private Database database;
	
	public Indexer(Config config, Deduper deduper, Database database) {
		this.config = config;
		this.deduper = deduper;
		this.database = database;
	}
	
	public DatabaseVersion index(List<File> files) throws IOException {
		DatabaseVersion newDatabaseVersion = new DatabaseVersion();		
		
		// Find and index new files
		deduper.deduplicate(files, new IndexerDeduperListener(newDatabaseVersion));			
		
		// Find and remove deleted files
		removeDeletedFiles(newDatabaseVersion);
		
		return newDatabaseVersion;
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
			File lastLocalVersionOnDisk = new File(config.getLocalDir()+File.separator+lastLocalVersion.getFullName());
			
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
		private DatabaseVersion newDatabaseVersion;
		private ChunkEntry chunkEntry;		
		private MultiChunkEntry multiChunkEntry;	
		private FileContent fileContent;
		
		public IndexerDeduperListener(DatabaseVersion newDatabaseVersion) {
			this.newDatabaseVersion = newDatabaseVersion;
		}				

		@Override
		public void onStart() {
			// Go fish.
		}		

		@Override
		public void onFinish() {
			// Go fish.
		}
		
		@Override
		public void onFileStart(File file) {
			logger.log(Level.FINER, "- +File {0}", file); 
			
			// Content
			if (file.isFile()) {
				logger.log(Level.FINER, "- +FileContent: {0}", file);			
				fileContent = new FileContent();
				fileContent.setSize((int) file.length()); 
			}			
			
		}

		@Override
		public void onFileEnd(File file, byte[] checksum) {
			if (checksum != null) {
				logger.log(Level.FINER, "- /File: {0} (checksum {1})", new Object[] { file, StringUtil.toHex(checksum) });
			}
			else {
				logger.log(Level.FINER, "- /File: {0} (directory)", file);
			}
			
			// 1. Determine if file already exists in database 
			PartialFileHistory lastFileHistory = guessLastFileHistory(file, checksum);						
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
				
				fileVersion = (FileVersion) lastFileVersion.clone();
				fileVersion.setVersion(lastFileVersion.getVersion()+1);									
			}			

			fileVersion.setPath(FileUtil.getRelativePath(config.getLocalDir(), file.getParentFile()));
			fileVersion.setName(file.getName());
			fileVersion.setChecksum(checksum);
			fileVersion.setLastModified(new Date(file.lastModified()));
			fileVersion.setUpdated(new Date());
			fileVersion.setCreatedBy(config.getMachineName());
			
			// Determine status
			if (lastFileVersion != null) {
				if (!fileVersion.isFolder() && !Arrays.equals(fileVersion.getChecksum(), lastFileVersion.getChecksum())) {
					fileVersion.setStatus(FileStatus.CHANGED);
				}
				else if (!fileVersion.getFullName().equals(lastFileVersion.getFullName())) {
					fileVersion.setStatus(FileStatus.RENAMED);
				}
				else {
					fileVersion.setStatus(FileStatus.UNKNOWN);
				}						
			}
			
			// Only add if not identical
			boolean isIdenticalToLastVersion = lastFileVersion != null && Arrays.equals(lastFileVersion.getChecksum(), fileVersion.getChecksum())
					&& lastFileVersion.getName().equals(fileVersion.getName()) && lastFileVersion.getPath().equals(fileVersion.getPath());
			
			if (!isIdenticalToLastVersion) {
				newDatabaseVersion.addFileHistory(fileHistory);
				newDatabaseVersion.addFileVersionToHistory(fileHistory.getFileId(), fileVersion);
			}
			else {
				logger.log(Level.INFO, "   * NOT ADDING file version "+fileVersion+", identical to existing previous version "+lastFileVersion);
			}
			
			// 3. Add file content (if not a directory)			
			if (fileContent != null) {
				fileContent.setChecksum(checksum);

				// Check if content already exists, throw gathered content away if it does!
				FileContent existingContent = database.getContent(checksum);
				
				if (existingContent == null) { 
					newDatabaseVersion.addFileContent(fileContent);
				}
			}			
			
			// Reset
			fileContent = null;					
		}

		private PartialFileHistory guessLastFileHistory(File file, byte[] checksum) {
			PartialFileHistory lastFileHistory = null;
			
			// 1a. by file name
			if (checksum != null) {
				String relativeFilePath = FileUtil.getRelativeParentDirectory(config.getLocalDir(), file) + Constants.DATABASE_FILE_SEPARATOR + file.getName();
				lastFileHistory = database.getFileHistory(relativeFilePath);
	
				if (lastFileHistory == null) {
					// 1b. by checksum
					Collection<PartialFileHistory> fileHistoriesWithSameChecksum = database.getFileHistories(checksum);
					
					if (fileHistoriesWithSameChecksum != null) {
						// check if they do not exist anymore --> assume it has moved!
						// TODO [low] choose a more appropriate file history, this takes the first best version with the same checksum
						for (PartialFileHistory fileHistoryWithSameChecksum : fileHistoriesWithSameChecksum) {
							File lastVersionOnLocalDisk = new File(config.getLocalDir()+File.separator+fileHistoryWithSameChecksum.getLastVersion().getFullName());
							
							if (!lastVersionOnLocalDisk.exists()) {
								lastFileHistory = fileHistoryWithSameChecksum;
								break;
							}
						}
					}
					
					if (lastFileHistory == null) {
						logger.log(Level.FINER, "   * No old file history found, starting new history (path: "+relativeFilePath+", checksum: "+StringUtil.toHex(checksum)+")");
					}
					else {
						logger.log(Level.FINER, "   * Found old file history "+lastFileHistory.getFileId()+" (by checksum: "+StringUtil.toHex(checksum)+"), appending new version.");
					}
				}
				else {
					logger.log(Level.FINER, "   * Found old file history "+lastFileHistory.getFileId()+" (by path: "+relativeFilePath+"), appending new version.");
				}
			}
			
			return lastFileHistory;
		}
		
		@Override
		public void onOpenMultiChunk(MultiChunk multiChunk) {
			logger.log(Level.FINER, "- +MultiChunk {0}", StringUtil.toHex(multiChunk.getId()));
			multiChunkEntry = new MultiChunkEntry(chunkEntry.getChecksum());
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
