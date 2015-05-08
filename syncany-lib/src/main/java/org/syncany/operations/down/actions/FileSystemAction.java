/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.syncany.config.Config;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.FileVersionComparator;
import org.syncany.database.FileVersionComparator.FileChange;
import org.syncany.database.FileVersionComparator.FileVersionComparison;
import org.syncany.database.MemoryDatabase;
import org.syncany.util.CollectionUtil;
import org.syncany.util.EnvironmentUtil;
import org.syncany.util.FileUtil;
import org.syncany.util.NormalizedPath;

/**
 * File system actions perform operations on the local disk -- creating, updating and
 * deleting files. Given an expected and a new {@link FileVersion} (namely file1 and file2),
 * the concrete implementation of a file system action performs an action on the file.
 *
 * <p>Implementations of this class treat file1 and file2 differently, depending on what
 * action they implement.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class FileSystemAction {
	protected static final Logger logger = Logger.getLogger(FileSystemAction.class.getSimpleName());

	protected Config config;
	protected MemoryDatabase winningDatabase;
	protected FileVersion fileVersion1;
	protected FileVersion fileVersion2;
	protected FileVersionComparator fileVersionHelper;

	public FileSystemAction(Config config, MemoryDatabase winningDatabase, FileVersion file1, FileVersion file2) {
		this.config = config;
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

		if (EnvironmentUtil.symlinksSupported()) {
			// Make directory if it does not exist
			File reconstructedFileParentDir = reconstructedFileAtFinalLocation.getParentFile();

			if (!FileUtil.exists(reconstructedFileParentDir)) {
				logger.log(Level.INFO, "     - Parent folder does not exist, creating " + reconstructedFileParentDir + " ...");
				reconstructedFileParentDir.mkdirs();
			}

			// Make link
			logger.log(Level.INFO,
					"     - Creating symlink at " + reconstructedFileAtFinalLocation + " (target: " + reconstructedFileVersion.getLinkTarget()
					+ ") ...");
			FileUtil.createSymlink(reconstructedFileVersion.getLinkTarget(), reconstructedFileAtFinalLocation);
		}
		else {
			logger.log(Level.INFO, "     - Skipping symlink (not supported) at " + reconstructedFileAtFinalLocation + " (target: "
					+ reconstructedFileVersion.getLinkTarget() + ") ...");
		}
	}

	protected void setLastModified(FileVersion reconstructedFileVersion) {
		File reconstructedFilesAtFinalLocation = getAbsolutePathFile(reconstructedFileVersion.getPath());
		setLastModified(reconstructedFileVersion, reconstructedFilesAtFinalLocation);
	}

	protected void setLastModified(FileVersion reconstructedFileVersion, File reconstructedFilesAtFinalLocation) {
		// Using Files.setLastModifiedTime() instead of File.setLastModified()  
		// due to pre-1970 issue. See #374 for details.
		
		try {
			FileTime newLastModifiedTime = FileTime.fromMillis(reconstructedFileVersion.getLastModified().getTime());
			Files.setLastModifiedTime(reconstructedFilesAtFinalLocation.toPath(), newLastModifiedTime);
		}
		catch (IOException e) {
			logger.log(Level.WARNING, "Warning: Could not set last modified date for file " + reconstructedFilesAtFinalLocation + "; Ignoring error.", e);
		}		
	}

	protected void moveToConflictFile(FileVersion targetFileVersion) throws IOException {
		NormalizedPath targetConflictingFile = new NormalizedPath(config.getLocalDir(), targetFileVersion.getPath());
		moveToConflictFile(targetConflictingFile);
	}

	protected void moveToConflictFile(NormalizedPath conflictingPath) throws IOException {
		if (!FileUtil.exists(conflictingPath.toFile())) {
			logger.log(Level.INFO, "     - Creation of conflict file not necessary. Locally conflicting file vanished from " + conflictingPath);
			return;
		}

		int attempts = 0;

		while (attempts++ < 10) {
			NormalizedPath conflictedCopyPath = null;

			try {
				conflictedCopyPath = findConflictFilename(conflictingPath);
				logger.log(Level.INFO, "     - Local version conflicts, moving local file " + conflictingPath + " to " + conflictedCopyPath + " ...");

				if (conflictingPath.toFile().isDirectory()) {
					FileUtils.moveDirectory(conflictingPath.toFile(), conflictedCopyPath.toFile());
				}
				else {
					FileUtils.moveFile(conflictingPath.toFile(), conflictedCopyPath.toFile());
				}

				// Success!
				break;
			}
			catch (FileExistsException e) {
				logger.log(Level.SEVERE, "     - Cannot create conflict file; attempt = " + attempts + " for file: " + conflictedCopyPath, e);
			}
			catch (FileNotFoundException e) {
				logger.log(Level.INFO, "     - Conflict file vanished. Don't care!", e);
			}
			catch (Exception e) {
				throw new RuntimeException("What to do here?", e);
			}
		}
	}

	protected File moveFileToFinalLocation(File reconstructedFileInCache, FileVersion targetFileVersion) throws IOException {
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
			logger.log(Level.FINE, "File already existed", e);
			moveToConflictFile(targetPath);
		}
		catch (Exception e) {
			throw new RuntimeException("What to do here?!");
		}

		return targetPath.toFile();
	}

	protected void createFolder(NormalizedPath targetDir) throws Exception {
		if (!FileUtil.exists(targetDir.toFile())) {
			logger.log(Level.INFO, "     - Creating folder at " + targetDir.toFile() + " ...");
			boolean targetDirCreated = targetDir.toFile().mkdirs();

			if (!targetDirCreated) {
				throw new Exception("Cannot create target dir: " + targetDir);
			}
		}
		else if (!FileUtil.isDirectory(targetDir.toFile())) {
			logger.log(Level.INFO, "     - Expected a folder at " + targetDir.toFile() + " ...");
			moveToConflictFile(targetDir);
		}
	}

	private NormalizedPath findConflictFilename(NormalizedPath conflictingPath) throws Exception {
		String conflictUserName = (config.getDisplayName() != null) ? config.getDisplayName() : config.getMachineName();
		boolean conflictUserNameEndsWithS = conflictUserName.endsWith("s");
		String conflictDate = new SimpleDateFormat("d MMM yy, h-mm a").format(new Date());

		String conflictFilenameSuffix;

		if (conflictUserNameEndsWithS) {
			conflictFilenameSuffix = String.format("%s' conflicted copy, %s", conflictUserName, conflictDate);
		}
		else {
			conflictFilenameSuffix = String.format("%s's conflicted copy, %s", conflictUserName, conflictDate);
		}

		return conflictingPath.withSuffix(conflictFilenameSuffix, false);
	}

	protected void setFileAttributes(FileVersion reconstructedFileVersion) throws IOException {
		File reconstructedFilesAtFinalLocation = getAbsolutePathFile(reconstructedFileVersion.getPath());
		setFileAttributes(reconstructedFileVersion, reconstructedFilesAtFinalLocation);
	}

	protected void setFileAttributes(FileVersion reconstructedFileVersion, File reconstructedFilesAtFinalLocation) throws IOException {
		if (EnvironmentUtil.isWindows()) {
			if (reconstructedFileVersion.getDosAttributes() != null) {
				logger.log(Level.INFO, "     - Setting DOS attributes: " + reconstructedFileVersion.getDosAttributes() + " ...");

				DosFileAttributes dosAttrs = FileUtil.dosAttrsFromString(reconstructedFileVersion.getDosAttributes());
				Path filePath = Paths.get(reconstructedFilesAtFinalLocation.getAbsolutePath());

				try {
					Files.setAttribute(filePath, "dos:readonly", dosAttrs.isReadOnly());
					Files.setAttribute(filePath, "dos:hidden", dosAttrs.isHidden());
					Files.setAttribute(filePath, "dos:archive", dosAttrs.isArchive());
					Files.setAttribute(filePath, "dos:system", dosAttrs.isSystem());
				}
				catch (IOException e) {
					logger.log(Level.WARNING, "     - WARNING: Cannot set file attributes for " + filePath, e);
				}
			}
		}
		else if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			if (reconstructedFileVersion.getPosixPermissions() != null) {
				logger.log(Level.INFO, "     - Setting POSIX permissions: " + reconstructedFileVersion.getPosixPermissions() + " ...");

				Set<PosixFilePermission> posixPerms = PosixFilePermissions.fromString(reconstructedFileVersion.getPosixPermissions());

				Path filePath = Paths.get(reconstructedFilesAtFinalLocation.getAbsolutePath());

				try {
					Files.setPosixFilePermissions(filePath, posixPerms);
				}
				catch (IOException e) {
					logger.log(Level.WARNING, "     - WARNING: Cannot set file permissions for " + filePath, e);
				}
			}
		}
	}

	protected boolean fileAsExpected(FileVersion expectedLocalFileVersion) {
		return fileAsExpected(expectedLocalFileVersion, new FileChange[] {});
	}

	protected boolean fileAsExpected(FileVersion expectedLocalFileVersion, FileChange... allowedFileChanges) {
		FileVersionComparison fileVersionComparison = fileChanges(expectedLocalFileVersion);

		if (fileVersionComparison.areEqual()) {
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
		return new File(config.getLocalDir(), relativePath); // TODO [medium] This does not work for 'some\file' on windows!
	}	
	
	public abstract FileSystemActionResult execute() throws Exception;
}
