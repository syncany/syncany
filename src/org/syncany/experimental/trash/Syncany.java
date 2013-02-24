package org.syncany.experimental.trash;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.syncany.Constants;
import org.syncany.chunk.Chunk;
import org.syncany.chunk.Chunker;
import org.syncany.chunk.CustomMultiChunker;
import org.syncany.chunk.FixedOffsetChunker;
import org.syncany.chunk.GzipCompressor;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.Transformer;
import org.syncany.config.Config;
import org.syncany.config.Profile;
import org.syncany.config.Settings;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.PluginInfo;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.experimental.db.ChunkEntry;
import org.syncany.experimental.db.DatabaseNEW;
import org.syncany.experimental.db.FileContent;
import org.syncany.experimental.db.Database;
import org.syncany.experimental.db.FileHistory;
import org.syncany.experimental.db.FileVersion;
import org.syncany.experimental.db.MultiChunkEntry;
import org.syncany.experimental.trash.Deduper.IndexerListener;
import org.syncany.util.FileLister;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;
import org.syncany.util.FileLister.FileListerAdapter;
import org.syncany.util.FileLister.FileListerListener;

public class Syncany {
	public static void main(String[] args) throws Exception {
		Syncany sy = new Syncany();

		// Parse arguments
		File configFile = new File(System.getProperty("user.dir")+File.separator+"config.json");
		
		if (args.length > 0) {
			configFile = new File(args[0]);
		}
				
		// Read config
		Config cfg = new Config(configFile);
		sy.init(cfg);
		
		// sy up
		sy.up(cfg);
	}
	
	public Syncany() {
		// Read config
		// ...
	}
	
	public UpstreamStatus up(Config cfg) throws FileNotFoundException, IOException {
		final File localRepoDir = new File("/tmp/syncany-test-db");
		final File localCacheDir = new File("/tmp/syncany-db-cache");		
		
		DatabaseNEW localRepoDB = loadLocalRepoDB(cfg.getAppDir());
		List<File> localFiles = listFiles(cfg.getRootDir());
		DatabaseNEW updatedRepoDB = index(localFiles,localRepoDB, localRepoDir, localCacheDir);
		
		UpstreamStatus statusCode = null;
		
		//FIXME
		/*TransferManager tm = (TransferManager) cfg.getConnection();
		if(uploadMultiChunks(updatedRepoDB.getNewestDatabaseVersion().getMetaMultiChunks(),cfg.getCacheDir(),tm)) {
			boolean status = uploadLocalRepoDB(updatedRepoDB);
			
			statusCode.setResponse(status ? "Job" : "Nope"); 
		} else
		{
			statusCode.setResponse("Error");
		}*/
		
		return statusCode;
	}	
	
	private boolean uploadMultiChunks(List<MultiChunkEntry> metaMultiChunks, String cacheDir, TransferManager tm) {
		//Find and upload given multi chunks by the power of grayskull
		return false;
	}

	private boolean uploadLocalRepoDB(DatabaseNEW updatedRepoDB) {
		//upload DB, either in one big file, or each version seperatly
		return true;
	}

	//FIXME
	private DatabaseNEW index(List<File> localFiles, final DatabaseNEW db, final File localRepoDir, final File localCacheDir) throws FileNotFoundException, IOException {
		final Deduper indexer = new Deduper();
		final List<File> files = new ArrayList<File>();
		Chunker chunker = new FixedOffsetChunker(16 * 1024);
		MultiChunker multiChunker = new CustomMultiChunker(512 * 1024, 0);
		Transformer transformer = new GzipCompressor();		
		
		indexer.deduplicate(files, chunker, multiChunker, transformer, new IndexerListener() {
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
						FileUtil.getRelativePath(localRepoDir, file), file.getName());
	
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
				fileVersion.setPath(FileUtil.getRelativePath(localRepoDir,
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
					return new File(localCacheDir 
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
		
		return db;
	}

	private List<File> listFiles(String rootDir) {
		final List<File> files = new ArrayList<File>();
		
		new FileLister(new File(rootDir), new FileListerAdapter() {
			@Override public void proceedFile(File f) { files.add(f); }			
		}).start();
		
		return files;
	}

	private DatabaseNEW loadLocalRepoDB(String appDir) {
		DatabaseNEW newRepoDB = null; 
		//load localRepoDB
		return newRepoDB;
	}

	public void init(Config configFile) throws Exception {   
	  	Profile profile; 
    	Connection conn = null;
    	File rootFolder;
    	
    	// create the needed directories if not already present
    	new File(configFile.getAppDir()).mkdirs();
    	new File(configFile.getCacheDir()).mkdirs();
    	new File(configFile.getRootDir()).mkdirs();
    	

    	/* ATTENTION: Keep this order in mind 
		 * Create and configure Settings first,
		 * then create Profile */
    	
    	// SETTING EVERYTHING IN SETTINGS-CLASS
    	Settings.createInstance(
    			new File(configFile.getAppDir()), 
    			new File(configFile.getCacheDir()),
    			configFile.getMachineName()
    	);

    	profile = Profile.getInstance();

    	// set encryption password & salt
    	profile.getRepository().getEncryption().setPassword(configFile.getEncryption().getPass());
    	profile.getRepository().getEncryption().setSalt("SALT"); // TODO: What to use as salt?    	
    	profile.getRepository().setChunkSize(Constants.DEFAULT_CHUNK_SIZE); // TODO: Make configurable

    	// Load the required plugin
    	PluginInfo plugin = Plugins.get(configFile.getConnection().getType());
    	
    	if (plugin == null) {
    		throw new Exception("Plugin not supported: " + configFile.getConnection().getType());
    	}
    	
    	// initialize connection
    	conn = plugin.createConnection();
    	conn.init(configFile.getConnection().getSettings());
 
    	profile.getRepository().setConnection(conn);
    	rootFolder = new File(configFile.getRootDir());
    	profile.setRoot(rootFolder);    
    	
    	// load DB
    	//if (p)
	}
	
}
