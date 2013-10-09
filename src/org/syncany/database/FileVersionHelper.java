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
		if (fileComparison.localFileProperties.exists() != fileComparison.databaseFileProperties.exists()) {			
			logger.log(Level.INFO, "     - Local file DIFFERS from file version, expected EXISTS = {0}, but actual EXISTS = {1}, for file {2}", 
					new Object[] { fileComparison.databaseFileProperties.exists(), fileComparison.localFileProperties.exists(), actualLocalFile });
			
			return fileComparison;
		}
		
		// Check file type (folder/file)
		if (fileComparison.localFileProperties.getType() != null && fileComparison.databaseFileProperties.getType() != null
				&& !fileComparison.localFileProperties.getType().equals(fileComparison.databaseFileProperties.getType())) {
			
			logger.log(Level.INFO, "     - Local file DIFFERS from file version, expected TYPE = {0}, but actual TYPE = {1}, for file {2}", 
					new Object[] { fileComparison.databaseFileProperties.getType(), fileComparison.localFileProperties.getType(), actualLocalFile });
			
			return fileComparison;
		}
		
		// Check folder
		if (fileComparison.localFileProperties.getType() != null && fileComparison.localFileProperties.getType() == FileType.FOLDER) {		
			logger.log(Level.INFO, "     - Local file matches file version, directory {0}", new Object[] { actualLocalFile });

			fileComparison.equals = true;
			return fileComparison;
		}
		
		// Check symlink
		if (fileComparison.localFileProperties.getType() != null && fileComparison.localFileProperties.getType() == FileType.SYMLINK) {			
			boolean linkTargetsIdentical = fileComparison.localFileProperties.getLinkTarget() != null
					&& fileComparison.localFileProperties.getLinkTarget().equals(fileComparison.databaseFileProperties.getLinkTarget());
			
			if (!linkTargetsIdentical) {
				logger.log(Level.INFO, "     - Local file DIFFERS from file version, expected LINK TARGET = {0}, but actual LINK TARGET = {1}, for file {2}", 
						new Object[] { fileComparison.databaseFileProperties.getLinkTarget(), fileComparison.localFileProperties.getLinkTarget(), actualLocalFile });
				
				return fileComparison;
			}
			else {
				logger.log(Level.INFO, "     - Local file matches file version, directory {0}", new Object[] { actualLocalFile });

				fileComparison.equals = true;
				return fileComparison;
			}
		}
		
		// Check modified date
		if (fileComparison.localFileProperties.getLastModified() != fileComparison.databaseFileProperties.getLastModified()) {			
			logger.log(Level.INFO, "     - Local file DIFFERS from file version, expected MOD. DATE = {0}, but actual MOD. DATE = {1}, for file {2}", 
					new Object[] { new Date(fileComparison.databaseFileProperties.getLastModified()), new Date(fileComparison.localFileProperties.getLastModified()), actualLocalFile });
			
			return fileComparison;
		}
		
		// Check size	
		if (fileComparison.localFileProperties.getSize() != fileComparison.databaseFileProperties.getSize()) {			
			logger.log(Level.INFO, "     - Local file DIFFERS from file version, expected SIZE = {0}, but actual SIZE = {1}, for file {2}", 
					new Object[] { fileComparison.databaseFileProperties.getSize(), fileComparison.localFileProperties.getSize(), actualLocalFile });
			
			return fileComparison;
		}	
		
		// Check size (0-byte files, no checksum check necessary)
		if (fileComparison.localFileProperties.getSize() == 0 && fileComparison.databaseFileProperties.getSize() == 0) {			
			logger.log(Level.INFO, "     - Local file matches file version, 0-byte file {0}", new Object[] { actualLocalFile });

			fileComparison.equals = true;
			return fileComparison;
		}	
		
		// Check DOS attributes / POSIX permissions
		if (FileUtil.isWindows()) {
			if (fileComparison.databaseFileProperties.getDosAttributes() != null 
					&& !fileComparison.databaseFileProperties.getDosAttributes().equals(fileComparison.localFileProperties.getDosAttributes())) {
				
				logger.log(Level.INFO, "     - Local file DIFFERS from file version, expected DOS ATTRS = {0}, but actual DOS ATTRS = {1}, for file {2}", 
						new Object[] { fileComparison.databaseFileProperties.getDosAttributes(), fileComparison.localFileProperties.getDosAttributes(), actualLocalFile });
				
				return fileComparison;
			}
		}
		else if (FileUtil.isUnixLikeOperatingSystem()) {
			if (fileComparison.databaseFileProperties.getPosixPermissions() != null 
					&& !fileComparison.databaseFileProperties.getPosixPermissions().equals(fileComparison.localFileProperties.getPosixPermissions())) {
				
				logger.log(Level.INFO, "     - Local file DIFFERS from file version, expected POSIX ATTRS = {0}, but actual POSIX ATTRS = {1}, for file {2}", 
						new Object[] { fileComparison.databaseFileProperties.getPosixPermissions(), fileComparison.localFileProperties.getPosixPermissions(), actualLocalFile });
				
				return fileComparison;
			}			
		}
				
		// Do not check checksum
		if (!forceChecksum) {
			logger.log(Level.INFO, "     - Local file matches file version (checksum SKIPPED), file {0}", new Object[] { actualLocalFile });
			
			fileComparison.equals = true;
			return fileComparison;			
		}
		
		// Check checksum		
		if (fileComparison.localFileProperties.getChecksum() == null || fileComparison.databaseFileProperties.getChecksum() == null) {
			logger.log(Level.SEVERE, "     - Local file DIFFERS or at least that is what we are guessing here, file {0}", new Object[] { actualLocalFile });
			logger.log(Level.SEVERE, "        ---> If checksum checks are enabled, there should be no case in which checksums are null. The if-statements above must have missed a case.");
			logger.log(Level.SEVERE, "        ---> Assuming file has changed now!");
			
			return fileComparison;	
		}
		
		try {			 
			boolean isChecksumEqual = Arrays.equals(fileComparison.localFileProperties.getChecksum(), fileComparison.databaseFileProperties.getChecksum());
			
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
		
		Path filePath = Paths.get(file.getAbsolutePath());
		
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
			
			fileProperties.exists = true;
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
		fileProperties.posixPermissions = fileVersion.getPosixPermissions();
		fileProperties.dosAttributes = fileVersion.getDosAttributes();
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
