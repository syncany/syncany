/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.operations.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.config.Config;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.Database;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MultiChunkEntry;
import org.syncany.util.EnvUtil;
import org.syncany.util.FileUtil;
import org.syncany.util.FileUtil.NormalizedPath;
import org.syncany.util.StringUtil;

public abstract class FileCreatingFileSystemAction extends FileSystemAction {
	public FileCreatingFileSystemAction(Config config, Database localDatabase, Database winningDatabase, FileVersion file1, FileVersion file2) {
		super(config, localDatabase, winningDatabase, file1, file2);
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

	protected void createFolder(FileVersion reconstructedFileVersion) throws IOException {
		File reconstructedFilesAtFinalLocation = getAbsolutePathFile(reconstructedFileVersion.getPath());
		logger.log(Level.INFO, "     - Creating folder at " + reconstructedFilesAtFinalLocation + " ...");

		reconstructedFilesAtFinalLocation.mkdirs();
		setFileAttributes(reconstructedFileVersion);
	}

	protected void createFile(FileVersion reconstructedFileVersion) throws Exception {
		File reconstructedFileInCache = assembleFileToCache(reconstructedFileVersion);		
		moveFileToFinalLocation(reconstructedFileInCache, reconstructedFileVersion);	
	}
	
	private File assembleFileToCache(FileVersion reconstructedFileVersion) throws Exception {
		File reconstructedFileInCache = config.getCache().createTempFile("reconstructedFileVersion");
		logger.log(Level.INFO, "     - Creating file " + reconstructedFileVersion.getPath() + " to " + reconstructedFileInCache + " ...");

		FileContent fileContent = localDatabase.getContent(reconstructedFileVersion.getChecksum());

		if (fileContent == null) {
			fileContent = winningDatabase.getContent(reconstructedFileVersion.getChecksum());
		}

		// Create file
		// TODO [low] Create an assembler/reconstructor class to package re-assembly in the chunk-package
		MultiChunker multiChunker = config.getMultiChunker();
		FileOutputStream reconstructedFileOutputStream = new FileOutputStream(reconstructedFileInCache);

		if (fileContent != null) { // File can be empty!
			Collection<ChunkChecksum> fileChunks = fileContent.getChunks();

			for (ChunkChecksum chunkChecksum : fileChunks) {
				MultiChunkEntry multiChunkForChunk = localDatabase.getMultiChunkForChunk(chunkChecksum);

				if (multiChunkForChunk == null) {
					multiChunkForChunk = winningDatabase.getMultiChunkForChunk(chunkChecksum);
				}

				File decryptedMultiChunkFile = config.getCache().getDecryptedMultiChunkFile(multiChunkForChunk.getId().getRaw());

				MultiChunk multiChunk = multiChunker.createMultiChunk(decryptedMultiChunkFile);
				InputStream chunkInputStream = multiChunk.getChunkInputStream(chunkChecksum.getRaw());

				FileUtil.appendToOutputStream(chunkInputStream, reconstructedFileOutputStream);
			}
		}

		reconstructedFileOutputStream.close();
		
		// Set attributes & timestamp
		setFileAttributes(reconstructedFileVersion, reconstructedFileInCache);
		setLastModified(reconstructedFileVersion, reconstructedFileInCache);
		
		return reconstructedFileInCache;
	}

	
	private boolean hasIllegalChars(String pathPart) {
		if (EnvUtil.isWindows() && pathPart.matches("[\\/:*?\"<>|]")) {
			return true;
		}
		else if (pathPart.matches("[/]")) {
			return true;
		}
		
		return false;
	}
	
	private String cleanIllegalChars(String pathPart) {
		if (FileUtil.isWindows()) {
			String cleanedFilePath = pathPart.replaceAll("[\\/:*?\"<>|]","");						
			return cleanedFilePath;
		}
		else {
			return pathPart.replaceAll("[/]","");
		}
	}	
	
	private String cleanAsciiOnly(String pathPart) {
		return pathPart.replaceAll("[^a-zA-Z0-9., ]","");
	}	
	
	private String addFilenameConflictSuffix(String pathPart) {
		String conflictFileExtension = FileUtil.getExtension(pathPart, false);		
		boolean originalFileHasExtension = conflictFileExtension != null && !"".equals(conflictFileExtension);

		if (originalFileHasExtension) {
			return String.format("%s (filename conflict).%s", pathPart, conflictFileExtension);						
		}
		else {
			return String.format("%s (filename conflict)", pathPart);
		}
	}
	
	private boolean canCreate(String pathPart) {
		File tmpFile = new File(config.getCacheDir()+File.separator+pathPart);

		try {
			tmpFile.createNewFile();
			tmpFile.delete();

			return true;
		}
		catch (IOException e) {
			logger.log(Level.SEVERE, "WARNING: Cannot create file: "+tmpFile);
			return false;
		}
	}
	
	private void moveFileToFinalLocation(File reconstructedFileInCache, FileVersion targetFileVersion) throws IOException {
		File reconstructedFileAtFinalLocation = getAbsolutePathFile(targetFileVersion.getPath());
		NormalizedPath relativeNormalizedTargetPath = new NormalizedPath(targetFileVersion.getPath());
		
		// Create cleaned path
		List<String> cleanedRelativePathParts = new ArrayList<String>();
		
		for (String pathPart : relativeNormalizedTargetPath.getParts()) {
			if (hasIllegalChars(pathPart)) {
				String cleanedParentPart = addFilenameConflictSuffix(cleanIllegalChars(pathPart));
				
				if (canCreate(cleanedParentPart)) {
					pathPart = cleanedParentPart;
				}
				else {
					pathPart = addFilenameConflictSuffix(cleanAsciiOnly(pathPart));
				}				
			}
			
			cleanedRelativePathParts.add(pathPart);			
		}
		
		String cleanedRelativeTargetPath = StringUtil.join(cleanedRelativePathParts, File.separator);
		NormalizedPath normalizedCleanedRelativeTargetPath = new NormalizedPath(cleanedRelativeTargetPath);
		
		File absoluteTargetDir = new File(config.getLocalDir()+File.separator+normalizedCleanedRelativeTargetPath.getParent());
		File absoluteTargetFile = new File(config.getLocalDir()+File.separator+normalizedCleanedRelativeTargetPath);
			

		// Make directory if it does not exist

		if (!FileUtil.exists(absoluteTargetDir)) {
			logger.log(Level.INFO, "     - Parent folder does not exist, creating " + absoluteTargetDir + " ...");
			absoluteTargetDir.mkdirs();
		}

		logger.log(Level.INFO, "     - Okay, now moving to " + absoluteTargetFile + " ...");
		FileUtils.moveFile(reconstructedFileInCache, absoluteTargetFile); // TODO [medium] This should be in a try/catch block

		
	
		
		/*     pictures/
		 *       some/
		 *         folder/
		 *           file.jpg
		 *       some\\folder/
		 * ->       file.jpg
		 * 
		 *  relativeNormalizedPath = pictures/some\\folder/file.jpg
		 */
		
	}

	private boolean isIllegalFilename(File file) throws IOException {
		try {
			file.createNewFile();
			file.delete();

			return false;
		}
		catch (IOException e) {
			logger.log(Level.SEVERE, "WARNING: Illegal file: "+file);
			return true;
		}
	}	

	private File cleanFilename(File conflictFile) {
		String originalDirectory = FileUtil.getDatabaseParentDirectory(conflictFile.getAbsolutePath());
		String originalName = FileUtil.getDatabaseBasename(conflictFile.getAbsolutePath());
		
		String conflictName = cleanOsSpecificIllegalFilenames(originalName);
				
		String conflictBasename = FileUtil.getDatabaseBasename(conflictName);
		String conflictFileExtension = FileUtil.getExtension(conflictName, false);
				
		boolean originalFileHasExtension = conflictFileExtension != null && !"".equals(conflictFileExtension);

		String newFullName;

		if (originalFileHasExtension) {
			newFullName = String.format("%s (filename conflict).%s", conflictBasename, conflictFileExtension);						
		}
		else {
			newFullName = String.format("%s (filename conflict)", conflictBasename);
		}

		return new File(originalDirectory+File.separator+newFullName);
	}
	
	private String cleanOsSpecificIllegalFilenames(String originalFilename) {
		if (FileUtil.isWindows()) {
			String cleanedFilePath = originalFilename.replaceAll("[\\/:*?\"<>|]","");
			
			if (originalFilename.endsWith(".")) {
				cleanedFilePath = cleanedFilePath.substring(0, cleanedFilePath.length()-1);
			}	
			
			// TODO [medium] Many Windows special cases missing: COM, (empty), ...
			
			return cleanedFilePath;
		}
		else {
			return originalFilename.replaceAll("[/]","");
		}
	}		
}
