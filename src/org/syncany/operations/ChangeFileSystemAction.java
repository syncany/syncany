package org.syncany.operations;

import java.io.File;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;

public class ChangeFileSystemAction extends FileSystemAction {
	private FileVersion fromFileVersion;
	private FileVersion toFileVersion;
	
	public ChangeFileSystemAction(Config config, FileVersion fromFileVersion, FileVersion toFileVersion, Database localDatabase, Database winningDatabase) {
		super(config, localDatabase, winningDatabase);
		this.fromFileVersion = fromFileVersion;
		this.toFileVersion = toFileVersion;
	}

	@Override
	public void execute() throws Exception {
		if (!isExpectedFile(fromFileVersion)) {
			createConflictFile(fromFileVersion);
			reconstructFile(toFileVersion);
		}
		else {
			File fromFileOnDisk = getAbsolutePathFile(fromFileVersion.getFullName());
			fromFileOnDisk.delete();
			
			reconstructFile(toFileVersion);				
		}			
	}				
}	
