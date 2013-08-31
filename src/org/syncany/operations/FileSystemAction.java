package org.syncany.operations;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.ChunkEntry.ChunkEntryId;
import org.syncany.database.Database;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.util.FileUtil;

public abstract class FileSystemAction {
	public static enum FileSystemActionType { FILE, FOLDER }; 
	protected static final Logger logger = Logger.getLogger(FileSystemAction.class.getSimpleName());
	
	protected Config config;
	protected Database localDatabase;
	protected Database winningDatabase;
	protected FileVersion file1;
	protected FileVersion file2;
	
	public FileSystemAction(Config config, Database localDatabase, Database winningDatabase, FileVersion file1, FileVersion file2) {
		this.config = config;
		this.localDatabase = localDatabase;
		this.winningDatabase = winningDatabase;
		this.file1 = file1;
		this.file2 = file2;
	}
	
	public FileVersion getFile1() {
		return file1;
	}

	public FileVersion getFile2() {
		return file2;
	}
	
	public FileSystemActionType getType() {
		return file1.isFolder() ? FileSystemActionType.FOLDER : FileSystemActionType.FILE;
	}

	protected void reconstructFile(FileVersion reconstructedFileVersion) throws Exception {
		if (!reconstructedFileVersion.isFolder()) {
			File reconstructedFileInCache = config.getCache().createTempFile("file-"+reconstructedFileVersion.getName()+"-"+reconstructedFileVersion.getVersion());
			FileOutputStream reconstructedFileOutputStream = new FileOutputStream(reconstructedFileInCache);

			logger.log(Level.INFO, "  + Reconstructing file "+reconstructedFileVersion.getFullName()+" to "+reconstructedFileInCache+" ...");				

			FileContent fileContent = localDatabase.getContent(reconstructedFileVersion.getChecksum()); 
			
			if (fileContent == null) {
				fileContent = winningDatabase.getContent(reconstructedFileVersion.getChecksum());
			}
			
			Collection<ChunkEntryId> fileChunks = fileContent.getChunks();
			
			for (ChunkEntryId chunkChecksum : fileChunks) {
				File chunkFile = config.getCache().getChunkFile(chunkChecksum.getArray());
				FileUtil.appendToOutputStream(chunkFile, reconstructedFileOutputStream);
			}
			
			reconstructedFileOutputStream.close();		
			
			// Set timestamp
			reconstructedFileInCache.setLastModified(reconstructedFileVersion.getLastModified().getTime());
			
			// Okay. Now move to real place
			File reconstructedFilesAtFinalLocation = new File(config.getLocalDir()+File.separator+reconstructedFileVersion.getFullName());
			logger.log(Level.INFO, "    * Okay, now moving to "+reconstructedFilesAtFinalLocation+" ...");
			
			FileUtil.renameVia(reconstructedFileInCache, reconstructedFilesAtFinalLocation);
		}
		
		// Folder
		else {
			File reconstructedFilesAtFinalLocation = new File(config.getLocalDir()+File.separator+reconstructedFileVersion.getFullName());
			
			logger.log(Level.INFO, "  + Creating folder at "+reconstructedFilesAtFinalLocation+" ...");
			FileUtil.mkdirsVia(reconstructedFilesAtFinalLocation);
		}									
	}
	
	protected void createConflictFile(FileVersion conflictingLocalVersion) {
		File conflictingLocalFile = getAbsolutePathFile(conflictingLocalVersion.getFullName());
		
		String newConflictExtension = FileUtil.getExtension(conflictingLocalFile);
		String newConflictBasename = FileUtil.getBasename(conflictingLocalFile)
				+ " ("+conflictingLocalVersion.getCreatedBy()+"'s conflict version, "+conflictingLocalVersion.getLastModified()+")";
		
		String newFullName = ("".equals(newConflictExtension)) ? newConflictBasename : newConflictBasename+"."+newConflictExtension;			
		File newConflictFile = new File(
				  FileUtil.getAbsoluteParentDirectory(conflictingLocalFile)
				+ File.separator
				+ newFullName);
		
		FileUtil.renameVia(conflictingLocalFile, newConflictFile);
	}
	
	protected boolean isExpectedFile(FileVersion expectedLocalFileVersion) {
		File actualLocalFile = getAbsolutePathFile(expectedLocalFileVersion.getFullName());
		
		boolean fileExists = actualLocalFile.exists();
		
		if (expectedLocalFileVersion.getStatus() == FileStatus.DELETED) {
			// Check existance
			if (fileExists) {
				return false;
			}
			else {
				return true;
			}
		}
		else {
			// Check existance
			if (!fileExists) {
				return false;
			}
			
			// Check file type (folder/file)
			if (actualLocalFile.isDirectory() != expectedLocalFileVersion.isFolder()) {
				return false;
			}
			
			// Check folder
			if (actualLocalFile.isDirectory()) {
				return true;
			}
			
			// Check modified date
			boolean modifiedEquals = expectedLocalFileVersion.getLastModified().equals(new Date(actualLocalFile.lastModified()));
			
			if (!modifiedEquals) {
				return false;
			}
			
			// Check size	
			FileContent expectedFileContent = localDatabase.getContent(expectedLocalFileVersion.getChecksum());
			
			if (expectedFileContent == null) {
				expectedFileContent = winningDatabase.getContent(expectedLocalFileVersion.getChecksum());
			}
			
			boolean isSizeEqual = expectedFileContent.getSize() == actualLocalFile.length();
			
			if (isSizeEqual) {
				return true;
			}
			else {
				return false;
			}
		}
	}
	
	protected File getAbsolutePathFile(String relativePath) {
		return new File(config.getLocalDir()+File.separator+relativePath);
	}
	
	public abstract void execute() throws Exception;
}
