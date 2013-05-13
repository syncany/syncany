package org.syncany.operations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.chunk.Chunk;
import org.syncany.chunk.Chunker;
import org.syncany.chunk.Deduper;
import org.syncany.chunk.DeduperListener;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.Transformer;
import org.syncany.config.Constants;
import org.syncany.config.Profile;
import org.syncany.connection.Uploader;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.db.ChunkEntry;
import org.syncany.db.Database;
import org.syncany.db.DatabaseVersion;
import org.syncany.db.FileContent;
import org.syncany.db.FileHistoryPart;
import org.syncany.db.FileVersion;
import org.syncany.db.MultiChunkEntry;
import org.syncany.util.FileLister;
import org.syncany.util.FileLister.FileListerAdapter;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class SyncUpOperation extends Operation {
	private static final Logger logger = Logger.getLogger(SyncUpOperation.class.getSimpleName());
	
	private Uploader uploader;
	
	public SyncUpOperation(Profile profile) {
		super(profile);
		this.uploader = new Uploader(profile.getConnection());
	}	
	
	public void execute() throws Exception {
		logger.log(Level.INFO, "Sync up ...");
		
		File localDatabaseFile = new File(profile.getAppDatabaseDir()+"/local.db");		
		Database db = loadLocalDatabase(localDatabaseFile);
		
		List<File> localFiles = listFiles(profile.getLocalDir());
		long newestLocalDatabaseVersion = index(localFiles, profile.getChunker(), profile.getMultiChunker(), profile.getTransformer(),
				db, profile.getLocalDir(), profile.getAppCacheDir());
		
		long fromLocalDatabaseVersion = (newestLocalDatabaseVersion-1 >= 1) ? newestLocalDatabaseVersion : 1;		
		saveLocalDatabase(db, fromLocalDatabaseVersion, newestLocalDatabaseVersion, localDatabaseFile);
		
		boolean uploadMultiChunksSuccess = uploadMultiChunks(db.getLastDatabaseVersion().getMultiChunks());
		
		if (uploadMultiChunksSuccess) {
			boolean uploadLocalDatabaseSuccess = uploadLocalDatabase(localDatabaseFile, newestLocalDatabaseVersion);			
		}
		else {
			throw new Exception("aa");
		}		
	}	
	
	private boolean uploadMultiChunks(Collection<MultiChunkEntry> multiChunksEntries) throws InterruptedException {
		for (MultiChunkEntry multiChunkEntry : multiChunksEntries) {
			File multiChunkFile = profile.getCache().getEncryptedMultiChunkFile(multiChunkEntry.getId()); // FIXME id != checksum
			uploader.queue(multiChunkFile);
		}
		
		uploader.start();
		Thread.sleep(5000);
		return true; // FIXME
	}

	private boolean uploadLocalDatabase(File localDatabaseFile, long newestLocalDatabaseVersion) throws InterruptedException {
		RemoteFile remoteDatabaseFile = new RemoteFile("db-"+profile.getMachineName()+"-"+newestLocalDatabaseVersion);
		uploader.queue(localDatabaseFile, remoteDatabaseFile);
		return true;
	}

	//FIXME
	private long index(List<File> localFiles, Chunker chunker, MultiChunker multiChunker, Transformer transformer, 
			final Database db, final File localDir, final File appCacheDir) throws FileNotFoundException, IOException {
		
		final Deduper deduper = new Deduper(chunker, multiChunker, transformer);
		final DatabaseVersion dbv = new DatabaseVersion();
		
		deduper.deduplicate(localFiles, new DeduperListener() {
			private FileHistoryPart fileHistory;
			private FileVersion fileVersion;
			private ChunkEntry chunkEntry;		
			private MultiChunkEntry multiChunkEntry;	
			private FileContent content;
			
			/*
			 * Checks if chunk already exists in all database versions (db)
			 * Afterwards checks if chunk exists in new introduced databaseversion. 
			 * (non-Javadoc)
			 * @see org.syncany.chunk.DeduperListener#onChunk(org.syncany.chunk.Chunk)
			 */
			@Override
			public boolean onChunk(Chunk chunk) {
				System.out.println("CHUNK       "+chunk);
				chunkEntry = db.getChunk(chunk.getChecksum());

				if (chunkEntry == null) {
					chunkEntry = dbv.getChunk(chunk.getChecksum());
					
					if (chunkEntry == null) {
						chunkEntry = new ChunkEntry(chunk.getChecksum(), chunk.getSize());
						dbv.addChunk(chunkEntry);
						
						return true;	
					}
				}
				
				return false;
			}
			
			@Override
			public void onFileStart(File file) {
				System.out.println("FILE OPEN   "+file);
				// Check if file exists in full database stock, or create new
				// onFileStart is only called once for each file,
				// thereby new dbv is not aware of incoming file   
				fileHistory = db.getFileHistory(
						FileUtil.getRelativePath(localDir,
							file) + Constants.DATABASE_FILE_SEPARATOR + file.getName());
	
				if (fileHistory == null) {
					fileHistory = new FileHistoryPart();
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
				
				newFileVersion.setPath(FileUtil.getRelativePath(localDir, file.getParentFile()));
				newFileVersion.setName(file.getName());
				
				dbv.addFileHistory(fileHistory);
				dbv.addFileVersionToHistory(fileHistory.getFileId(),newFileVersion);
				
				// Required for other events
				fileVersion = newFileVersion;
			}


			@Override
			public void onOpenMultiChunk(MultiChunk multiChunk) {
				System.out.println("MULTI OPEN  "+multiChunk);
				multiChunkEntry = new MultiChunkEntry(chunkEntry.getChecksum());
			}

			@Override
			public void onCloseMultiChunk(MultiChunk multiChunk) {
				System.out.println("MULTI CLOSE  ");
				dbv.addMultiChunk(multiChunkEntry);
				multiChunkEntry = null;
			}

			@Override
			public File getMultiChunkFile(byte[] multiChunkId) {
					return profile.getCache().getEncryptedMultiChunkFile(multiChunkId);
			}

			@Override
			public void onWriteMultiChunk(MultiChunk multiChunk, Chunk chunk) {
				System.out.println("WRITE CHUNK TO MULTI "+chunk);			
				multiChunkEntry.addChunk(chunkEntry);				
			}

			@Override
			public void onFileAddChunk(File file, Chunk chunk) {
				System.out.println("ADD CHUNK TO CONTENT "+chunk);			
				if (content == null) {
					content = new FileContent();
				}
				
				content.addChunk(chunkEntry);				
			}

			@Override
			public void onFileEnd(File file, byte[] checksum) {
				if (checksum != null) {
					System.out.println("FILE END "+StringUtil.toHex(checksum));
				}
				else {
					System.out.println("FILE END ");
				}
				
				
				if (content != null) {
					content.setChecksum(checksum);

					fileVersion.setContent(content);
					dbv.addFileContent(content);
				}
				
				content = null;		
				

				// fileHistory.addVersion(fileVersion);
				dbv.addFileHistory(fileHistory);
				
			}

			@Override
			public void onFinish() {
				db.addDatabaseVersion(dbv);
			}

			@Override
			public void onStart() {
				// Go fish.
			}
		});
				
		return db.getLastLocalDatabaseVersion();
	}

	// FIXME this should not be here
	private List<File> listFiles(File localDir) {
		final List<File> files = new ArrayList<File>();
		
		new FileLister(localDir, new FileListerAdapter() {
			@Override public void proceedFile(File f) { files.add(f); }			
		}).start();
		
		return files;
	}
}
