package org.syncany.operations;

import java.io.File;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;
import org.syncany.util.FileUtil;

public class RenameFileSystemAction extends FileSystemAction {
	public RenameFileSystemAction(Config config, FileVersion from, FileVersion to, Database localDatabase, Database winningDatabase) {
		super(config, localDatabase, winningDatabase, from, to);
	}

	@Override
	public void execute() throws Exception {
		if (!isExpectedFile(file1)) {
			createConflictFile(file1);
			reconstructFile(file2);
		}
		else {
			File fromFileOnDisk = getAbsolutePathFile(file1.getFullName());
			File toFileOnDisk = getAbsolutePathFile(file2.getFullName());			
			
			FileUtil.renameVia(fromFileOnDisk, toFileOnDisk);						
		}			
	}

	@Override
	public String toString() {
		return "RenameFileSystemAction [file1=" + file1 + ", file2=" + file2 + "]";
	}		
	
}