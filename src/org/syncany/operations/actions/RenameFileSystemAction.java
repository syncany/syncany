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
		File fromFileOnDisk = getAbsolutePathFile(fileVersion1.getFullName());
		File toFileOnDisk = getAbsolutePathFile(fileVersion2.getFullName());			
		
		boolean fromFileExists = fromFileOnDisk.exists();
		boolean toFileExists = toFileOnDisk.exists();
				
		if (fromFileExists && !toFileExists) { 
			if (fileAsExpected(fileVersion1)) { // << Expected case!
				logger.log(Level.INFO, "     - (1) Renaming file "+fromFileOnDisk+" to "+toFileOnDisk+" ...");				
				FileUtils.moveFile(fromFileOnDisk, toFileOnDisk);
			}
			else {
				logger.log(Level.INFO, "     - (2) Source file differs from what we expected. Creating target file at "+toFileOnDisk+" ...");
				createFile(fileVersion2);
			}
		}
		else if (fromFileExists && toFileExists) {
			if (fileAsExpected(fileVersion2)) {
				logger.log(Level.INFO, "     - (3) File at destination is what was expected. Nothing to do for "+toFileOnDisk+" ...");
			}
			else {
				logger.log(Level.INFO, "     - (4) Cannot rename because target file exists, but differs. Creating file at "+toFileOnDisk+" ...");
				
				// TODO [medium] Should we check if the formFile is as expected and delete it if it is?
				
				createConflictFile(fileVersion2);
				createFile(fileVersion2); 
			}			
		}		
		else if (!fromFileExists && !toFileExists) {
			logger.log(Level.INFO, "     - (5) Cannot rename because orig. file does not exist. Creating file at "+toFileOnDisk+" ...");
			createFile(fileVersion2);
		}
		else if (!fromFileExists && toFileExists) {
			if (fileAsExpected(fileVersion2)) {
				logger.log(Level.INFO, "     - (6) File at destination is what was expected. Nothing to do for "+toFileOnDisk+" ...");
			}
			else {
				logger.log(Level.INFO, "     - (7) Cannot rename because orig. file does not exist. Creating file at "+toFileOnDisk+" ...");
				
				createConflictFile(fileVersion2);
				createFile(fileVersion2);
			}
		}	
	}	
	
	@Override
	public String toString() {
		return "RenameFileSystemAction [file1=" + fileVersion1 + ", file2=" + fileVersion2 + "]";
	}

	
}