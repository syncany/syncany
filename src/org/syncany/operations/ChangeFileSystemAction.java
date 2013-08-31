package org.syncany.operations;

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
		if (!isExpectedFile(file1)) {
			createConflictFile(file1);
			reconstructFile(file2);
		}
		else {
			File fromFileOnDisk = getAbsolutePathFile(file1.getFullName());
			fromFileOnDisk.delete();
			
			reconstructFile(file2);				
		}			
	}

	@Override
	public String toString() {
		return "ChangeFileSystemAction [file1=" + file1 + ", file2=" + file2 + "]";
	}				
}	
