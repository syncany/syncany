package org.syncany.operations.actions;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;

public class NewSymlinkFileSystemAction extends FileSystemAction {
	public NewSymlinkFileSystemAction(Config config, FileVersion newFileVersion, Database localDatabase, Database winningDatabase) {
		super(config, localDatabase, winningDatabase, null, newFileVersion);
	}
	
	@Override
	public void execute() throws Exception {
		if (fileExists(fileVersion2)) {
			if (!fileAsExpected(fileVersion2)) {
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
