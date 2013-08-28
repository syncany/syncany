package org.syncany.operations;

import java.io.File;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;
import org.syncany.util.FileUtil;

public class RenameFileSystemAction extends FileSystemAction {
	private FileVersion fromFileVersion;
	private FileVersion toFileVersion;
	
	public RenameFileSystemAction(Config config, FileVersion from, FileVersion to, Database localDatabase, Database winningDatabase) {
		super(config, localDatabase, winningDatabase);
		this.fromFileVersion = from;
		this.toFileVersion = to;
	}

	@Override
	public void execute() throws Exception {
		if (!isExpectedFile(fromFileVersion)) {
			createConflictFile(fromFileVersion);
			reconstructFile(toFileVersion);
		}
		else {
			File fromFileOnDisk = getAbsolutePathFile(fromFileVersion.getFullName());
			File toFileOnDisk = getAbsolutePathFile(toFileVersion.getFullName());			
			
			FileUtil.renameVia(fromFileOnDisk, toFileOnDisk);						
		}			
	}				
}