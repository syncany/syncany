package org.syncany.operations;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;

public class NewFileSystemAction extends FileSystemAction {

	public NewFileSystemAction(Config config, FileVersion newFileVersion, Database localDatabase, Database winningDatabase) {
		super(config, localDatabase, winningDatabase, null, newFileVersion);
	}
	
	@Override
	public void execute() throws Exception {
		if (fileExists(fileVersion2)) {
			if (!fileAsExpected(fileVersion2)) {
				createConflictFile(fileVersion2);
			}
		}
		else {		
			createFile(fileVersion2);
		}
	}

	@Override
	public String toString() {
		return "NewFileSystemAction [file1=" + fileVersion1 + ", file2=" + fileVersion2 + "]";
	}
}

