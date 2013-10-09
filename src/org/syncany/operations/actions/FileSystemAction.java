package org.syncany.operations.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Set;
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
import org.syncany.database.FileVersionHelper;
import org.syncany.database.FileVersionHelper.FileVersionComparison;
import org.syncany.database.MultiChunkEntry;
import org.syncany.util.FileUtil;

public abstract class FileSystemAction {
	protected static final Logger logger = Logger.getLogger(FileSystemAction.class.getSimpleName()); 
	
	protected Config config;
	protected Database localDatabase;
	protected Database winningDatabase;
	protected FileVersion fileVersion1;
	protected FileVersion fileVersion2;
	protected FileVersionHelper fileVersionHelper;
	
	public FileSystemAction(Config config, Database localDatabase, Database winningDatabase, FileVersion file1, FileVersion file2) {
		this.config = config;
		this.localDatabase = localDatabase;
		this.winningDatabase = winningDatabase;
		this.fileVersion1 = file1;
		this.fileVersion2 = file2;
		this.fileVersionHelper = new FileVersionHelper(config);
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
						
			// Set permissions
			if (FileUtil.isWindows()) {
				if (reconstructedFileVersion.getDosAttributes() != null) {
					logger.log(Level.INFO, "     - Setting DOS attributes: "+reconstructedFileVersion.getDosAttributes()+" ...");

					DosFileAttributes dosAttrs = FileUtil.dosAttrsFromString(reconstructedFileVersion.getDosAttributes());					
					Path filePath = Paths.get(reconstructedFileInCache.getAbsolutePath());
					
					Files.setAttribute(filePath, "dos:readonly", dosAttrs.isReadOnly());
					Files.setAttribute(filePath, "dos:hidden", dosAttrs.isHidden());
					Files.setAttribute(filePath, "dos:archive", dosAttrs.isArchive());
					Files.setAttribute(filePath, "dos:system", dosAttrs.isSystem());
				}
			}
			else if (FileUtil.isUnixLikeOperatingSystem()) {
				if (reconstructedFileVersion.getPosixPermissions() != null) {
					logger.log(Level.INFO, "     - Setting POSIX permissions: "+reconstructedFileVersion.getPosixPermissions()+" ...");
					
					Set<PosixFilePermission> posixPerms = PosixFilePermissions.fromString(reconstructedFileVersion.getPosixPermissions());
					
					Path filePath = Paths.get(reconstructedFileInCache.getAbsolutePath());
					Files.setPosixFilePermissions(filePath, posixPerms);
				}
			}
			
			// Set timestamp
			reconstructedFileInCache.setLastModified(reconstructedFileVersion.getLastModified().getTime());			
			
			// Okay. Now move to real place
			File reconstructedFilesAtFinalLocation = new File(config.getLocalDir()+File.separator+reconstructedFileVersion.getPath());
			logger.log(Level.INFO, "     - Okay, now moving to "+reconstructedFilesAtFinalLocation+" ...");
			
			FileUtils.moveFile(reconstructedFileInCache, reconstructedFilesAtFinalLocation); // TODO [medium] This should be in a try/catch block
		}
		
		// Folder
		else if (reconstructedFileVersion.getType() == FileType.FOLDER) {
			File reconstructedFilesAtFinalLocation = new File(config.getLocalDir()+File.separator+reconstructedFileVersion.getPath());
			
			logger.log(Level.INFO, "     - Creating folder at "+reconstructedFilesAtFinalLocation+" ...");
			reconstructedFilesAtFinalLocation.mkdirs();
		}	
		
		// Symlink
		else if (reconstructedFileVersion.getType() == FileType.SYMLINK) {
			File reconstructedFilesAtFinalLocation = new File(config.getLocalDir()+File.separator+reconstructedFileVersion.getPath());
			File linkTargetFile = new File(reconstructedFileVersion.getLinkTarget());

			if (FileUtil.symlinksSupported()) {				
				logger.log(Level.INFO, "     - Creating symlink at "+reconstructedFilesAtFinalLocation+" (target: "+linkTargetFile+") ...");
				FileUtil.createSymlink(linkTargetFile, reconstructedFilesAtFinalLocation);
			}
			else {
				logger.log(Level.INFO, "     - Skipping symlink (not supported) at "+reconstructedFilesAtFinalLocation+" (target: "+linkTargetFile+") ...");
			}
		}
		
		else {
			logger.log(Level.INFO, "     - Unknown file type: "+reconstructedFileVersion.getType());
			throw new Exception("Unknown file type: "+reconstructedFileVersion.getType());
		}
	}
	
	protected void createConflictFile(FileVersion conflictingLocalVersion) throws IOException {
		File conflictingLocalFile = getAbsolutePathFile(conflictingLocalVersion.getPath());
		
		if (!conflictingLocalFile.exists()) {
			logger.log(Level.INFO, "     - Creation of conflict file not necessary. Locally conflicting file vanished from "+conflictingLocalFile);
			return;
		}
		
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
		FileUtils.moveFile(conflictingLocalFile, newConflictFile); // TODO [high] Should this be in a try/catch block? What if this throws an IOException?
	}
	
	protected boolean fileAsExpected(FileVersion expectedLocalFileVersion) {
		File actualLocalFile = getAbsolutePathFile(expectedLocalFileVersion.getPath());						
		FileVersionComparison fileVersionComparison = fileVersionHelper.compare(expectedLocalFileVersion, actualLocalFile, true);
		
		if (fileVersionComparison.equals()) {
			return true;
		}
		else {
			return false;
		}
	}
	
	protected boolean fileExists(FileVersion expectedLocalFileVersion) {
		File actualLocalFile = getAbsolutePathFile(expectedLocalFileVersion.getPath());
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
