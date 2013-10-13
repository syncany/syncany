package org.syncany.database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class FileVersionHelper {
	private static final Logger logger = Logger.getLogger(FileVersionHelper.class.getSimpleName());
	private Config config;	
	
	public FileVersionHelper(Config config) {
		this.config = config;
	}
	
	public FileVersionComparison compare(FileVersion expectedLocalFileVersion, FileVersion actualLocalFileVersion) {
		FileProperties expectedFileProperties = captureFileVersionProperties(expectedLocalFileVersion);
		FileProperties actualFileProperties = captureFileVersionProperties(actualLocalFileVersion);
		 
		return compare(expectedFileProperties, actualFileProperties, true);
	}
	
	public FileVersionComparison compare(FileVersion expectedLocalFileVersion, File actualLocalFile, boolean forceChecksum) {
		return compare(expectedLocalFileVersion, actualLocalFile, null, forceChecksum);
	}
	
	public FileVersionComparison compare(FileVersion expectedLocalFileVersion, File actualLocalFile, byte[] knownChecksum, boolean forceChecksum) {
		FileProperties expectedLocalFileVersionProperties = captureFileVersionProperties(expectedLocalFileVersion);
		FileProperties actualFileProperties= captureFileProperties(actualLocalFile, knownChecksum, forceChecksum);
		
		return compare(expectedLocalFileVersionProperties, actualFileProperties, forceChecksum);
	}
	
	public FileVersionComparison compare(FileProperties expectedFileProperties, FileProperties actualFileProperties, boolean compareChecksums) {
		FileVersionComparison fileComparison = new FileVersionComparison();
		
		fileComparison.fileChanges = new HashSet<FileChange>();
		fileComparison.expectedFileProperties = expectedFileProperties;
		fileComparison.actualFileProperties = actualFileProperties;
			
		performCancellingTests(fileComparison);
		
		if (!fileComparison.equals()) {
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
				throw new RuntimeException("This should not happen. Unknown file type: "+actualFileProperties.getType());
		}	
		
		return fileComparison;
	}
	
	private void compareSymlink(FileVersionComparison fileComparison) {
		//comparePath(fileComparison);
		compareSymlinkTarget(fileComparison);
	}

	private void compareFolder(FileVersionComparison fileComparison) {
		//comparePath(fileComparison);
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
		boolean isChecksumEqual = Arrays.equals(fileComparison.expectedFileProperties.getChecksum(), fileComparison.actualFileProperties.getChecksum());
		
		if (!isChecksumEqual) {
			fileComparison.fileChanges.add(FileChange.CHANGED_CHECKSUM);
			
			logger.log(Level.INFO, "     - "+fileComparison.fileChanges+": Local file DIFFERS from file version, expected CHECKSUM = {0}, but actual CHECKSUM = {1}, for file {2}", 
					new Object[] { StringUtil.toHex(fileComparison.expectedFileProperties.checksum), StringUtil.toHex(fileComparison.actualFileProperties.checksum), fileComparison.actualFileProperties.getRelativePath() });
		}
	}

	private void compareSymlinkTarget(FileVersionComparison fileComparison) {
		boolean linkTargetsIdentical = fileComparison.expectedFileProperties.getLinkTarget() != null
				&& fileComparison.expectedFileProperties.getLinkTarget().equals(fileComparison.actualFileProperties.getLinkTarget());
		
		if (!linkTargetsIdentical) {
			fileComparison.fileChanges.add(FileChange.CHANGED_LINK_TARGET);
			
			logger.log(Level.INFO, "     - "+fileComparison.fileChanges+": Local file DIFFERS from file version, expected LINK TARGET = {0}, but actual LINK TARGET = {1}, for file {2}", 
					new Object[] { fileComparison.actualFileProperties.getLinkTarget(), fileComparison.expectedFileProperties.getLinkTarget(), fileComparison.actualFileProperties.getRelativePath() });
		}				
	}
	
	private void compareAttributes(FileVersionComparison fileComparison) {
		if (FileUtil.isWindows()) {
			compareDosAttributes(fileComparison);			
		}
		else if (FileUtil.isUnixLikeOperatingSystem()) {
			comparePosixPermissions(fileComparison);					
		}	
	}

	private void comparePosixPermissions(FileVersionComparison fileComparison) {
		if (fileComparison.actualFileProperties.getPosixPermissions() != null 
				&& !fileComparison.actualFileProperties.getPosixPermissions().equals(fileComparison.expectedFileProperties.getPosixPermissions())) {
			
			fileComparison.fileChanges.add(FileChange.CHANGED_ATTRIBUTES);
			
			logger.log(Level.INFO, "     - "+fileComparison.fileChanges+": Local file DIFFERS from file version, expected POSIX ATTRS = {0}, but actual POSIX ATTRS = {1}, for file {2}", 
					new Object[] { fileComparison.actualFileProperties.getPosixPermissions(), fileComparison.expectedFileProperties.getPosixPermissions(), fileComparison.actualFileProperties.getRelativePath() });
		}	
	}

	private void compareDosAttributes(FileVersionComparison fileComparison) {
		if (fileComparison.actualFileProperties.getDosAttributes() != null 
				&& !fileComparison.actualFileProperties.getDosAttributes().equals(fileComparison.expectedFileProperties.getDosAttributes())) {
			
			fileComparison.fileChanges.add(FileChange.CHANGED_ATTRIBUTES);
			
			logger.log(Level.INFO, "     - "+fileComparison.fileChanges+": Local file DIFFERS from file version, expected DOS ATTRS = {0}, but actual DOS ATTRS = {1}, for file {2}", 
					new Object[] { fileComparison.actualFileProperties.getDosAttributes(), fileComparison.expectedFileProperties.getDosAttributes(), fileComparison.actualFileProperties.getRelativePath() });
		}
	}

	private void compareSize(FileVersionComparison fileComparison) {
		if (fileComparison.expectedFileProperties.getSize() != fileComparison.actualFileProperties.getSize()) {
			fileComparison.fileChanges.add(FileChange.CHANGED_SIZE);			

			logger.log(Level.INFO, "     - "+fileComparison.fileChanges+": Local file DIFFERS from file version, expected SIZE = {0}, but actual SIZE = {1}, for file {2}", 
					new Object[] { fileComparison.actualFileProperties.getSize(), fileComparison.expectedFileProperties.getSize(), fileComparison.actualFileProperties.getRelativePath() });			
		}	
	}

	private void compareModifiedDate(FileVersionComparison fileComparison) {
		if (fileComparison.expectedFileProperties.getLastModified() != fileComparison.actualFileProperties.getLastModified()) {			
			fileComparison.fileChanges.add(FileChange.CHANGED_LAST_MOD_DATE);			

			logger.log(Level.INFO, "     - "+fileComparison.fileChanges+": Local file DIFFERS from file version, expected MOD. DATE = {0}, but actual MOD. DATE = {1}, for file {2}", 
					new Object[] { new Date(fileComparison.actualFileProperties.getLastModified()), new Date(fileComparison.expectedFileProperties.getLastModified()), fileComparison.actualFileProperties.getRelativePath() });			
		}		
	}

	private void comparePath(FileVersionComparison fileComparison) {
		if (!fileComparison.expectedFileProperties.getRelativePath().equals(fileComparison.actualFileProperties.getRelativePath())) {			
			fileComparison.fileChanges.add(FileChange.CHANGED_PATH);
			
			logger.log(Level.INFO, "     - "+fileComparison.fileChanges+": Local file DIFFERS from file version, expected PATH = {0}, but actual PATH = {1}, for file {2}", 
					new Object[] { fileComparison.expectedFileProperties.getRelativePath(), fileComparison.actualFileProperties.getRelativePath(), fileComparison.actualFileProperties.getRelativePath() });
		}				
	}
	
	private FileVersionComparison performCancellingTests(FileVersionComparison fileComparison) {
		// Check null
		if (fileComparison.actualFileProperties == null) {
			fileComparison.fileChanges.add(FileChange.DELETED);
			
			logger.log(Level.INFO, "     - "+fileComparison.fileChanges+": Local file DIFFERS from file version, actual file is NULL, for file {0}", 
					new Object[] { fileComparison.expectedFileProperties.getRelativePath() });
			
			return fileComparison;
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

			logger.log(Level.INFO, "     - "+fileComparison.fileChanges+": Local file DIFFERS from file version, expected EXISTS = {0}, but actual EXISTS = {1}, for file {2}", 
					new Object[] { fileComparison.actualFileProperties.exists(), fileComparison.expectedFileProperties.exists(), fileComparison.actualFileProperties.getRelativePath() });
						
			return fileComparison;
		}
		
		// Check file type (folder/file)
		if (!fileComparison.expectedFileProperties.getType().equals(fileComparison.actualFileProperties.getType())) {			
			fileComparison.fileChanges.add(FileChange.DELETED);
			
			logger.log(Level.INFO, "     - "+fileComparison.fileChanges+": Local file DIFFERS from file version, expected TYPE = {0}, but actual TYPE = {1}, for file {2}", 
					new Object[] { fileComparison.actualFileProperties.getType(), fileComparison.expectedFileProperties.getType(), fileComparison.actualFileProperties.getRelativePath() });
			
			return fileComparison;
		}	
		
		return fileComparison;
	}

	public FileProperties captureFileProperties(File file, byte[] knownChecksum, boolean forceChecksum) {
		Path filePath = Paths.get(file.getAbsolutePath());

		FileProperties fileProperties = new FileProperties();
		fileProperties.relativePath = FileUtil.getRelativePath(config.getLocalDir(), file);
		fileProperties.exists = Files.exists(filePath, LinkOption.NOFOLLOW_LINKS);
		
		if (!fileProperties.exists) {
			return fileProperties;
		}
		
		try {
			// Read operating system dependent file attributes 
			BasicFileAttributes fileAttributes = null;
			
			if (FileUtil.isWindows()) {
				DosFileAttributes dosAttrs = Files.readAttributes(filePath, DosFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
				fileProperties.dosAttributes = FileUtil.dosAttrsToString(dosAttrs);					

				fileAttributes = dosAttrs;
			}
			else if (FileUtil.isUnixLikeOperatingSystem()) {
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
							String checksumAlgorithm = config.getChunker().getChecksumAlgorithm();
							fileProperties.checksum = FileUtil.createChecksum(file, checksumAlgorithm);
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
	
	private FileProperties captureFileVersionProperties(FileVersion fileVersion) {
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
		
		public FileProperties getDatabaseFileProperties() {
			return actualFileProperties;
		}
		
		public FileProperties getLocalFileProperties() {
			return expectedFileProperties;
		}				
	}
	
	public static enum FileChange {		
		NEW, 
		CHANGED_CHECKSUM,
		CHANGED_ATTRIBUTES,
		CHANGED_LAST_MOD_DATE,
		CHANGED_LINK_TARGET,
		CHANGED_SIZE,
		CHANGED_PATH,
		DELETED, 		
	}

	public static class FileProperties {
		private long lastModified = -1;
		private FileType type = null;
		private long size = -1;
		private String relativePath;
		private String linkTarget;
		private byte[] checksum = null;
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
		
		public byte[] getChecksum() {
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
