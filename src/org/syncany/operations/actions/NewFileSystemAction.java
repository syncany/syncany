package org.syncany.operations.actions;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;

public class NewFileSystemAction extends FileCreatingFileSystemAction {

	public NewFileSystemAction(Config config, FileVersion newFileVersion, Database localDatabase, Database winningDatabase) {
		super(config, localDatabase, winningDatabase, null, newFileVersion);
	}
	
	@Override
	public void execute() throws Exception {
		if (fileExists(fileVersion2)) {
			if (fileAsExpected(fileVersion2)) {
				// Nothing to do
			}
			else {
				createConflictFile(fileVersion2);
				createFileFolderOrSymlink(fileVersion2);				
			}
		}
		else {
			createFileFolderOrSymlink(fileVersion2);
		}		
	}

	@Override
	public String toString() {
		return "NewFileSystemAction [file1=" + fileVersion1 + ", file2=" + fileVersion2 + "]";
	}
}

