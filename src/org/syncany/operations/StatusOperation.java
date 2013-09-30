package org.syncany.operations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.PartialFileHistory;
import org.syncany.operations.LoadDatabaseOperation.LoadDatabaseOperationResult;
import org.syncany.util.FileLister;
import org.syncany.util.FileLister.FileListerAdapter;
import org.syncany.util.FileUtil;

public class StatusOperation extends Operation {
	private static final Logger logger = Logger.getLogger(StatusOperation.class.getSimpleName());	
	private Database loadedDatabase;
	private StatusOperationOptions options;
	
	public StatusOperation(Config config) {
		this(config, null, new StatusOperationOptions());
	}	
	
	public StatusOperation(Config config, Database database, StatusOperationOptions options) {
		super(config);		
		this.loadedDatabase = database;
		this.options = options;
	}	
	
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Status' at client "+config.getMachineName()+" ...");
		logger.log(Level.INFO, "--------------------------------------------");
		
		if (options != null && options.isForceChecksum()) {
			logger.log(Level.INFO, "Force checksum ENABLED.");
		}
		
		Database database = (loadedDatabase != null) 
				? loadedDatabase
				: ((LoadDatabaseOperationResult) new LoadDatabaseOperation(config).execute()).getDatabase();		
		
		logger.log(Level.INFO, "Analyzing local folder "+config.getLocalDir()+" ...");				
		ChangeSet changeSet = findChangedAndNewFiles(config.getLocalDir(), database);
		
		if (!changeSet.hasChanges()) {
			logger.log(Level.INFO, "- No changes to local database");
		}
		
		StatusOperationResult statusResult = new StatusOperationResult();
		statusResult.setChangeSet(changeSet);
		
		return statusResult;
	}		

	private ChangeSet findChangedAndNewFiles(final File root, final Database database) throws FileNotFoundException, IOException {
		final ChangeSet changeSet = new ChangeSet();
		
		FileLister fileLister = new FileLister(root, new FileListerAdapter() {
			@Override
			public void enterDirectory(File directory) {
				processFile(directory);
			}
			
			@Override
			public void processFile(File file) {
				String relativeFilePath = FileUtil.getRelativePath(root, file);

				// Check database by file path
				PartialFileHistory potentiallyMatchingFileHistory = database.getFileHistory(relativeFilePath);
				
				if (potentiallyMatchingFileHistory != null) {
					FileVersion potentiallyMatchingLastFileVersion = potentiallyMatchingFileHistory.getLastVersion();
					
					// Don't do anything if this file is marked as deleted
					if (potentiallyMatchingLastFileVersion.getStatus() == FileStatus.DELETED) {
						return;
					}
					
					// Don't do anything if file is folder
					if (file.isDirectory()) {
						logger.log(Level.FINEST, "- Unchanged file (directory): {0}", relativeFilePath);						
						changeSet.unchangedFiles.add(relativeFilePath);
						
						return;
					}										
					
					// Simple check by size
					boolean sizeMatches = file.length() == potentiallyMatchingLastFileVersion.getSize();
					
					if (!sizeMatches) {
						changeSet.changedFiles.add(relativeFilePath);
						logger.log(Level.FINEST, "- Changed file (size, db: {0}, disk: {1}): {2}", new Object[] { potentiallyMatchingLastFileVersion.getSize(), file.length(), relativeFilePath });
						
						return;
					}
					
					// Simple check by modified date
					boolean modifiedDateMatches = file.lastModified() == potentiallyMatchingLastFileVersion.getLastModified().getTime();

					if (!modifiedDateMatches) {
						changeSet.changedFiles.add(relativeFilePath);
						logger.log(Level.FINEST, "- Changed file (mod. date, db: {0}, disk: {1}): {2}", new Object[] { potentiallyMatchingLastFileVersion.getLastModified(), new Date(file.lastModified()), relativeFilePath });
						
						return;
					}
					
					// Zero-size has not checksum
					if (file.length() == 0) {
						changeSet.unchangedFiles.add(relativeFilePath);
						logger.log(Level.FINEST, "- Unchanged file (zero-size!): {0}", relativeFilePath);
						
						return;
					}
					
					if (options != null && !options.isForceChecksum()) {
						changeSet.unchangedFiles.add(relativeFilePath);
						logger.log(Level.FINEST, "- Unchanged file (assuming, --force-checksum disabled): {0}", relativeFilePath);

						return;
					}
					
					// If the file modified date from the database and the file system modified date
					// match, it could mean that both files are equal, or that the file was changed
					// immediately after the 'up'-operation (in the same second!)
					
					// To be sure, we must (!) calculate the checksum
					
					try {
						byte[] fileChecksum = FileUtil.createChecksum(file, "SHA1"); // TODO [low] The digest could be something else! Get digest from somewhere (Chunker?)
						
						if (!Arrays.equals(fileChecksum, potentiallyMatchingLastFileVersion.getChecksum())) {
							changeSet.changedFiles.add(relativeFilePath);
							logger.log(Level.FINEST, "- Changed file (checksum!): {0}", relativeFilePath);
						}
						else {
							changeSet.unchangedFiles.add(relativeFilePath);
							logger.log(Level.FINEST, "- Unchanged file (checksum!): {0}", relativeFilePath);
						}
					} 
					catch (Exception e) {
						// Error: Simply assume file has changed
						changeSet.changedFiles.add(relativeFilePath);
						logger.log(Level.FINEST, "- Error when creating checksum, assuming file was changed: {0}", relativeFilePath);
					}
				}
				else {
					changeSet.newFiles.add(relativeFilePath);
					logger.log(Level.FINEST, "- New file: "+relativeFilePath);
				}				
			}			
			
			@Override
			public boolean fileFilter(File file) {
				// Symlinks are not supported right now
				// TODO [low] Add support for symlinks
				try {
					if (FileUtils.isSymlink(file)) {
						logger.log(Level.FINEST, "- Ignored file (symlink!): {0}", file);
						return false;
					}
				}
				catch (Exception e) {
					logger.log(Level.FINEST, "- Ignored file (FAILED checking for symlink): {0}", file);
					return false;
				}

				return true;
			}			
			
			@Override
			public boolean directoryFilter(File directory) {
				boolean isAppRelatedDir = directory.equals(config.getAppDir())
					|| directory.equals(config.getCache())
					|| directory.equals(config.getDatabaseDir());
				
				return !isAppRelatedDir;
			}
		});
		
		fileLister.start();
		
		// Find deleted files
		for (PartialFileHistory fileHistory : database.getFileHistories()) {
			// Check if file exists, remove if it doesn't
			FileVersion lastLocalVersion = fileHistory.getLastVersion();
			File lastLocalVersionOnDisk = new File(config.getLocalDir()+File.separator+lastLocalVersion.getFullName());
			
			// Ignore this file history if the last version is marked "DELETED"
			if (lastLocalVersion.getStatus() == FileStatus.DELETED) {
				continue;
			}
			
			// If file has VANISHED, mark as DELETED 
			if (!lastLocalVersionOnDisk.exists()) {
				changeSet.deletedFiles.add(lastLocalVersion.getFullName());
			}
		}						
		
		return changeSet;
	}
	
	public static class ChangeSet {
		private List<String> changedFiles;
		private List<String> newFiles;
		private List<String> deletedFiles;
		private List<String> unchangedFiles;
		
		public ChangeSet() {
			changedFiles = new ArrayList<String>();
			newFiles = new ArrayList<String>();
			deletedFiles = new ArrayList<String>();
			unchangedFiles = new ArrayList<String>();
		}
		
		public boolean hasChanges() {
			return changedFiles.size() > 0 
				|| newFiles.size() > 0
				|| deletedFiles.size() > 0;
		}
		
		public List<String> getChangedFiles() {
			return changedFiles;
		}
		
		public List<String> getNewFiles() {
			return newFiles;
		}
		
		public List<String> getDeletedFiles() {
			return deletedFiles;
		}	
		
		public List<String> getUnchangedFiles() {
			return unchangedFiles;
		}	
	}
	
	public static class StatusOperationOptions implements OperationOptions {
		private boolean forceChecksum = false;

		public boolean isForceChecksum() {
			return forceChecksum;
		}

		public void setForceChecksum(boolean forceChecksum) {
			this.forceChecksum = forceChecksum;
		}				
	}
	
	public static class StatusOperationResult implements OperationResult {
		private ChangeSet changeSet;

		public StatusOperationResult() {
			changeSet = new ChangeSet();
		}
		
		public void setChangeSet(ChangeSet changeSet) {
			this.changeSet = changeSet;
		}

		public ChangeSet getChangeSet() {
			return changeSet;
		}
	}
}
