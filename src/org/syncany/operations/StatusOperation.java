package org.syncany.operations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.PartialFileHistory;
import org.syncany.util.FileLister;
import org.syncany.util.FileLister.FileListerAdapter;
import org.syncany.util.FileUtil;

public class StatusOperation extends Operation {
	private static final Logger logger = Logger.getLogger(StatusOperation.class.getSimpleName());
	
	public StatusOperation(Config config) {
		super(config);
	}	
	
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Status' at client "+config.getMachineName()+" ...");
		logger.log(Level.INFO, "--------------------------------------------");
		
		logger.log(Level.INFO, "Loading local database ...");		
		Database db = loadLocalDatabase(config.getDatabaseFile());		
		
		logger.log(Level.INFO, "Analyzing local folder "+config.getLocalDir()+" ...");				
		ChangeSet changeSet = findChangedAndNewFiles(config.getLocalDir(), db);
		
		if (changeSet.changedFiles.size() > 0 || changeSet.newFiles.size() > 0 || changeSet.deletedFiles.size() > 0) {
					
		}
		else {
			logger.log(Level.INFO, "- No changes to local database");
		}
		
		return new StatusOperationResult(changeSet);
	}		

	private ChangeSet findChangedAndNewFiles(final File root, final Database db) throws FileNotFoundException, IOException {
		final ChangeSet changeSet = new ChangeSet();
		
		FileLister fileLister = new FileLister(root, new FileListerAdapter() {			
			@Override
			public void proceedFile(File file) {
				String relativeFilePath = FileUtil.getRelativePath(root, file);
				PartialFileHistory potentiallyMatchingFileHistory = db.getFileHistory(relativeFilePath);
				
				if (potentiallyMatchingFileHistory != null) {
					FileVersion potentiallyMatchingLastFileVersion = potentiallyMatchingFileHistory.getLastVersion();
					
					if (potentiallyMatchingLastFileVersion.getStatus() != FileStatus.DELETED) {
						boolean localFileMatches = 
							   file.lastModified() == potentiallyMatchingLastFileVersion.getLastModified().getTime()
							&& file.length() == potentiallyMatchingLastFileVersion.getSize();
						
						if (!localFileMatches) {
							changeSet.changedFiles.add(file);
							logger.log(Level.FINEST, "- Changed file "+relativeFilePath);
						}
						else {
							logger.log(Level.FINEST, "- Unchanged file "+relativeFilePath);
						}
					}
				}
				else {
					changeSet.newFiles.add(file);
					logger.log(Level.FINEST, "- New file "+relativeFilePath);
				}				
			}			
			
			@Override
			public boolean fileFilter(File file) {
				return true;
			}			
			
			@Override
			public boolean directoryFilter(File directory) {
				return true;
			}
		});
		
		fileLister.start();
		
		// Find deleted files
		for (PartialFileHistory fileHistory : db.getFileHistories()) {
			// Check if file exists, remove if it doesn't
			FileVersion lastLocalVersion = fileHistory.getLastVersion();
			File lastLocalVersionOnDisk = new File(config.getLocalDir()+File.separator+lastLocalVersion.getFullName());
			
			// Ignore this file history if the last version is marked "DELETED"
			if (lastLocalVersion.getStatus() == FileStatus.DELETED) {
				continue;
			}
			
			// If file has VANISHED, mark as DELETED 
			if (!lastLocalVersionOnDisk.exists()) {
				changeSet.deletedFiles.add(lastLocalVersionOnDisk);
			}
		}						
		
		return changeSet;
	}
	
	public class ChangeSet {
		private List<File> changedFiles;
		private List<File> newFiles;
		private List<File> deletedFiles;
		
		public ChangeSet() {
			changedFiles = new ArrayList<File>();
			newFiles = new ArrayList<File>();
			deletedFiles = new ArrayList<File>();
		}
		
		public List<File> getChangedFiles() {
			return changedFiles;
		}
		
		public List<File> getNewFiles() {
			return newFiles;
		}
		
		public List<File> getDeletedFiles() {
			return deletedFiles;
		}		
	}
	
	public class StatusOperationResult implements OperationResult {
		private ChangeSet changeSet;

		public StatusOperationResult(ChangeSet changeSet) {
			this.changeSet = changeSet;
		}

		public ChangeSet getChangeSet() {
			return changeSet;
		}
	}
}
