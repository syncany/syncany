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
import org.syncany.experimental.db.DatabaseDAO;
import org.syncany.experimental.db.Database;
import org.syncany.experimental.db.FileContent;
import org.syncany.experimental.db.FileHistory;
import org.syncany.experimental.db.FileVersion;
import org.syncany.experimental.db.MultiChunkEntry;
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
		Database db = loadLocalRepoDB(profile.getAppDir());
		List<File> localFiles = listFiles(profile.getLocalDir());
		long newestLocalDatabaseVersion = index(localFiles, profile.getChunker(), profile.getMultiChunker(), profile.getTransformer(),
				db, profile.getAppDatabaseDir(), profile.getAppCacheDir());
		
		saveLocalRepoDB(db, 1, newestLocalDatabaseVersion, profile.getAppDatabaseDir());
		
		
		
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
			final Database db, final File appDatabaseDir, final File appCacheDir) throws FileNotFoundException, IOException {
		
		final Deduper indexer = new Deduper();
		
		indexer.deduplicate(localFiles, chunker, multiChunker, transformer, new DeduperListener() {
			private FileHistory fileHistory;
			private FileVersion fileVersion;
			private ChunkEntry chunkEntry;		
			private MultiChunkEntry multiChunkEntry;	
			private FileContent content;
		
			@Override
			public boolean onChunk(Chunk chunk) {
				System.out.println("CHUNK       "+chunk);
				chunkEntry = db.getChunk(chunk.getChecksum());

				if (chunkEntry == null) {
					chunkEntry = new ChunkEntry(chunk.getChecksum(), chunk.getSize());
					db.addChunk(chunkEntry);
					
					return true;
				}
				
				return false;
			}
			
			@Override
			public void onFileStart(File file) {
				System.out.println("FILE OPEN   "+file);
				// Check if file exists, or create new
				fileHistory = db.getFileHistory(
						FileUtil.getRelativePath(appDatabaseDir, file), file.getName());
	
				if (fileHistory == null) {
					fileHistory = new FileHistory();
				}
	
				// Check for versions
				fileVersion = fileHistory.getLastVersion();
	
				if (fileVersion == null) {
					fileVersion = new FileVersion();
					fileVersion.setHistory(fileHistory);
				}
	
				fileVersion.setVersion(1L);
				fileVersion.setPath(FileUtil.getRelativePath(appDatabaseDir,
						file.getParentFile()));
				fileVersion.setName(file.getName());
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

				db.addMultiChunk(multiChunkEntry);
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
					db.addContent(content);
				}
				
				content = null;		
				

				// fileHistory.addVersion(fileVersion);
				db.addFileHistory(fileHistory);
				
			}
			
			
		});
		
		return db.getLocalDatabaseVersion();
	}

	private List<File> listFiles(File localDir) {
		final List<File> files = new ArrayList<File>();
		
		new FileLister(localDir, new FileListerAdapter() {
			@Override public void proceedFile(File f) { files.add(f); }			
		}).start();
		
		return files;
	}

	private Database loadLocalRepoDB(File appDir) {
		//DatabaseDAO dao = new DatabaseDAO();
		
		return new Database();
	}
	
	private void saveLocalRepoDB(Database db, long fromVersion, long toVersion, File appDatabaseDir) throws IOException {
		File localDatabaseFile = new File(appDatabaseDir+"/local.db");
		new DatabaseDAO().save(db, fromVersion, toVersion, localDatabaseFile);
	}

	private void init(Profile profile) throws Exception {   
    	profile.getAppDir().mkdirs();
    	profile.getAppCacheDir().mkdirs();
    	profile.getAppDatabaseDir().mkdirs();
	}
	
}
