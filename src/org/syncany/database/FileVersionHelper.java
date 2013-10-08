package org.syncany.database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
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
	
	public FileVersionComparison compare(FileVersion expectedLocalFileVersion, File actualLocalFile, boolean forceChecksum) {
		return compare(expectedLocalFileVersion, actualLocalFile, null, forceChecksum);
	}
	
	public FileVersionComparison compare(FileVersion expectedLocalFileVersion, File actualLocalFile, byte[] knownChecksum, boolean forceChecksum) {
		FileVersionComparison fileComparison = new FileVersionComparison();
		
		fileComparison.equals = false;
		fileComparison.localFile = actualLocalFile;
		fileComparison.databaseFile = expectedLocalFileVersion;
		fileComparison.localFileProperties = captureFileProperties(actualLocalFile, knownChecksum, forceChecksum);
		fileComparison.databaseFileProperties = captureFileVersionProperties(expectedLocalFileVersion);
			
		// Check existence
		if (fileComparison.localFileProperties.exists != fileComparison.databaseFileProperties.exists) {
			logger.log(Level.INFO, "     - Local file DIFFERS from file version, expected EXISTS = {0}, but actual EXISTS = {1}, for file {2}", 
					new Object[] { fileComparison.databaseFileProperties.exists, fileComparison.localFileProperties.exists, actualLocalFile });
			
			return fileComparison;
		}
		
		// Check file type (folder/file)
		if (fileComparison.localFileProperties.type != fileComparison.databaseFileProperties.type) {			
			logger.log(Level.INFO, "     - Local file DIFFERS from file version, expected TYPE = {0}, but actual TYPE = {1}, for file {2}", 
					new Object[] { fileComparison.databaseFileProperties.type, fileComparison.localFileProperties.type, actualLocalFile });
			
			return fileComparison;
		}
		
		// Check folder
		if (fileComparison.localFileProperties.type == FileType.FOLDER) {
			logger.log(Level.INFO, "     - Local file matches file version, directory {0}", new Object[] { actualLocalFile });

			fileComparison.equals = true;
			return fileComparison;
		}
		
		// Check symlink
		if (fileComparison.localFileProperties.type == FileType.SYMLINK) {
			boolean linkTargetsIdentical = fileComparison.localFileProperties.linkTarget != null
					&& fileComparison.localFileProperties.linkTarget.equals(fileComparison.databaseFileProperties.linkTarget);
			
			if (!linkTargetsIdentical) {
				logger.log(Level.INFO, "     - Local file DIFFERS from file version, expected LINK TARGET = {0}, but actual LINK TARGET = {1}, for file {2}", 
						new Object[] { fileComparison.databaseFileProperties.linkTarget, fileComparison.localFileProperties.linkTarget, actualLocalFile });
				
				return fileComparison;
			}
			else {
				logger.log(Level.INFO, "     - Local file matches file version, directory {0}", new Object[] { actualLocalFile });

				fileComparison.equals = true;
				return fileComparison;
			}
		}
		
		// Check modified date
		if (fileComparison.localFileProperties.lastModified != fileComparison.databaseFileProperties.lastModified) {			
			logger.log(Level.INFO, "     - Local file DIFFERS from file version, expected MOD. DATE = {0}, but actual MOD. DATE = {1}, for file {2}", 
					new Object[] { fileComparison.databaseFileProperties.lastModified, fileComparison.localFileProperties.lastModified, actualLocalFile });
			
			return fileComparison;
		}
		
		// Check size	
		if (fileComparison.localFileProperties.size != fileComparison.databaseFileProperties.size) {			
			logger.log(Level.INFO, "     - Local file DIFFERS from file version, expected SIZE = {0}, but actual SIZE = {1}, for file {2}", 
					new Object[] { fileComparison.databaseFileProperties.size, fileComparison.localFileProperties.size, actualLocalFile });
			
			return fileComparison;
		}	
		
		// Check size (0-byte files, no checksum check necessary)
		if (fileComparison.localFileProperties.size == 0 && fileComparison.databaseFileProperties.size == 0) {			
			logger.log(Level.INFO, "     - Local file matches file version, 0-byte file {0}", new Object[] { actualLocalFile });

			fileComparison.equals = true;
			return fileComparison;
		}	
				
		// Do not check checksum
		if (!forceChecksum) {
			logger.log(Level.INFO, "     - Local file matches file version (checksum SKIPPED), file {0}", new Object[] { actualLocalFile });
			
			fileComparison.equals = true;
			return fileComparison;			
		}
		
		// Check checksum		
		if (fileComparison.localFileProperties.checksum == null || fileComparison.databaseFileProperties.checksum == null) {
			logger.log(Level.SEVERE, "     - Local file DIFFERS or at least that is what we are guessing here, file {0}", new Object[] { actualLocalFile });
			logger.log(Level.SEVERE, "        ---> If checksum checks are enabled, there should be no case in which checksums are null. The if-statements above must have missed a case.");
			logger.log(Level.SEVERE, "        ---> Assuming file has changed now!");
			
			return fileComparison;	
		}
		
		try {			 
			boolean isChecksumEqual = Arrays.equals(fileComparison.localFileProperties.checksum, fileComparison.databaseFileProperties.checksum);
			
			if (isChecksumEqual) {
				logger.log(Level.INFO, "     - Local file matches file version (checksum!), file {0}", new Object[] { actualLocalFile });

				fileComparison.equals = true;
				return fileComparison;	
			}
			else {
				logger.log(Level.INFO, "     - Local file DIFFERS from file version, expected CHECKSUM = {0}, but actual CHECKSUM = {1}, for file {2}", 
						new Object[] { StringUtil.toHex(fileComparison.databaseFileProperties.checksum), StringUtil.toHex(fileComparison.localFileProperties.checksum), actualLocalFile });

				return fileComparison;
			}
		}
		catch (Exception e) {
			logger.log(Level.INFO, "     - Unexpected behavior: Unable to create checksum for local file, assuming differs: "+actualLocalFile);
			return fileComparison;
		}
			
	}

	public FileProperties captureFileProperties(File file, byte[] knownChecksum, boolean forceChecksum) {
		FileProperties fileProperties = new FileProperties();
		fileProperties.relativePath = FileUtil.getRelativePath(config.getLocalDir(), file);
		
		try {
			BasicFileAttributes fileAttributes = Files.readAttributes(Paths.get(file.getAbsolutePath()), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
						
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
						String checksumAlgorithm = config.getChunker().getChecksumAlgorithm();
						fileProperties.checksum = FileUtil.createChecksum(file, checksumAlgorithm);
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
			fileProperties.exists = file.exists();
			fileProperties.locked = fileProperties.exists && FileUtil.isFileLocked(file);
			
			return fileProperties;
		}
		catch (IOException e) {
			logger.log(Level.SEVERE, "SEVERE: Cannot read file {0}. Assuming file is locked.", file);
			
			fileProperties.locked = true;
			return fileProperties;
		}
	}
	
	private FileProperties captureFileVersionProperties(FileVersion fileVersion) {
		FileProperties fileProperties = new FileProperties();
		
		fileProperties.lastModified = fileVersion.getLastModified().getTime();
		fileProperties.size = fileVersion.getSize();
		fileProperties.relativePath = fileVersion.getPath();
		fileProperties.linkTarget = fileVersion.getLinkTarget();
		fileProperties.checksum = fileVersion.getChecksum();
		fileProperties.type = fileVersion.getType();
		fileProperties.exists = fileVersion.getStatus() != FileStatus.DELETED;
		fileProperties.locked = false;
		
		return fileProperties;
	}
	
	public static class FileVersionComparison {
		private boolean equals;
		private boolean notAvailable;
		private FileVersion databaseFile;
		private File localFile;
		private FileProperties databaseFileProperties;
		private FileProperties localFileProperties;
		
		public boolean equals() {
			return equals;
		}		
		
		public boolean isNotAvailable() {
			return notAvailable;
		}
		
		public FileVersion getDatabaseFile() {
			return databaseFile;
		}
		
		public File getLocalFile() {
			return localFile;
		}
		
		public FileProperties getDatabaseFileProperties() {
			return databaseFileProperties;
		}
		
		public FileProperties getLocalFileProperties() {
			return localFileProperties;
		}				
	}

	public static class FileProperties {
		long lastModified;
		FileType type;
		long size;
		String relativePath;
		String linkTarget;
		byte[] checksum;
		boolean locked;
		boolean exists;		
		
		public boolean exists() {
			return exists;
		}
		
		public boolean isLocked() {
			return locked;
		}
		
		public long getLastModified() {
			return lastModified;
		}
		
		public long getSize() {
			return size;
		}
		
		public FileType getType() {
			return type;
		}
		
		public String getRelativePath() {
			return relativePath;
		}
		
		public byte[] getChecksum() {
			return checksum;
		}		
		
		public String getLinkTarget() {
			return linkTarget;
		}
	}
}
