package org.syncany.operations.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.config.Config;
import org.syncany.database.ChunkEntry.ChunkEntryId;
import org.syncany.database.Database;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MultiChunkEntry;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public abstract class FileSystemAction {
	protected static final Logger logger = Logger.getLogger(FileSystemAction.class.getSimpleName()); 
	
	protected Config config;
	protected Database localDatabase;
	protected Database winningDatabase;
	protected FileVersion fileVersion1;
	protected FileVersion fileVersion2;
	
	public FileSystemAction(Config config, Database localDatabase, Database winningDatabase, FileVersion file1, FileVersion file2) {
		this.config = config;
		this.localDatabase = localDatabase;
		this.winningDatabase = winningDatabase;
		this.fileVersion1 = file1;
		this.fileVersion2 = file2;
	}
	
	public FileVersion getFile1() {
		return fileVersion1;
	}

	public FileVersion getFile2() {
		return fileVersion2;
	}
	
	public FileType getType() {
		if (fileVersion1 != null) {
			return fileVersion1.getType();
		}
		else {
			return fileVersion2.getType();
		}
	}

	protected void createFile(FileVersion reconstructedFileVersion) throws Exception {
		if (reconstructedFileVersion.getType() == FileType.FILE) {
			File reconstructedFileInCache = config.getCache().createTempFile("file-"+reconstructedFileVersion.getName()+"-"+reconstructedFileVersion.getVersion());

			logger.log(Level.INFO, "     - Creating file "+reconstructedFileVersion.getFullName()+" to "+reconstructedFileInCache+" ...");				

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
			
			// Set timestamp
			reconstructedFileInCache.setLastModified(reconstructedFileVersion.getLastModified().getTime());
			
			// Okay. Now move to real place
			File reconstructedFilesAtFinalLocation = new File(config.getLocalDir()+File.separator+reconstructedFileVersion.getFullName());
			logger.log(Level.INFO, "     - Okay, now moving to "+reconstructedFilesAtFinalLocation+" ...");
			
			FileUtils.moveFile(reconstructedFileInCache, reconstructedFilesAtFinalLocation);
		}
		
		// Folder
		else {
			File reconstructedFilesAtFinalLocation = new File(config.getLocalDir()+File.separator+reconstructedFileVersion.getFullName());
			
			logger.log(Level.INFO, "     - Creating folder at "+reconstructedFilesAtFinalLocation+" ...");
			reconstructedFilesAtFinalLocation.mkdirs();
		}									
	}
	
	protected void createConflictFile(FileVersion conflictingLocalVersion) throws IOException {
		File conflictingLocalFile = getAbsolutePathFile(conflictingLocalVersion.getFullName());
		
		String conflictDirectory = FileUtil.getAbsoluteParentDirectory(conflictingLocalFile);
		String conflictBasename = FileUtil.getBasename(conflictingLocalFile);
		String conflictFileExtension = FileUtil.getExtension(conflictingLocalFile);		
		String conflictMachineName = config.getMachineName();
		String conflictDate = new SimpleDateFormat("d MMM yy, h-mm a").format(conflictingLocalVersion.getLastModified()); 
				
		boolean conflictCreatedByEndsWithS = conflictingLocalVersion.getCreatedBy().endsWith("s");
		boolean conflictFileHasExtension = conflictFileExtension != null && !"".equals(conflictFileExtension);
		
		String newFullName;
		
		if (conflictFileHasExtension) {
			if (conflictCreatedByEndsWithS) {
				newFullName = String.format("%s (%s' conflicted copy, %s).%s", 
						conflictBasename, conflictMachineName, conflictDate, conflictFileExtension);
			}
			else {
				newFullName = String.format("%s (%s's conflicted copy, %s).%s", 
						conflictBasename, conflictMachineName, conflictDate, conflictFileExtension);				
			}
		}
		else {
			if (conflictCreatedByEndsWithS) {
				newFullName = String.format("%s (%s' conflicted copy, %s)", 
						conflictBasename, conflictMachineName, conflictDate);
			}
			else {
				newFullName = String.format("%s (%s's conflicted copy, %s)", 
						conflictBasename, conflictMachineName, conflictDate);				
			}
		}
					
		File newConflictFile = new File(conflictDirectory+File.separator+newFullName);
		
		logger.log(Level.INFO, "     - Local version conflicts, moving local file "+conflictingLocalFile+" to "+newConflictFile+" ...");
		FileUtils.moveFile(conflictingLocalFile, newConflictFile);
	}
	
	// TODO [medium] This is duplicate code, the indexer and the status operation also compare a FileVersion to a local file
	protected boolean fileAsExpected(FileVersion expectedLocalFileVersion) {
		File actualLocalFile = getAbsolutePathFile(expectedLocalFileVersion.getFullName());		
		boolean actualLocalFileExists = actualLocalFile.exists();
		
		// Check existance
		if (!actualLocalFileExists) {
			logger.log(Level.INFO, "     - Unexpected file detected, is expected to EXIST, but does not: "+actualLocalFile);
			return false;
		}
		
		// Check file type (folder/file)
		if ((actualLocalFile.isDirectory() && expectedLocalFileVersion.getType() != FileType.FOLDER)
				|| (actualLocalFile.isFile() && expectedLocalFileVersion.getType() != FileType.FILE)) {
			
			if (logger.isLoggable(Level.INFO)) {
				String actualFileType = (actualLocalFile.isDirectory()) ? "DIRECTORY" : "FILE";
				String expectedFileType = expectedLocalFileVersion.getType().toString();
				
				logger.log(Level.INFO, "     - Unexpected file detected, is expected to be the same file type: "+actualLocalFile+" is "+actualFileType+", expected for "+expectedLocalFileVersion+" is "+expectedFileType);
			}
			
			return false;
		}
		
		// Check folder
		if (actualLocalFile.isDirectory()) {
			return true;
		}
		
		// Check modified date
		boolean modifiedEquals = expectedLocalFileVersion.getLastModified().equals(new Date(actualLocalFile.lastModified()));
		
		if (!modifiedEquals) {
			logger.log(Level.INFO, "     - Unexpected file detected, modified date differs: "+actualLocalFile+" was modified "+new Date(actualLocalFile.lastModified())+", expected for "+expectedLocalFileVersion+" is "+expectedLocalFileVersion.getLastModified());
			return false;
		}
		
		// Check size	
		if (expectedLocalFileVersion.getChecksum() == null) { // File can be empty!
			if (actualLocalFile.length() == 0) {
				return true;
			}
			else {
				logger.log(Level.INFO, "     - Unexpected file detected, empty file expected: "+actualLocalFile+" has size "+actualLocalFile.length()+", expected for "+expectedLocalFileVersion+" is 0");
				return false;
			}			
		}
		
		FileContent expectedFileContent = localDatabase.getContent(expectedLocalFileVersion.getChecksum());
		
		if (expectedFileContent == null) {
			expectedFileContent = winningDatabase.getContent(expectedLocalFileVersion.getChecksum());
			
			if (expectedFileContent == null) {
				// TODO [low] This should be an Exception instead of an error message. 
				logger.log(Level.SEVERE, "WARNING: Content for "+expectedLocalFileVersion+" not found using checksum "+StringUtil.toHex(expectedLocalFileVersion.getChecksum()));
				return false;
			}
		}
		
		boolean isSizeEqual = expectedFileContent.getSize() == actualLocalFile.length();
		
		if (!isSizeEqual) {
			logger.log(Level.INFO, "     - Unexpected file detected, size differs: "+actualLocalFile+" has size "+actualLocalFile.length()+", expected for "+expectedLocalFileVersion+" is "+expectedFileContent.getSize());
			return false;
		}
		
		// Check checksum 
		try {
			byte[] actualFileChecksum = FileUtil.createChecksum(actualLocalFile);
			boolean isChecksumEqual = Arrays.equals(actualFileChecksum, expectedFileContent.getChecksum());
			
			if (isChecksumEqual) {
				return true;
			}
			else {
				logger.log(Level.INFO, "     - Unexpected file detected, checksum differs: "+actualLocalFile+" -> "+StringUtil.toHex(actualFileChecksum)+", expected for "+expectedLocalFileVersion+" -> "+StringUtil.toHex(expectedFileContent.getChecksum()));
				return false;
			}
		}
		catch (Exception e) {
			logger.log(Level.INFO, "     - Unexpected behavior: Unable to create checksum for local file, assuming differs: "+actualLocalFile);
			return false;
		}
	}
	
	protected boolean fileExists(FileVersion expectedLocalFileVersion) {
		File actualLocalFile = getAbsolutePathFile(expectedLocalFileVersion.getFullName());
		boolean actualLocalFileExists = actualLocalFile.exists();
		
		// Check existence
		if (actualLocalFileExists) {
			logger.log(Level.INFO, "     - Unexpected file detected, is expected to be NON-EXISTANT, but exists: "+actualLocalFile);
			return true;
		}
		else {
			return false;
		}
	}	
	
	protected File getAbsolutePathFile(String relativePath) {
		return new File(config.getLocalDir()+File.separator+relativePath);
	}
	
	public abstract void execute() throws Exception;
}
