package org.syncany.operations.actions;

import java.io.File;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;

public class RenameFileSystemAction extends FileSystemAction {
	public RenameFileSystemAction(Config config, FileVersion from, FileVersion to, Database localDatabase, Database winningDatabase) {
		super(config, localDatabase, winningDatabase, from, to);
	}

	@Override
	public void execute() throws Exception {
		if (!fileAsExpected(fileVersion1)) {
			createConflictFile(fileVersion1);
			createFile(fileVersion2);
		}
		else {
			File fromFileOnDisk = getAbsolutePathFile(fileVersion1.getFullName());
			File toFileOnDisk = getAbsolutePathFile(fileVersion2.getFullName());			
			
			logger.log(Level.INFO, "     - Renaming file "+fromFileOnDisk+" to "+toFileOnDisk+" ...");				
			FileUtils.moveFile(fromFileOnDisk, toFileOnDisk);						
		}			
	}	
	
	@Override
	public String toString() {
		return "RenameFileSystemAction [file1=" + fileVersion1 + ", file2=" + fileVersion2 + "]";
	}

	
}