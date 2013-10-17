package org.syncany.operations.actions;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersionComparator.FileChange;
import org.syncany.database.FileVersionComparator.FileVersionComparison;
import org.syncany.util.CollectionUtil;

public class NewSymlinkFileSystemAction extends FileSystemAction {
	public NewSymlinkFileSystemAction(Config config, FileVersion newFileVersion, Database localDatabase, Database winningDatabase) {
		super(config, localDatabase, winningDatabase, null, newFileVersion);
	}
	
	@Override
	public void execute() throws Exception {
		if (fileExists(fileVersion2)) {
			FileVersionComparison fileComparison = fileChanges(fileVersion2);
			
			if (fileComparison.equals()) {
				// Nothing to do.
			}
			else if (CollectionUtil.containsOnly(fileComparison.getFileChanges(), FileChange.CHANGED_LINK_TARGET)) {
				deleteFile(fileVersion2);
				createSymlink(fileVersion2);
			}
			else {
				createConflictFile(fileVersion2);
				createSymlink(fileVersion2);
			}
		}
		else {
			createSymlink(fileVersion2);
		}			
	}

	@Override
	public String toString() {
		return "NewSymlinkFileSystemAction [file1=" + fileVersion1 + ", file2=" + fileVersion2 + "]";
	}				
}	
