package org.syncany.operations;

import java.io.File;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;

public class DeleteFileSystemAction extends FileSystemAction {
	public DeleteFileSystemAction(Config config, FileVersion fromFileVersion, FileVersion toDeleteFileVersion, Database localDatabase, Database winningDatabase) {
		super(config, localDatabase, winningDatabase, fromFileVersion, toDeleteFileVersion);
	}
	
	@Override
	public void execute() {
		if (!isExpectedFile(file1)) {
			createConflictFile(file2);				
		}
		
		File toDeleteFileOnDisk = getAbsolutePathFile(file2.getFullName());
		toDeleteFileOnDisk.delete();			
	}

	@Override
	public String toString() {
		return "DeleteFileSystemAction [file1=" + file1 + ", file2=" + file2 + "]";
	}
}

