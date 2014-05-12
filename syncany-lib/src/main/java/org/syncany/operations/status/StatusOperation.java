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
package org.syncany.operations.status;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersionComparator;
import org.syncany.database.FileVersionComparator.FileVersionComparison;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.ChangeSet;
import org.syncany.operations.Operation;
import org.syncany.operations.OperationOptions;
import org.syncany.operations.OperationResult;
import org.syncany.util.FileUtil;

/**
 * The status operation analyzes the local file tree and compares it to the current local
 * database. It uses the {@link FileVersionComparator} to determine differences and returns
 * new/changed/deleted files in form of a {@link ChangeSet}.
 *   
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class StatusOperation extends Operation {
	private static final Logger logger = Logger.getLogger(StatusOperation.class.getSimpleName());	
	
	private FileVersionComparator fileVersionComparator; 
	private SqlDatabase localDatabase;
	private StatusOperationOptions options;
	
	public StatusOperation(Config config) {
		this(config, new StatusOperationOptions());
	}	
	
	public StatusOperation(Config config, StatusOperationOptions options) {
		super(config);		
		
		this.fileVersionComparator = new FileVersionComparator(config.getLocalDir(), config.getChunker().getChecksumAlgorithm());
		this.localDatabase = new SqlDatabase(config);
		this.options = options;		
	}	
	
	@Override
	public StatusOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Status' at client "+config.getMachineName()+" ...");
		logger.log(Level.INFO, "--------------------------------------------");
		
		if (options != null && options.isForceChecksum()) {
			logger.log(Level.INFO, "Force checksum ENABLED.");
		}
		
		// Get local database
		logger.log(Level.INFO, "Querying current file tree from database ...");				

		// Path to actual file version
		final Map<String, FileVersion> filesInDatabase = localDatabase.getCurrentFileTree();

		// Find local changes
		logger.log(Level.INFO, "Analyzing local folder "+config.getLocalDir()+" ...");								
		ChangeSet localChanges = findLocalChanges(filesInDatabase);
		
		if (!localChanges.hasChanges()) {
			logger.log(Level.INFO, "- No changes to local database");
		}
		
		// Return result
		StatusOperationResult statusResult = new StatusOperationResult();
		statusResult.setChangeSet(localChanges);
		
		return statusResult;
	}

	private ChangeSet findLocalChanges(final Map<String, FileVersion> filesInDatabase) throws FileNotFoundException, IOException {
		ChangeSet localChanges = findLocalChangedAndNewFiles(config.getLocalDir(), filesInDatabase);
		findAndAppendDeletedFiles(localChanges, filesInDatabase);
		
		return localChanges;
	}		
	
	private ChangeSet findLocalChangedAndNewFiles(final File root, Map<String, FileVersion> filesInDatabase) throws FileNotFoundException, IOException {
		Path rootPath = Paths.get(root.getAbsolutePath());
		
		StatusFileVisitor fileVisitor = new StatusFileVisitor(rootPath, filesInDatabase);		
		Files.walkFileTree(rootPath, fileVisitor);
		
		return fileVisitor.getChangeSet();		
	}
	
	private void findAndAppendDeletedFiles(ChangeSet localChanges, Map<String,FileVersion> filesInDatabase) {
		for (FileVersion lastLocalVersion : filesInDatabase.values()) {
			// Check if file exists, remove if it doesn't
			File lastLocalVersionOnDisk = new File(config.getLocalDir()+File.separator+lastLocalVersion.getPath());
			
			// Ignore this file history if the last version is marked "DELETED"
			if (lastLocalVersion.getStatus() == FileStatus.DELETED) {
				continue;
			}
			
			// If file has VANISHED, mark as DELETED 
			if (!FileUtil.exists(lastLocalVersionOnDisk)) {
				localChanges.getDeletedFiles().add(lastLocalVersion.getPath());
			}
		}		
	}
	
	private class StatusFileVisitor implements FileVisitor<Path> {
		private Path root;
		private ChangeSet changeSet;		
		private Map<String, FileVersion> currentFileTree;
		
		public StatusFileVisitor(Path root, Map<String, FileVersion> currentFileTree) {
			this.root = root;
			this.changeSet = new ChangeSet();
			this.currentFileTree = currentFileTree;
		}

		public ChangeSet getChangeSet() {
			return changeSet;
		}
		 
		@Override
		public FileVisitResult visitFile(Path actualLocalFile, BasicFileAttributes attrs) throws IOException {
			String relativeFilePath = FileUtil.getRelativeDatabasePath(root.toFile(), actualLocalFile.toFile()); //root.relativize(actualLocalFile).toString();
			
			// Skip Syncany root folder
			if (actualLocalFile.toFile().equals(config.getLocalDir())) {
				return FileVisitResult.CONTINUE;
			}
			
			// Skip .syncany (or app related acc. to config) 		
			boolean isAppRelatedDir =
				   actualLocalFile.toFile().equals(config.getAppDir())
				|| actualLocalFile.toFile().equals(config.getCache())
				|| actualLocalFile.toFile().equals(config.getDatabaseDir())
				|| actualLocalFile.toFile().equals(config.getLogDir());
			
			if (isAppRelatedDir) {
				logger.log(Level.FINEST, "- Ignoring file (syncany app-related): {0}", relativeFilePath);
				return FileVisitResult.SKIP_SUBTREE;
			}
				
			// Check if file is locked
			boolean fileLocked = FileUtil.isFileLocked(actualLocalFile.toFile());
			
			if (fileLocked) {
				logger.log(Level.FINEST, "- Ignoring file (locked): {0}", relativeFilePath);						
				return FileVisitResult.CONTINUE;
			}				
			
			// Check database by file path
			FileVersion expectedLastFileVersion = currentFileTree.get(relativeFilePath);
			
			if (expectedLastFileVersion != null) {				
				// Compare
				boolean forceChecksum = options != null && options.isForceChecksum();
				FileVersionComparison fileVersionComparison = fileVersionComparator.compare(expectedLastFileVersion, actualLocalFile.toFile(), forceChecksum); 
				
				if (fileVersionComparison.equals()) {
					changeSet.getUnchangedFiles().add(relativeFilePath);
				}
				else {
					changeSet.getChangedFiles().add(relativeFilePath);
				}					
			}
			else {
				if (!config.getIgnoredFiles().isFileIgnored(relativeFilePath)) {
					changeSet.getNewFiles().add(relativeFilePath);
					logger.log(Level.FINEST, "- New file: "+relativeFilePath);
				}
				else {
					logger.log(Level.FINEST, "- Ignoring file; " + relativeFilePath);
					return FileVisitResult.SKIP_SUBTREE;
				}
			}			
			
			// Check if file is symlink directory
			boolean isSymlinkDir = attrs.isDirectory() && attrs.isSymbolicLink();
			
			if (isSymlinkDir) {
				logger.log(Level.FINEST, "   + File is sym. directory. Skipping subtree.");
				return FileVisitResult.SKIP_SUBTREE;
			}
			else {
				return FileVisitResult.CONTINUE;
			}
		}
		
		@Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException { 
			return visitFile(dir, attrs);
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
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
