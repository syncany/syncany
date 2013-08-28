package org.syncany.operations;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;

public class NewFileSystemAction extends FileSystemAction {
	private FileVersion newFileVersion;
	
	public NewFileSystemAction(Config config, FileVersion newFileVersion, Database localDatabase, Database winningDatabase) {
		super(config, localDatabase, winningDatabase);
		this.newFileVersion = newFileVersion;
	}
	
	@Override
	public void execute() throws Exception {
		if (!isExpectedFile(newFileVersion)) {
			createConflictFile(newFileVersion);
		}
		
		reconstructFile(newFileVersion);			
	}
}

