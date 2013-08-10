package org.syncany.operations;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.chunk.Chunk;
import org.syncany.chunk.Deduper;
import org.syncany.chunk.DeduperListener;
import org.syncany.chunk.MultiChunk;
import org.syncany.config.Constants;
import org.syncany.config.Config;
import org.syncany.database.ChunkEntry;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class Indexer {
	private static final Logger logger = Logger.getLogger(Indexer.class.getSimpleName());
	
	private Config profile;
	private Deduper deduper;
	private Database database;
	
	public Indexer(Config profile, Deduper deduper, Database database) {
		this.profile = profile;
		this.deduper = deduper;
		this.database = database;
	}
	
	public DatabaseVersion index(List<File> files) throws IOException {
		DatabaseVersion newDatabaseVersion = new DatabaseVersion();		
		deduper.deduplicate(files, new IndexerDeduperListener(newDatabaseVersion));			
		
		return newDatabaseVersion;
	}

	private class IndexerDeduperListener implements DeduperListener {
		private DatabaseVersion newDatabaseVersion;
		private PartialFileHistory fileHistory;
		private FileVersion fileVersion;
		private ChunkEntry chunkEntry;		
		private MultiChunkEntry multiChunkEntry;	
		private FileContent content;
		
		public IndexerDeduperListener(DatabaseVersion newDatabaseVersion) {
			this.newDatabaseVersion = newDatabaseVersion;
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
		
		@Override
		public void onFileStart(File file) {
			logger.log(Level.FINER, "- +File {0}", file); 
			
			// Check if file exists in full database stock, or create new onFileStart is only called once for each file,
			// thereby new dbv is not aware of incoming file   
			String relativeFilePath = FileUtil.getRelativePath(profile.getLocalDir(), file) + Constants.DATABASE_FILE_SEPARATOR + file.getName(); 
			fileHistory = database.getFileHistory(relativeFilePath);

			if (fileHistory == null) {
				fileHistory = new PartialFileHistory();
			}

			// Check for versions
			fileVersion = fileHistory.getLastVersion();
			FileVersion newFileVersion;
			
			if (fileVersion == null) {
				newFileVersion = new FileVersion();
				newFileVersion.setVersion(1L);
			} 
			else {
				newFileVersion = (FileVersion) fileVersion.clone();
				fileVersion.setVersion(fileVersion.getVersion()+1);	
			}
			
			newFileVersion.setPath(FileUtil.getRelativePath(profile.getLocalDir(), file.getParentFile()));
			newFileVersion.setName(file.getName());
			
			newDatabaseVersion.addFileHistory(fileHistory);
			newDatabaseVersion.addFileVersionToHistory(fileHistory.getFileId(),newFileVersion);
			
			// Required for other events
			fileVersion = newFileVersion;
		}


		@Override
		public void onOpenMultiChunk(MultiChunk multiChunk) {
			logger.log(Level.FINER, "- +MultiChunk {0}", StringUtil.toHex(multiChunk.getId()));
			multiChunkEntry = new MultiChunkEntry(chunkEntry.getChecksum());
		}

		@Override
		public void onCloseMultiChunk(MultiChunk multiChunk) {
			logger.log(Level.FINER, "- /MultiChunk {0}", StringUtil.toHex(multiChunk.getId()));
			
			newDatabaseVersion.addMultiChunk(multiChunkEntry);
			multiChunkEntry = null;
		}

		@Override
		public File getMultiChunkFile(byte[] multiChunkId) {
				return profile.getCache().getEncryptedMultiChunkFile(multiChunkId);
		}

		@Override
		public void onWriteMultiChunk(MultiChunk multiChunk, Chunk chunk) {
			logger.log(Level.FINER, "- Chunk > MultiChunk: {0} > {1}", new Object[] { StringUtil.toHex(chunk.getChecksum()), StringUtil.toHex(multiChunk.getId()) });		
			multiChunkEntry.addChunk(chunkEntry);				
		}

		@Override
		public void onFileAddChunk(File file, Chunk chunk) {
			if (content == null) {
				logger.log(Level.FINER, "- +FileContent: {0}", file);			
				content = new FileContent();
			}
			
			logger.log(Level.FINER, "- Chunk > FileContent: {0} > {1}", new Object[] { StringUtil.toHex(chunk.getChecksum()), file });
			content.addChunk(chunkEntry);				
		}

		@Override
		public void onFileEnd(File file, byte[] checksum) {
			if (checksum != null) {
				logger.log(Level.FINER, "- /File: {0} (checksum {1})", new Object[] { file, StringUtil.toHex(checksum) });
			}
			else {
				logger.log(Level.FINER, "- /File: {0} (directory)", file);
			}
			
			
			if (content != null) {
				content.setChecksum(checksum);

				fileVersion.setContent(content);
				newDatabaseVersion.addFileContent(content);
			}
			
			content = null;		
			

			// fileHistory.addVersion(fileVersion);
			newDatabaseVersion.addFileHistory(fileHistory);				
		}

		@Override
		public void onFinish() {
			// Go fish.
		}

		@Override
		public void onStart() {
			// Go fish.
		}		
	}	
}
