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
		// Special case: locally unknown file to be deleted (= nothing to do!)
		if (file1 == null) {
			if (!isExpectedFile(file2)) {
				createConflictFile(file2);				
			}
		}
		
		// Normal case: locally known file to be deleted
		else {
			if (!isExpectedFile(file1)) {
				createConflictFile(file2);				
			}
		}
			
		File toDeleteFileOnDisk = getAbsolutePathFile(file2.getFullName());
		toDeleteFileOnDisk.delete();
	}

	@Override
	public String toString() {
		return "DeleteFileSystemAction [file1=" + file1 + ", file2=" + file2 + "]";
	}
}

