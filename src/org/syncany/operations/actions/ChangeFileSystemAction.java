package org.syncany.operations.actions;

import java.io.File;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;

public class ChangeFileSystemAction extends FileSystemAction {
	public ChangeFileSystemAction(Config config, FileVersion fromFileVersion, FileVersion toFileVersion, Database localDatabase, Database winningDatabase) {
		super(config, localDatabase, winningDatabase, fromFileVersion, toFileVersion);
	}
	
	@Override
	public void execute() throws Exception {
		if (!fileAsExpected(fileVersion1)) {
			createConflictFile(fileVersion1);
			createFile(fileVersion2);
		}
		else {
			File fromFileOnDisk = getAbsolutePathFile(fileVersion1.getPath());
			fromFileOnDisk.delete();
			
			if (fileExists(fileVersion2) && !fileAsExpected(fileVersion2)) {
				createConflictFile(fileVersion2);
			}
			
			createFile(fileVersion2);				
		}			
	}

	@Override
	public String toString() {
		return "ChangeFileSystemAction [file1=" + fileVersion1 + ", file2=" + fileVersion2 + "]";
	}				
}	
