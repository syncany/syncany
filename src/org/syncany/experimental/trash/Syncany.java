package org.syncany.experimental.trash;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.syncany.Constants;
import org.syncany.chunk.chunking.Chunk;
import org.syncany.chunk.chunking.Chunker;
import org.syncany.chunk.chunking.FixedOffsetChunker;
import org.syncany.chunk.multi.CustomMultiChunker;
import org.syncany.chunk.multi.MultiChunk;
import org.syncany.chunk.multi.MultiChunker;
import org.syncany.config.Config;
import org.syncany.config.Profile;
import org.syncany.config.Settings;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.PluginInfo;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.experimental.db.ChunkEntry;
import org.syncany.experimental.db.Content;
import org.syncany.experimental.db.Database;
import org.syncany.experimental.db.FileHistory;
import org.syncany.experimental.db.FileVersion;
import org.syncany.experimental.db.MultiChunkEntry;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

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
		RepoDB localRepoDB = loadLocalRepoDB(cfg.getAppDir());
		List<File> localFiles = listFiles(cfg.getRootDir());
		RepoDB updatedRepoDB = index(localFiles,localRepoDB, null, null);
		
		UpstreamStatus statusCode = null;
		
		//FIXME
		TransferManager tm = (TransferManager) cfg.getConnection();
		if(uploadMultiChunks(updatedRepoDB.getNewestDBVersion().getMetaMultiChunks(),cfg.getCacheDir(),tm)) {
			boolean status = uploadLocalRepoDB(updatedRepoDB);
			
			statusCode.setResponse(status ? "Job" : "Nope"); 
		} else
		{
			statusCode.setResponse("Error");
		}
		
		return statusCode;
	}	
	
	private boolean uploadMultiChunks(List<MultiChunkEntry> metaMultiChunks, String cacheDir, TransferManager tm) {
		//Find and upload given multi chunks by the power of grayskull
		return false;
	}

	private boolean uploadLocalRepoDB(RepoDBVersion updatedRepoDB) {
		//upload DB, either in one big file, or each version seperatly
		return true;
	}

	//FIXME
	private RepoDB index(List<File> localFiles, RepoDBVersion localRepoDB, File localRepoDir, File localCacheDir) throws FileNotFoundException, IOException {

		// DB: Create DB and chunk/multichunk entries
		Database db = new Database();

		ChunkEntry chunkEntry = null;
		MultiChunkEntry multiChunkEntry = null;

		// Data: Create chunker and multichunker
		Chunker chunker = new FixedOffsetChunker(16 * 1024);
		MultiChunker multiChunker = new CustomMultiChunker(512 * 1024, 0);

		Chunk chunk = null;
		MultiChunk multiChunk = null;

		for (File file : localFiles) {
			// Check if file exists, or create new
			FileHistory fileHistory = db.getFileHistory(
					FileUtil.getRelativePath(localRepoDir, file), file.getName());

			if (fileHistory == null) {
				fileHistory = new FileHistory();
			}

			// Check for versions
			FileVersion fileVersion = fileHistory.getLastVersion();

			if (fileVersion == null) {
				fileVersion = db.createFileVersion(fileHistory);
			}

			fileVersion.setVersion(1L);
			fileVersion.setPath(FileUtil.getRelativePath(localRepoDir,
					file.getParentFile()));
			fileVersion.setName(file.getName());

			if (file.isFile()) {
				Content content = new Content();

				// Create chunks from file
				Enumeration<Chunk> chunks = chunker.createChunks(file);

				while (chunks.hasMoreElements()) {
					chunk = chunks.nextElement();

					// Update DB
					chunkEntry = db.getChunk(chunk.getChecksum());

					if (chunkEntry == null) {
						chunkEntry = db.createChunk(chunk.getChecksum(),
								chunk.getSize(), true);

						// Add chunk data to multichunk

						// - Check if multichunk full
						if (multiChunk != null && multiChunk.isFull()) {
							// Data
							multiChunk.close();
							multiChunk = null;

							// DB
							db.addMultiChunk(multiChunkEntry);
							multiChunkEntry = null;
						}

						// - Open new multichunk if none existant
						if (multiChunk == null) {
							// Data
							File multiChunkFile = new File(localCacheDir 
									+ "/multichunk-"
									+ StringUtil.toHex(chunk.getChecksum()));
							multiChunk = multiChunker.createMultiChunk(chunk
									.getChecksum(), new FileOutputStream(
									multiChunkFile));

							// DB
							multiChunkEntry = new MultiChunkEntry();
							multiChunkEntry.setChecksum(chunk.getChecksum());
						}

						// - Add chunk data
						multiChunk.write(chunk);
						multiChunkEntry.addChunk(chunkEntry);
					}

					content.addChunk(chunkEntry);
				}

				if (chunk != null) {
					content.setChecksum(chunk.getChecksum());
				} else {
					content.setChecksum(null);
				}

				fileVersion.setContent(content);
				db.addContent(content);
			}

			// fileHistory.addVersion(fileVersion);
			db.addFileHistory(fileHistory);
		}

		// Close and add last multichunk
		if (multiChunk != null) {
			// Data
			multiChunk.close();
			multiChunk = null;

			// DB
			db.addMultiChunk(multiChunkEntry);
			multiChunkEntry = null;
		}
		return (RepoDB) localRepoDB;
	}

	private List<File> listFiles(String rootDir) {
		List<File> files = new ArrayList<File>();
		return files;
	}

	private RepoDB loadLocalRepoDB(String appDir) {
		RepoDB newRepoDB = null; 
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
