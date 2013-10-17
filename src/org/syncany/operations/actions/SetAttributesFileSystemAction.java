package org.syncany.operations.actions;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersionComparator.FileChange;

public class SetAttributesFileSystemAction extends FileSystemAction {

	public SetAttributesFileSystemAction(Config config, FileVersion newFileVersion, Database localDatabase, Database winningDatabase) {
		super(config, localDatabase, winningDatabase, null, newFileVersion);
	}
	
	@Override
	public void execute() throws Exception {
		if (fileExists(fileVersion2) 
				&& fileAsExpected(fileVersion2, FileChange.CHANGED_ATTRIBUTES, FileChange.CHANGED_LAST_MOD_DATE)) {
			
			setFileAttributes(fileVersion2);
			setLastModified(fileVersion2);
		}
		else {		
			throw new Exception("Inconsistent file system, file not as expected: "+ fileVersion2);
		}
	}

	@Override
	public String toString() {
		return "NewFileSystemAction [file1=" + fileVersion1 + ", file2=" + fileVersion2 + "]";
	}
}

