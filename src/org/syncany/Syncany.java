package org.syncany;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.syncany.chunk.Chunk;
import org.syncany.chunk.Chunker;
import org.syncany.chunk.Deduper;
import org.syncany.chunk.DeduperListener;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.Transformer;
import org.syncany.config.ConfigTO;
import org.syncany.config.Profile;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.experimental.db.ChunkEntry;
import org.syncany.experimental.db.Database;
import org.syncany.experimental.db.DatabaseDAO;
import org.syncany.experimental.db.DatabaseVersion;
import org.syncany.experimental.db.FileContent;
import org.syncany.experimental.db.FileHistoryPart;
import org.syncany.experimental.db.FileVersion;
import org.syncany.experimental.db.MultiChunkEntry;
import org.syncany.experimental.db.VectorClock;
import org.syncany.util.FileLister;
import org.syncany.util.FileLister.FileListerAdapter;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class Syncany {
	public static void main(String[] args) throws Exception {
		Syncany sy = new Syncany();

		// Parse arguments
		File configFile = new File("config.json");//System.getProperty("user.dir")+File.separator+"config.json");
		
		if (args.length > 0) {
			configFile = new File(args[0]);
		}
				
		// Read config
		Profile profile = new Profile(ConfigTO.load(configFile));
		sy.init(profile);
		
		// sy up
		sy.up(profile);
	}
	
	public Syncany() {
		// Read config
		// ...
	}
	
	public UpstreamStatus up(Profile profile) throws FileNotFoundException, IOException {
		File localDatabaseFile = new File(profile.getAppDatabaseDir()+"/local.db");		
		Database db = loadLocalRepoDB(localDatabaseFile);
		
		List<File> localFiles = listFiles(profile.getLocalDir());
		long newestLocalDatabaseVersion = index(localFiles, profile.getChunker(), profile.getMultiChunker(), profile.getTransformer(),
				db, profile.getLocalDir(), profile.getAppCacheDir());
		
		long fromLocalDatabaseVersion = (newestLocalDatabaseVersion-1 >= 1) ? newestLocalDatabaseVersion : 1;		
		saveLocalRepoDB(db, fromLocalDatabaseVersion, newestLocalDatabaseVersion, localDatabaseFile);
		
		
		
		UpstreamStatus statusCode = null;
		
		TransferManager tm = profile.getConnection().createTransferManager();
		/*
		if (uploadMultiChunks(updatedRepoDB.getNewestDatabaseVersion().getMetaMultiChunks(),cfg.getCacheDir(),tm)) {
			boolean status = uploadLocalRepoDB(updatedRepoDB);
			
			statusCode.setResponse(status ? "Job" : "Nope"); 
		} else
		{
			statusCode.setResponse("Error");
		}*/
		
		return statusCode;
	}	
	
	private boolean uploadMultiChunks(List<MultiChunkEntry> metaMultiChunks, String cacheDir, TransferManager tm) {
		
		/*
		for (MultiChunkEntry multiChunkEntry : metaMultiChunks) {
			
			tm.upload(localFile, new RemoteFile(localFile.getName());
		}*/
		//Find and upload given multi chunks by the power of grayskull
		return false;
	}

	private boolean uploadLocalRepoDB(Database updatedRepoDB) {
		//upload DB, either in one big file, or each version seperatly
		return true;
	}

	//FIXME
	private long index(List<File> localFiles, Chunker chunker, MultiChunker multiChunker, Transformer transformer, 
			final Database db, final File localDir, final File appCacheDir) throws FileNotFoundException, IOException {
		
		final Deduper deduper = new Deduper();
		final DatabaseVersion dbv = new DatabaseVersion();
		
		deduper.deduplicate(localFiles, chunker, multiChunker, transformer, new DeduperListener() {
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
						db.addChunk(chunkEntry);
						
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
				
				newFileVersion.setPath(FileUtil.getRelativePath(localDir,
						file.getParentFile()));
				newFileVersion.setName(file.getName());
				dbv.addFileHistory(fileHistory);
				dbv.addFileVersionToHistory(fileHistory.getFileId(),fileVersion);
			}


			@Override
			public void onOpenMultiChunk(MultiChunk multiChunk) {
				System.out.println("MULTI OPEN  "+multiChunk);
				multiChunkEntry = new MultiChunkEntry();
				multiChunkEntry.setChecksum(chunkEntry.getChecksum());
			}

			@Override
			public void onCloseMultiChunk(MultiChunk multiChunk) {
				System.out.println("MULTI CLOSE  ");
				dbv.addMultiChunk(multiChunkEntry);
				multiChunkEntry = null;
			}

			@Override
			public File getMultiChunkFile(byte[] multiChunkId) {
					return new File(appCacheDir 
									+ "/multichunk-"
									+ StringUtil.toHex(multiChunkId));
			}

			@Override
			public void onWriteMultiChunk(MultiChunk multiChunk, Chunk chunk) {
				System.out.println("WRITE CHUNK TO MULTI "+chunk);			
				multiChunkEntry.addChunk(chunkEntry);				
			}

			@Override
			public void onFileAddChunk(Chunk chunk) {
				System.out.println("ADD CHUNK TO CONTENT "+chunk);			
				if (content == null) {
					content = new FileContent();
				}
				
				content.addChunk(chunkEntry);				
			}

			@Override
			public void onFileEnd(byte[] checksum) {
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

	private List<File> listFiles(File localDir) {
		final List<File> files = new ArrayList<File>();
		
		new FileLister(localDir, new FileListerAdapter() {
			@Override public void proceedFile(File f) { files.add(f); }			
		}).start();
		
		return files;
	}

	private Database loadLocalRepoDB(File localDatabaseFile) throws IOException {
		DatabaseDAO dao = new DatabaseDAO();
		Database db = new Database();

		if (localDatabaseFile.exists() && localDatabaseFile.isFile() && localDatabaseFile.canRead()) {
			dao.load(db, localDatabaseFile);
		}
		
		return db;
	}
	
	private void saveLocalRepoDB(Database db, long fromVersion, long toVersion, File localDatabaseFile) throws IOException {
		DatabaseDAO dao = new DatabaseDAO();
		dao.save(db, fromVersion, toVersion, localDatabaseFile);
	}

	private void init(Profile profile) throws Exception {   
    	profile.getAppDir().mkdirs();
    	profile.getAppCacheDir().mkdirs();
    	profile.getAppDatabaseDir().mkdirs();
	}
	
}
