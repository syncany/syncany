package org.syncany.database;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.syncany.config.Config;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.operations.Indexer;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class FileVersionHelper {
	private static final Logger logger = Logger.getLogger(Indexer.class.getSimpleName());
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
			logger.log(Level.INFO, "     - Local file DIFFERS from database file, expected EXISTS = {0}, but actual EXISTS = {1}, for file {2}", 
					new Object[] { fileComparison.databaseFileProperties.exists, fileComparison.localFileProperties.exists, actualLocalFile });
			
			return fileComparison;
		}
		
		// Check file type (folder/file)
		if (fileComparison.localFileProperties.type != fileComparison.databaseFileProperties.type) {			
			logger.log(Level.INFO, "     - Local file DIFFERS from database file, expected TYPE = {0}, but actual TYPE = {1}, for file {2}", 
					new Object[] { fileComparison.databaseFileProperties.type, fileComparison.localFileProperties.type, actualLocalFile });
			
			return fileComparison;
		}
		
		// Check folder
		if (fileComparison.localFileProperties.type == FileType.FOLDER) {
			logger.log(Level.INFO, "     - Local file matches database file, directory {0}", new Object[] { actualLocalFile });

			fileComparison.equals = true;
			return fileComparison;
		}
		
		// Check modified date
		if (fileComparison.localFileProperties.lastModified != fileComparison.databaseFileProperties.lastModified) {			
			logger.log(Level.INFO, "     - Local file DIFFERS from database file, expected MOD. DATE = {0}, but actual MOD. DATE = {1}, for file {2}", 
					new Object[] { fileComparison.databaseFileProperties.lastModified, fileComparison.localFileProperties.lastModified, actualLocalFile });
			
			return fileComparison;
		}
		
		// Check size	
		if (fileComparison.localFileProperties.size != fileComparison.databaseFileProperties.size) {			
			logger.log(Level.INFO, "     - Local file DIFFERS from database file, expected SIZE = {0}, but actual SIZE = {1}, for file {2}", 
					new Object[] { fileComparison.databaseFileProperties.size, fileComparison.localFileProperties.size, actualLocalFile });
			
			return fileComparison;
		}	
		
		// Check size (0-byte files, no checksum check necessary)
		if (fileComparison.localFileProperties.size == 0 && fileComparison.databaseFileProperties.size == 0) {			
			logger.log(Level.INFO, "     - Local file matches database file, 0-byte file {0}", new Object[] { actualLocalFile });

			fileComparison.equals = true;
			return fileComparison;
		}	
				
		// Do not check checksum
		if (!forceChecksum) {
			logger.log(Level.INFO, "     - Local file matches database file (checksum SKIPPED), file {0}", new Object[] { actualLocalFile });
			
			fileComparison.equals = true;
			return fileComparison;			
		}
		
		// Check checksum		
		try {			 
			boolean isChecksumEqual = Arrays.equals(fileComparison.localFileProperties.checksum, fileComparison.databaseFileProperties.checksum);
			
			if (isChecksumEqual) {
				logger.log(Level.INFO, "     - Local file matches database file (checksum!), file {0}", new Object[] { actualLocalFile });

				fileComparison.equals = true;
				return fileComparison;	
			}
			else {
				logger.log(Level.INFO, "     - Local file DIFFERS from database file, expected CHECKSUM = {0}, but actual CHECKSUM = {1}, for file {2}", 
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
		
		fileProperties.lastModified = file.lastModified();
		fileProperties.size = file.length();
		fileProperties.relativePath = FileUtil.getRelativePath(config.getLocalDir(), file);		
		
		// Type
		try {
			if (file.isDirectory()) {	
				fileProperties.type = FileType.FOLDER;
				fileProperties.linkTarget = null;
			}
			else if (FileUtils.isSymlink(file)) {
				fileProperties.type = FileType.SYMLINK;
				fileProperties.linkTarget = FileUtil.readSymlinkTarget(file);
			}
			else {
				fileProperties.type = FileType.FILE;
				fileProperties.linkTarget = null;
			}
		}
		catch (Exception e) {
			fileProperties.type = FileType.FILE;
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
