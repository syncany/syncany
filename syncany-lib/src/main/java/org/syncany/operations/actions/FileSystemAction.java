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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.FileVersionComparator;
import org.syncany.database.FileVersionComparator.FileChange;
import org.syncany.database.FileVersionComparator.FileVersionComparison;
import org.syncany.util.CollectionUtil;
import org.syncany.util.FileUtil;

public abstract class FileSystemAction {
	protected static final Logger logger = Logger.getLogger(FileSystemAction.class.getSimpleName()); 
	
	protected Config config;
	protected Database localDatabase;
	protected Database winningDatabase;
	protected FileVersion fileVersion1;
	protected FileVersion fileVersion2;
	protected FileVersionComparator fileVersionHelper;
	
	public FileSystemAction(Config config, Database localDatabase, Database winningDatabase, FileVersion file1, FileVersion file2) {
		this.config = config;
		this.localDatabase = localDatabase;
		this.winningDatabase = winningDatabase;
		this.fileVersion1 = file1;
		this.fileVersion2 = file2;
		this.fileVersionHelper = new FileVersionComparator(config.getLocalDir(), config.getChunker().getChecksumAlgorithm());
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

	protected void createSymlink(FileVersion reconstructedFileVersion) throws Exception {
		File reconstructedFileAtFinalLocation = getAbsolutePathFile(reconstructedFileVersion.getPath());

		if (FileUtil.symlinksSupported()) {				
			// Make directory if it does not exist
			File reconstructedFileParentDir = reconstructedFileAtFinalLocation.getParentFile();
			
			if (!FileUtil.exists(reconstructedFileParentDir)) {
				logger.log(Level.INFO, "     - Parent folder does not exist, creating "+reconstructedFileParentDir+" ...");
				reconstructedFileParentDir.mkdirs();
			}
			
			// Make link
			logger.log(Level.INFO, "     - Creating symlink at "+reconstructedFileAtFinalLocation+" (target: "+reconstructedFileVersion.getLinkTarget()+") ...");
			FileUtil.createSymlink(reconstructedFileVersion.getLinkTarget(), reconstructedFileAtFinalLocation);
		}
		else {
			logger.log(Level.INFO, "     - Skipping symlink (not supported) at "+reconstructedFileAtFinalLocation+" (target: "+reconstructedFileVersion.getLinkTarget()+") ...");
		}
	}
	
	protected void setLastModified(FileVersion reconstructedFileVersion) {
		File reconstructedFilesAtFinalLocation = getAbsolutePathFile(reconstructedFileVersion.getPath());
		setLastModified(reconstructedFileVersion, reconstructedFilesAtFinalLocation);
	}
	
	protected void setLastModified(FileVersion reconstructedFileVersion, File reconstructedFilesAtFinalLocation) {		
		reconstructedFilesAtFinalLocation.setLastModified(reconstructedFileVersion.getLastModified().getTime());			
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
		String conflictUserName = (config.getDisplayName() != null) ? config.getDisplayName() : config.getMachineName();
		String conflictDate = new SimpleDateFormat("d MMM yy, h-mm a").format(conflictingLocalVersion.getLastModified()); 
				
		boolean conflictCreatedByEndsWithS = conflictingLocalVersion.getCreatedBy().endsWith("s");
		boolean conflictFileHasExtension = conflictFileExtension != null && !"".equals(conflictFileExtension);
		
		String newFullName;
		
		if (conflictFileHasExtension) {
			if (conflictCreatedByEndsWithS) {
				newFullName = String.format("%s (%s' conflicted copy, %s).%s", 
						conflictBasename, conflictUserName, conflictDate, conflictFileExtension);
			}
			else {
				newFullName = String.format("%s (%s's conflicted copy, %s).%s", 
						conflictBasename, conflictUserName, conflictDate, conflictFileExtension);				
			}
		}
		else {
			if (conflictCreatedByEndsWithS) {
				newFullName = String.format("%s (%s' conflicted copy, %s)", 
						conflictBasename, conflictUserName, conflictDate);
			}
			else {
				newFullName = String.format("%s (%s's conflicted copy, %s)", 
						conflictBasename, conflictUserName, conflictDate);				
			}
		}
					
		File newConflictFile = new File(conflictDirectory+File.separator+newFullName);
		
		logger.log(Level.INFO, "     - Local version conflicts, moving local file "+conflictingLocalFile+" to "+newConflictFile+" ...");
		
		if (conflictingLocalFile.isDirectory()) {
			conflictingLocalFile.renameTo(newConflictFile); 
		}
		else {
			FileUtils.moveFile(conflictingLocalFile, newConflictFile);
		}
	}
	
	protected void setFileAttributes(FileVersion reconstructedFileVersion) throws IOException {
		File reconstructedFilesAtFinalLocation = getAbsolutePathFile(reconstructedFileVersion.getPath());
		setFileAttributes(reconstructedFileVersion, reconstructedFilesAtFinalLocation);
	}
	
	protected void setFileAttributes(FileVersion reconstructedFileVersion, File reconstructedFilesAtFinalLocation) throws IOException {
		if (FileUtil.isWindows()) {
			if (reconstructedFileVersion.getDosAttributes() != null) {
				logger.log(Level.INFO, "     - Setting DOS attributes: "+reconstructedFileVersion.getDosAttributes()+" ...");

				DosFileAttributes dosAttrs = FileUtil.dosAttrsFromString(reconstructedFileVersion.getDosAttributes());					
				Path filePath = Paths.get(reconstructedFilesAtFinalLocation.getAbsolutePath());
				
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
				
				Path filePath = Paths.get(reconstructedFilesAtFinalLocation.getAbsolutePath());
				Files.setPosixFilePermissions(filePath, posixPerms);
			}
		}		
	}
	
	protected boolean fileAsExpected(FileVersion expectedLocalFileVersion) {
		return fileAsExpected(expectedLocalFileVersion, new FileChange[] { });
	}	
	
	protected boolean fileAsExpected(FileVersion expectedLocalFileVersion, FileChange... allowedFileChanges) {
		FileVersionComparison fileVersionComparison = fileChanges(expectedLocalFileVersion);
		
		if (fileVersionComparison.equals()) {
			return true;
		}
		else if (allowedFileChanges.length > 0) {
			return CollectionUtil.containsOnly(fileVersionComparison.getFileChanges(), allowedFileChanges);
		}
		else {			
			return false;
		}
	}	
	
	protected FileVersionComparison fileChanges(FileVersion expectedLocalFileVersion) {
		File actualLocalFile = getAbsolutePathFile(expectedLocalFileVersion.getPath());						
		FileVersionComparison fileVersionComparison = fileVersionHelper.compare(expectedLocalFileVersion, actualLocalFile, true);
		
		return fileVersionComparison;
	}
	
	protected boolean fileExists(FileVersion expectedLocalFileVersion) {
		File actualLocalFile = getAbsolutePathFile(expectedLocalFileVersion.getPath());
		return FileUtil.exists(actualLocalFile);	
	}	
	
	protected void deleteFile(FileVersion deleteFileVersion) {
		File fromFileOnDisk = getAbsolutePathFile(deleteFileVersion.getPath());
		fromFileOnDisk.delete();		
	}
	
	protected File getAbsolutePathFile(String relativePath) {
		return new File(config.getLocalDir()+File.separator+relativePath);
	}	
	
	public abstract void execute() throws InconsistentFileSystemException, Exception;
	
	public static class InconsistentFileSystemException extends Exception {
		private static final long serialVersionUID = 14239478881237L;

		public InconsistentFileSystemException() {
			super();
		}

		public InconsistentFileSystemException(String message, Throwable cause) {
			super(message, cause);
		}

		public InconsistentFileSystemException(String message) {
			super(message);
		}

		public InconsistentFileSystemException(Throwable cause) {
			super(cause);
		}
	}
}
