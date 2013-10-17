package org.syncany.operations.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.ChunkEntry.ChunkEntryId;
import org.syncany.database.FileVersion.FileType;
import org.syncany.util.FileUtil;

public abstract class FileCreatingFileSystemAction extends FileSystemAction {
	public FileCreatingFileSystemAction(Config config, Database localDatabase, Database winningDatabase, FileVersion file1, FileVersion file2) {
		super(config, localDatabase, winningDatabase, file1, file2);
	}

	protected void createFileFolderOrSymlink(FileVersion reconstructedFileVersion) throws Exception {
		
		if (reconstructedFileVersion.getType() == FileType.FILE) {
			createFile0(reconstructedFileVersion);			
		}
		else if (reconstructedFileVersion.getType() == FileType.FOLDER) {
			createFolder(reconstructedFileVersion);			
		}	
		else if (reconstructedFileVersion.getType() == FileType.SYMLINK) {
			createSymlink(reconstructedFileVersion);
		}		
		else {
			logger.log(Level.INFO, "     - Unknown file type: "+reconstructedFileVersion.getType());
			throw new Exception("Unknown file type: "+reconstructedFileVersion.getType());
		}
	}
	
	protected void createFolder(FileVersion reconstructedFileVersion) throws IOException {
		File reconstructedFilesAtFinalLocation = getAbsolutePathFile(reconstructedFileVersion.getPath());
		logger.log(Level.INFO, "     - Creating folder at "+reconstructedFilesAtFinalLocation+" ...");
		
		reconstructedFilesAtFinalLocation.mkdirs();		
		setFileAttributes(reconstructedFileVersion);		
	}

	protected void createFile0(FileVersion reconstructedFileVersion) throws Exception {
		File reconstructedFilesAtFinalLocation = getAbsolutePathFile(reconstructedFileVersion.getPath());
		File reconstructedFileInCache = config.getCache().createTempFile("file-"+reconstructedFileVersion.getName()+"-"+reconstructedFileVersion.getVersion());
		logger.log(Level.INFO, "     - Creating file "+reconstructedFileVersion.getPath()+" to "+reconstructedFileInCache+" ...");				

		FileContent fileContent = localDatabase.getContent(reconstructedFileVersion.getChecksum()); 
		
		if (fileContent == null) {
			fileContent = winningDatabase.getContent(reconstructedFileVersion.getChecksum());
		}
		
		// Create file
		MultiChunker multiChunker = config.getMultiChunker();
		FileOutputStream reconstructedFileOutputStream = new FileOutputStream(reconstructedFileInCache);

		if (fileContent != null) { // File can be empty!
			Collection<ChunkEntryId> fileChunks = fileContent.getChunks();
			
			for (ChunkEntryId chunkChecksum : fileChunks) {
				MultiChunkEntry multiChunkForChunk = localDatabase.getMultiChunkForChunk(chunkChecksum);
				
				if (multiChunkForChunk == null) {
					multiChunkForChunk = winningDatabase.getMultiChunkForChunk(chunkChecksum);
				}
				
				File decryptedMultiChunkFile = config.getCache().getDecryptedMultiChunkFile(multiChunkForChunk.getId());

				// TODO [low] Make more sensible API for multichunking
				MultiChunk multiChunk = multiChunker.createMultiChunk(decryptedMultiChunkFile);
				InputStream chunkInputStream = multiChunk.getChunkInputStream(chunkChecksum.getArray());
				
				FileUtil.appendToOutputStream(chunkInputStream, reconstructedFileOutputStream);
			}
		}
		
		reconstructedFileOutputStream.close();		
							
		// Okay. Now move to real place
		logger.log(Level.INFO, "     - Okay, now moving to "+reconstructedFilesAtFinalLocation+" ...");
		
		FileUtils.moveFile(reconstructedFileInCache, reconstructedFilesAtFinalLocation); // TODO [medium] This should be in a try/catch block
		
		// Set attributes & timestamp
		setFileAttributes(reconstructedFileVersion);			
		setLastModified(reconstructedFileVersion);		
	}
}
