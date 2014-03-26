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
package org.syncany.database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.util.EnvironmentUtil;
import org.syncany.util.FileUtil;

/**
 * The file version comparator is a helper class to compare {@link FileVersion}s with each 
 * other, or compare {@link FileVersion}s to local {@link File}s. 
 * 
 * <p>It captures the {@link FileProperties} of two files or file versions and compares them
 * using the various <tt>compare*</tt>-methods. A comparison returns a set of {@link FileChange}s,
 * each of which identifies a certain attribute change (e.g. checksum changed, name changed).
 * A file can be considered equal if the returned set of {@link FileChange}s is empty.
 * 
 * <p>The file version comparator distinguishes between <i>cancelling</i> tests and regular tests.
 * Cancelling tests are implemented in {@link #performCancellingTests(FileVersionComparison) performCancellingTests()}.
 * They represent significant changes in a file, for which further comparison would not make
 * sense (e.g. new vs. deleted files or files vs. folders). If a cancelling test is not successful,
 * other tests are not performed.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FileVersionComparator {
	private static final Logger logger = Logger.getLogger(FileVersionComparator.class.getSimpleName());
	private File rootFolder;
	private String checksumAlgorithm;

	/**
	 * Creates a new file version comparator helper class. 
	 * 
	 * <p>The <tt>rootFolder</tt> is needed to allow a comparison of the relative file path.
	 * The <tt>checksumAlgorithm</tt> is used for calculate and compare file checksums. Both
	 * are used if a local {@link File} is compared to a {@link FileVersion}.
	 * 
	 * @param rootFolder Base folder to determine a relative path to 
	 * @param checksumAlgorithm Digest algorithm for checksum calculation, e.g. "SHA1" or "MD5"
	 */
	public FileVersionComparator(File rootFolder, String checksumAlgorithm) {
		this.rootFolder = rootFolder;
		this.checksumAlgorithm = checksumAlgorithm;
	}

	/**
	 * Compares two {@link FileVersion}s to each other and returns a {@link FileVersionComparison} object.
	 * 
	 * @param expectedFileVersion The expected file version (that is compared to the actual file version)
	 * @param actualFileVersion The actual file version (that is compared to the expected file version)
	 * @return Returns a file version comparison object, indicating if there are differences between the file versions
	 */
	public FileVersionComparison compare(FileVersion expectedFileVersion, FileVersion actualFileVersion) {
		FileProperties expectedFileProperties = captureFileProperties(expectedFileVersion);
		FileProperties actualFileProperties = captureFileProperties(actualFileVersion);

		return compare(expectedFileProperties, actualFileProperties, true);
	}

	/**
	 * Compares a {@link FileVersion} with a local {@link File} and returns a {@link FileVersionComparison} object.
	 * 
	 * <p>If the actual file does not differ in size, it is necessary to calculate and compare the checksum of the
	 * local file to the file version to reliably determine if it has changed. Unless comparing the size and last 
	 * modified date is enough, the <tt>actualFileForceChecksum</tt> parameter must be switched to <tt>true</tt>.  
	 * 
	 * @param expectedFileVersion The expected file version (that is compared to the actual file)
	 * @param actualFile The actual file (that is compared to the expected file version)
	 * @param actualFileForceChecksum Force a checksum comparison if necessary (if size does not differ)
	 * @return Returns a file version comparison object, indicating if there are differences between the file versions
	 */
	public FileVersionComparison compare(FileVersion expectedFileVersion, File actualFile, boolean actualFileForceChecksum) {
		return compare(expectedFileVersion, actualFile, null, actualFileForceChecksum);
	}

	/**
	 * Compares a {@link FileVersion} with a local {@link File} and returns a {@link FileVersionComparison} object.
	 * 
	 * <p>If the actual file does not differ in size, it is necessary to calculate and compare the checksum of the
	 * local file to the file version to reliably determine if it has changed. Unless comparing the size and last 
	 * modified date is enough, the <tt>actualFileForceChecksum</tt> parameter must be switched to <tt>true</tt>.  
	 * 
	 * <p>If the <tt>actualFileKnownChecksum</tt> parameter is set and a checksum comparison is necessary, this
	 * parameter is used to compare checksums. If not and force checksum is enabled, the checksum is calculated 
	 * and compared.
	 * 
	 * @param expectedFileVersion The expected file version (that is compared to the actual file)
	 * @param actualFile The actual file (that is compared to the expected file version)
	 * @param actualFileKnownChecksum If the checksum of the local file is known, it can be set 
	 * @param actualFileForceChecksum Force a checksum comparison if necessary (if size does not differ)
	 * @return Returns a file version comparison object, indicating if there are differences between the file versions
	 */
	public FileVersionComparison compare(FileVersion expectedLocalFileVersion, File actualLocalFile, FileChecksum actualFileKnownChecksum,
			boolean actualFileForceChecksum) {

		FileProperties expectedLocalFileVersionProperties = captureFileProperties(expectedLocalFileVersion);
		FileProperties actualFileProperties = captureFileProperties(actualLocalFile, actualFileKnownChecksum, actualFileForceChecksum);

		return compare(expectedLocalFileVersionProperties, actualFileProperties, actualFileForceChecksum);
	}

	public FileVersionComparison compare(FileProperties expectedFileProperties, FileProperties actualFileProperties, boolean compareChecksums) {
		FileVersionComparison fileComparison = new FileVersionComparison();

		fileComparison.fileChanges = new HashSet<FileChange>();
		fileComparison.expectedFileProperties = expectedFileProperties;
		fileComparison.actualFileProperties = actualFileProperties;

		boolean cancelFurtherTests = performCancellingTests(fileComparison);

		if (cancelFurtherTests) {
			return fileComparison;
		}

		switch (actualFileProperties.getType()) {
		case FILE:
			compareFile(fileComparison, compareChecksums);
			break;

		case FOLDER:
			compareFolder(fileComparison);
			break;

		case SYMLINK:
			compareSymlink(fileComparison);
			break;

		default:
			throw new RuntimeException("This should not happen. Unknown file type: " + actualFileProperties.getType());
		}

		return fileComparison;
	}

	private void compareSymlink(FileVersionComparison fileComparison) {
		// comparePath(fileComparison);
		compareSymlinkTarget(fileComparison);
	}

	private void compareFolder(FileVersionComparison fileComparison) {
		// comparePath(fileComparison);
		compareAttributes(fileComparison);
	}

	private void compareFile(FileVersionComparison fileComparison, boolean compareChecksums) {
		comparePath(fileComparison);
		compareModifiedDate(fileComparison);
		compareSize(fileComparison);
		compareAttributes(fileComparison);

		// Check if checksum comparison necessary
		if (fileComparison.getFileChanges().contains(FileChange.CHANGED_SIZE)) {
			fileComparison.fileChanges.add(FileChange.CHANGED_CHECKSUM);
		}
		else if (compareChecksums) {
			compareChecksum(fileComparison);
		}
	}

	private void compareChecksum(FileVersionComparison fileComparison) {
		boolean isChecksumEqual = FileChecksum.fileChecksumEquals(fileComparison.expectedFileProperties.getChecksum(),
				fileComparison.actualFileProperties.getChecksum());

		if (!isChecksumEqual) {
			fileComparison.fileChanges.add(FileChange.CHANGED_CHECKSUM);

			logger.log(Level.INFO, "     - " + fileComparison.fileChanges
					+ ": Local file DIFFERS from file version, expected CHECKSUM = {0}, but actual CHECKSUM = {1}, for file {2}",
					new Object[] { fileComparison.expectedFileProperties.checksum, fileComparison.actualFileProperties.checksum,
							fileComparison.actualFileProperties.getRelativePath() });
		}
	}

	private void compareSymlinkTarget(FileVersionComparison fileComparison) {
		boolean linkTargetsIdentical = fileComparison.expectedFileProperties.getLinkTarget() != null
				&& fileComparison.expectedFileProperties.getLinkTarget().equals(fileComparison.actualFileProperties.getLinkTarget());

		if (!linkTargetsIdentical) {
			fileComparison.fileChanges.add(FileChange.CHANGED_LINK_TARGET);

			logger.log(Level.INFO, "     - " + fileComparison.fileChanges
					+ ": Local file DIFFERS from file version, expected LINK TARGET = {0}, but actual LINK TARGET = {1}, for file {2}", new Object[] {
					fileComparison.actualFileProperties.getLinkTarget(), fileComparison.expectedFileProperties.getLinkTarget(),
					fileComparison.actualFileProperties.getRelativePath() });
		}
	}

	private void compareAttributes(FileVersionComparison fileComparison) {
		if (EnvironmentUtil.isWindows()) {
			compareDosAttributes(fileComparison);
		}
		else if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			comparePosixPermissions(fileComparison);
		}
	}

	private void comparePosixPermissions(FileVersionComparison fileComparison) {
		boolean posixPermsDiffer = false;

		boolean actualIsNull = fileComparison.actualFileProperties == null || fileComparison.actualFileProperties.getPosixPermissions() == null;
		boolean expectedIsNull = fileComparison.expectedFileProperties == null || fileComparison.expectedFileProperties.getPosixPermissions() == null;
				
		if (!actualIsNull && !expectedIsNull) {
			if (!fileComparison.actualFileProperties.getPosixPermissions().equals(fileComparison.expectedFileProperties.getPosixPermissions())) {
				posixPermsDiffer = true;
			}
		}
		else if ((actualIsNull && !expectedIsNull) || (!actualIsNull && expectedIsNull)) {
			posixPermsDiffer = true;
		}
		
		if (posixPermsDiffer) {
			fileComparison.fileChanges.add(FileChange.CHANGED_ATTRIBUTES);

			logger.log(Level.INFO, "     - " + fileComparison.fileChanges
					+ ": Local file DIFFERS from file version, expected POSIX ATTRS = {0}, but actual POSIX ATTRS = {1}, for file {2}", new Object[] {
					fileComparison.expectedFileProperties.getPosixPermissions(), fileComparison.actualFileProperties.getPosixPermissions(),
					fileComparison.actualFileProperties.getRelativePath() });
		}
	}

	private void compareDosAttributes(FileVersionComparison fileComparison) {
		boolean dosAttrsDiffer = false;

		boolean actualIsNull = fileComparison.actualFileProperties == null || fileComparison.actualFileProperties.getDosAttributes() == null;
		boolean expectedIsNull = fileComparison.expectedFileProperties == null || fileComparison.expectedFileProperties.getDosAttributes() == null;
			
		if (!actualIsNull && !expectedIsNull) {
			if (!fileComparison.actualFileProperties.getDosAttributes().equals(fileComparison.expectedFileProperties.getDosAttributes())) {
				dosAttrsDiffer = true;
			}
		}
		else if ((actualIsNull && !expectedIsNull) || (!actualIsNull && expectedIsNull)) {
			dosAttrsDiffer = true;
		}
		
		if (dosAttrsDiffer) {
			fileComparison.fileChanges.add(FileChange.CHANGED_ATTRIBUTES);

			logger.log(Level.INFO, "     - " + fileComparison.fileChanges
					+ ": Local file DIFFERS from file version, expected DOS ATTRS = {0}, but actual DOS ATTRS = {1}, for file {2}", new Object[] {
					fileComparison.expectedFileProperties.getDosAttributes(), fileComparison.actualFileProperties.getDosAttributes(),
					fileComparison.actualFileProperties.getRelativePath() });
		}
	}

	private void compareSize(FileVersionComparison fileComparison) {
		if (fileComparison.expectedFileProperties.getSize() != fileComparison.actualFileProperties.getSize()) {
			fileComparison.fileChanges.add(FileChange.CHANGED_SIZE);

			logger.log(Level.INFO, "     - " + fileComparison.fileChanges
					+ ": Local file DIFFERS from file version, expected SIZE = {0}, but actual SIZE = {1}, for file {2}", new Object[] {
					fileComparison.expectedFileProperties.getSize(), fileComparison.actualFileProperties.getSize(),
					fileComparison.actualFileProperties.getRelativePath() });
		}
	}

	private void compareModifiedDate(FileVersionComparison fileComparison) {
		if (fileComparison.expectedFileProperties.getLastModified() != fileComparison.actualFileProperties.getLastModified()) {
			fileComparison.fileChanges.add(FileChange.CHANGED_LAST_MOD_DATE);

			logger.log(Level.INFO, "     - " + fileComparison.fileChanges
					+ ": Local file DIFFERS from file version, expected MOD. DATE = {0}, but actual MOD. DATE = {1}, for file {2}", new Object[] {
					new Date(fileComparison.expectedFileProperties.getLastModified()), new Date(fileComparison.actualFileProperties.getLastModified()),
					fileComparison.actualFileProperties.getRelativePath() });
		}
	}

	private void comparePath(FileVersionComparison fileComparison) {
		if (!fileComparison.expectedFileProperties.getRelativePath().equals(fileComparison.actualFileProperties.getRelativePath())) {
			fileComparison.fileChanges.add(FileChange.CHANGED_PATH);

			logger.log(Level.INFO, "     - " + fileComparison.fileChanges
					+ ": Local file DIFFERS from file version, expected PATH = {0}, but actual PATH = {1}, for file {2}", new Object[] {
					fileComparison.expectedFileProperties.getRelativePath(), fileComparison.actualFileProperties.getRelativePath(),
					fileComparison.actualFileProperties.getRelativePath() });
		}
	}

	private boolean performCancellingTests(FileVersionComparison fileComparison) {
		// Check null
		if (fileComparison.actualFileProperties == null && fileComparison.expectedFileProperties == null) {
			throw new RuntimeException("actualFileProperties and expectedFileProperties cannot be null.");
		}
		else if (fileComparison.actualFileProperties != null && fileComparison.expectedFileProperties == null) {
			throw new RuntimeException("expectedFileProperties cannot be null.");			
		}
		else if (fileComparison.actualFileProperties == null && fileComparison.expectedFileProperties != null) {
			if (!fileComparison.expectedFileProperties.exists()) {
				logger.log(Level.INFO, "     - " + fileComparison.fileChanges
						+ ": Local file does not exist, and expected file was deleted, for file {0}",
						new Object[] { fileComparison.expectedFileProperties.getRelativePath() });

				return true;
			}
			else {
				fileComparison.fileChanges.add(FileChange.DELETED);

				logger.log(Level.INFO, "     - " + fileComparison.fileChanges
						+ ": Local file DIFFERS from file version, actual file is NULL, for file {0}",
						new Object[] { fileComparison.expectedFileProperties.getRelativePath() });

				return true;
			}
		}

		// Check existence
		if (fileComparison.expectedFileProperties.exists() != fileComparison.actualFileProperties.exists()) {
			// File is expected to exist, but it does NOT --> file has been deleted
			if (fileComparison.expectedFileProperties.exists() && !fileComparison.actualFileProperties.exists()) {
				fileComparison.fileChanges.add(FileChange.DELETED);
			}

			// File is expected to NOT exist, but it does --> file is new
			else {
				fileComparison.fileChanges.add(FileChange.NEW);
			}

			logger.log(Level.INFO, "     - " + fileComparison.fileChanges
					+ ": Local file DIFFERS from file version, expected EXISTS = {0}, but actual EXISTS = {1}, for file {2}",
					new Object[] { fileComparison.expectedFileProperties.exists(), fileComparison.actualFileProperties.exists(),
							fileComparison.actualFileProperties.getRelativePath() });

			return true;
		}
		else if (!fileComparison.expectedFileProperties.exists() && !fileComparison.actualFileProperties.exists()) {
			logger.log(Level.INFO, "     - " + fileComparison.fileChanges
					+ ": Local file does not exist, and expected file was deleted, for file {0}",
					new Object[] { fileComparison.expectedFileProperties.getRelativePath() });

			return true;
		}

		// Check file type (folder/file)
		if (!fileComparison.expectedFileProperties.getType().equals(fileComparison.actualFileProperties.getType())) {
			fileComparison.fileChanges.add(FileChange.DELETED);

			logger.log(Level.INFO, "     - " + fileComparison.fileChanges
					+ ": Local file DIFFERS from file version, expected TYPE = {0}, but actual TYPE = {1}, for file {2}", new Object[] {
					fileComparison.expectedFileProperties.getType(), fileComparison.actualFileProperties.getType(),
					fileComparison.actualFileProperties.getRelativePath() });

			return true;
		}

		return false;
	}

	public FileProperties captureFileProperties(File file, FileChecksum knownChecksum, boolean forceChecksum) {
		FileProperties fileProperties = new FileProperties();
		fileProperties.relativePath = FileUtil.getRelativeDatabasePath(rootFolder, file);

		Path filePath = null;
		
		try {
			filePath = Paths.get(file.getAbsolutePath());
			fileProperties.exists = Files.exists(filePath, LinkOption.NOFOLLOW_LINKS);
		}
		catch (InvalidPathException e) {
			// This throws an exception if the filename is invalid,
			// e.g. colon in filename on windows "file:name"
			
			logger.log(Level.WARNING, "- Path '{0}' is invalid on this file system. It cannot exist. ", file.getAbsolutePath());
			
			fileProperties.exists = false;
			return fileProperties;
		}

		if (!fileProperties.exists) {
			return fileProperties;
		}

		try {
			// Read operating system dependent file attributes
			BasicFileAttributes fileAttributes = null;

			if (EnvironmentUtil.isWindows()) {
				DosFileAttributes dosAttrs = Files.readAttributes(filePath, DosFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
				fileProperties.dosAttributes = FileUtil.dosAttrsToString(dosAttrs);

				fileAttributes = dosAttrs;
			}
			else if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
				PosixFileAttributes posixAttrs = Files.readAttributes(filePath, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
				fileProperties.posixPermissions = PosixFilePermissions.toString(posixAttrs.permissions());

				fileAttributes = posixAttrs;
			}
			else {
				fileAttributes = Files.readAttributes(filePath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
			}

			fileProperties.lastModified = fileAttributes.lastModifiedTime().toMillis();
			fileProperties.size = fileAttributes.size();

			// Type
			if (fileAttributes.isSymbolicLink()) {
				fileProperties.type = FileType.SYMLINK;
				fileProperties.linkTarget = FileUtil.readSymlinkTarget(file);
			}
			else if (fileAttributes.isDirectory()) {
				fileProperties.type = FileType.FOLDER;
				fileProperties.linkTarget = null;
			}
			else {
				fileProperties.type = FileType.FILE;
				fileProperties.linkTarget = null;
			}

			// Checksum
			if (knownChecksum != null) {
				fileProperties.checksum = knownChecksum;
			}
			else {
				if (fileProperties.type == FileType.FILE && forceChecksum) {
					try {
						if (fileProperties.size > 0) {
							fileProperties.checksum = new FileChecksum(FileUtil.createChecksum(file, checksumAlgorithm));
						}
						else {
							fileProperties.checksum = null;
						}
					}
					catch (Exception e) {
						logger.log(Level.SEVERE, "SEVERE: Unable to create checksum for file {0}", file);
						fileProperties.checksum = null;
					}
				}
				else {
					fileProperties.checksum = null;
				}
			}

			// Must be last (!), used for vanish-test later
			fileProperties.exists = Files.exists(filePath, LinkOption.NOFOLLOW_LINKS);
			fileProperties.locked = fileProperties.exists && FileUtil.isFileLocked(file);

			return fileProperties;
		}
		catch (IOException e) {
			logger.log(Level.SEVERE, "SEVERE: Cannot read file {0}. Assuming file is locked.", file);

			fileProperties.exists = true;
			fileProperties.locked = true;

			return fileProperties;
		}
	}

	public FileProperties captureFileProperties(FileVersion fileVersion) {
		if (fileVersion == null) {
			return null;
		}

		FileProperties fileProperties = new FileProperties();

		fileProperties.lastModified = fileVersion.getLastModified().getTime();
		fileProperties.size = fileVersion.getSize();
		fileProperties.relativePath = fileVersion.getPath();
		fileProperties.linkTarget = fileVersion.getLinkTarget();
		fileProperties.checksum = fileVersion.getChecksum();
		fileProperties.type = fileVersion.getType();
		fileProperties.posixPermissions = fileVersion.getPosixPermissions();
		fileProperties.dosAttributes = fileVersion.getDosAttributes();
		fileProperties.exists = fileVersion.getStatus() != FileStatus.DELETED;
		fileProperties.locked = false;

		return fileProperties;
	}

	public static class FileVersionComparison {
		private Set<FileChange> fileChanges = new HashSet<FileChange>();
		private FileProperties actualFileProperties;
		private FileProperties expectedFileProperties;

		public boolean equals() {
			return fileChanges.size() == 0;
		}

		public Set<FileChange> getFileChanges() {
			return fileChanges;
		}

		public FileProperties getActualFileProperties() {
			return actualFileProperties;
		}

		public FileProperties getExpectedFileProperties() {
			return expectedFileProperties;
		}
	}

	public static enum FileChange {
		NEW, CHANGED_CHECKSUM, CHANGED_ATTRIBUTES, CHANGED_LAST_MOD_DATE, CHANGED_LINK_TARGET, CHANGED_SIZE, CHANGED_PATH, DELETED,
	}

	public static class FileProperties {
		private long lastModified = -1;
		private FileType type = null;
		private long size = -1;
		private String relativePath;
		private String linkTarget;
		private FileChecksum checksum = null;
		private boolean locked = true;
		private boolean exists = false;

		private String posixPermissions = null;
		private String dosAttributes = null;

		public long getLastModified() {
			return lastModified;
		}

		public FileType getType() {
			return type;
		}

		public long getSize() {
			return size;
		}

		public String getRelativePath() {
			return relativePath;
		}

		public String getLinkTarget() {
			return linkTarget;
		}

		public FileChecksum getChecksum() {
			return checksum;
		}

		public boolean isLocked() {
			return locked;
		}

		public boolean exists() {
			return exists;
		}

		public String getPosixPermissions() {
			return posixPermissions;
		}

		public String getDosAttributes() {
			return dosAttributes;
		}
	}
}
