package org.syncany.operations;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;

public class NewFileSystemAction extends FileSystemAction {

	public NewFileSystemAction(Config config, FileVersion newFileVersion, Database localDatabase, Database winningDatabase) {
		super(config, localDatabase, winningDatabase, newFileVersion, null);
	}
	
	@Override
	public void execute() throws Exception {
		if (!isExpectedFile(file1)) {
			createConflictFile(file1);
		}
		
		reconstructFile(file1);			
	}

	@Override
	public String toString() {
		return "NewFileSystemAction [file1=" + file1 + ", file2=" + file2 + "]";
	}
}

