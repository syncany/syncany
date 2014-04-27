/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.operations.down.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.logging.Level;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.config.Config;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.SqlDatabase;
import org.syncany.util.FileUtil;
import org.syncany.util.NormalizedPath;

public abstract class FileCreatingFileSystemAction extends FileSystemAction {
	public FileCreatingFileSystemAction(Config config, MemoryDatabase winningDatabase, FileVersion file1, FileVersion file2) {
		super(config, winningDatabase, file1, file2);		
	}

	protected void createFileFolderOrSymlink(FileVersion reconstructedFileVersion) throws Exception {
		if (reconstructedFileVersion.getType() == FileType.FILE) {
			createFile(reconstructedFileVersion);
		}
		else if (reconstructedFileVersion.getType() == FileType.FOLDER) {
			createFolder(reconstructedFileVersion);
		}
		else if (reconstructedFileVersion.getType() == FileType.SYMLINK) {
			createSymlink(reconstructedFileVersion);
		}
		else {
			logger.log(Level.INFO, "     - Unknown file type: " + reconstructedFileVersion.getType());
			throw new Exception("Unknown file type: " + reconstructedFileVersion.getType());
		}
	}

	protected void createFolder(FileVersion targetFileVersion) throws Exception {
		NormalizedPath targetDirPath = new NormalizedPath(config.getLocalDir(), targetFileVersion.getPath());
		
		logger.log(Level.INFO, "     - Creating folder at " + targetFileVersion + " ...");
		
		try {
			// Clean filename
			if (targetDirPath.hasIllegalChars()) {
				targetDirPath = targetDirPath.toCreatable("filename conflict", true);
				logger.log(Level.INFO, "     - Had illegal chars, cleaned to "+targetDirPath);
			}

			// Try creating it
			createFolder(targetDirPath);
			setFileAttributes(targetFileVersion, targetDirPath.toFile());
		}
		catch (Exception e) {
			throw new RuntimeException("What to do here?!", e);
		}
	}

	protected void createFile(FileVersion reconstructedFileVersion) throws Exception {
		File reconstructedFileInCache = assembleFileToCache(reconstructedFileVersion);		
		moveFileToFinalLocation(reconstructedFileInCache, reconstructedFileVersion);	
	}
	
	private File assembleFileToCache(FileVersion reconstructedFileVersion) throws Exception {
		SqlDatabase localDatabase = new SqlDatabase(config);

		File reconstructedFileInCache = config.getCache().createTempFile("reconstructedFileVersion");
		logger.log(Level.INFO, "     - Creating file " + reconstructedFileVersion.getPath() + " to " + reconstructedFileInCache + " ...");

		FileContent fileContent = localDatabase.getFileContent(reconstructedFileVersion.getChecksum(), true);

		if (fileContent == null) {
			fileContent = winningDatabase.getContent(reconstructedFileVersion.getChecksum());
		}
		
		// Check consistency!
		if (fileContent == null && reconstructedFileVersion.getChecksum() != null) {
			throw new Exception("Cannot determine file content for checksum "+reconstructedFileVersion.getChecksum());
		}

		// Create file
		// TODO [low] Create an assembler/reconstructor class to package re-assembly in the chunk-package
		MultiChunker multiChunker = config.getMultiChunker();
		FileOutputStream reconstructedFileOutputStream = new FileOutputStream(reconstructedFileInCache);

		if (fileContent != null) { // File can be empty!
			Collection<ChunkChecksum> fileChunks = fileContent.getChunks();

			for (ChunkChecksum chunkChecksum : fileChunks) {
				MultiChunkId multiChunkIdForChunk = localDatabase.getMultiChunkId(chunkChecksum);

				if (multiChunkIdForChunk == null) {
					multiChunkIdForChunk = winningDatabase.getMultiChunkIdForChunk(chunkChecksum);
				}

				File decryptedMultiChunkFile = config.getCache().getDecryptedMultiChunkFile(multiChunkIdForChunk);

				MultiChunk multiChunk = multiChunker.createMultiChunk(decryptedMultiChunkFile);
				InputStream chunkInputStream = multiChunk.getChunkInputStream(chunkChecksum.getRaw());

				FileUtil.appendToOutputStream(chunkInputStream, reconstructedFileOutputStream);

				chunkInputStream.close();
				multiChunk.close();
			}
		}

		reconstructedFileOutputStream.close();
		
		// Set attributes & timestamp
		setFileAttributes(reconstructedFileVersion, reconstructedFileInCache);
		setLastModified(reconstructedFileVersion, reconstructedFileInCache);
		
		return reconstructedFileInCache;
	}	
	
	private void moveFileToFinalLocation(File reconstructedFileInCache, FileVersion targetFileVersion) throws IOException {
		NormalizedPath originalPath = new NormalizedPath(config.getLocalDir(), targetFileVersion.getPath());
		NormalizedPath targetPath = originalPath;
				
		try {
			// Clean filename
			if (targetPath.hasIllegalChars()) {
				targetPath = targetPath.toCreatable("filename conflict", true);
			}

			// Try creating folder
			createFolder(targetPath.getParent());
		}
		catch (Exception e) {
			throw new RuntimeException("What to do here?!");
		}
		
		// Try moving file to final destination 
		try {
			FileUtils.moveFile(reconstructedFileInCache, targetPath.toFile());
		}
		catch (FileExistsException e) {
			moveToConflictFile(targetPath);
		}
		catch (Exception e) {
			throw new RuntimeException("What to do here?!");
		}		
	}
	
	protected void createFolder(NormalizedPath targetDir) throws Exception {		
		if (!FileUtil.exists(targetDir.toFile())) {
			logger.log(Level.INFO, "     - Creating folder at " + targetDir.toFile() + " ...");
			boolean targetDirCreated = targetDir.toFile().mkdirs();
			
			if (!targetDirCreated) {
				throw new Exception("Cannot create target dir: "+targetDir);
			}
		}
		else if (!FileUtil.isDirectory(targetDir.toFile())) {
			logger.log(Level.INFO, "     - Expected a folder at " + targetDir.toFile() + " ...");
			//throw new FileExistsException("Cannot create target parent directory: "+targetDir);
			moveToConflictFile(targetDir);
		}
	}
}
