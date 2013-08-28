package org.syncany.operations;

import java.io.File;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;

public class DeleteFileSystemAction extends FileSystemAction {
	private FileVersion fromFileVersion;
	private FileVersion toDeleteFileVersion;
	
	public DeleteFileSystemAction(Config config, FileVersion fromFileVersion, FileVersion toDeleteFileVersion, Database localDatabase, Database winningDatabase) {
		super(config, localDatabase, winningDatabase);
		this.fromFileVersion = fromFileVersion;
		this.toDeleteFileVersion = toDeleteFileVersion;
	}
	
	@Override
	public void execute() {
		if (!isExpectedFile(fromFileVersion)) {
			createConflictFile(fromFileVersion);				
		}
		
		File toDeleteFileOnDisk = getAbsolutePathFile(toDeleteFileVersion.getFullName());
		toDeleteFileOnDisk.delete();			
	}
}

