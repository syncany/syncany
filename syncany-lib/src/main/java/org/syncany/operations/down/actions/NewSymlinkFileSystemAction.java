/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.operations.down.actions;

import org.syncany.config.Config;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersionComparator.FileChange;
import org.syncany.database.FileVersionComparator.FileVersionComparison;
import org.syncany.database.MemoryDatabase;
import org.syncany.util.CollectionUtil;

public class NewSymlinkFileSystemAction extends FileSystemAction {
	public NewSymlinkFileSystemAction(Config config, FileVersion newFileVersion, MemoryDatabase winningDatabase) {
		super(config, winningDatabase, null, newFileVersion);
	}
	
	@Override
	public FileSystemActionResult execute() throws Exception {
		if (fileExists(fileVersion2)) {
			FileVersionComparison fileComparison = fileChanges(fileVersion2);
			
			if (fileComparison.areEqual()) {
				// Nothing to do.
			}
			else if (CollectionUtil.containsOnly(fileComparison.getFileChanges(), FileChange.CHANGED_LINK_TARGET)) {
				deleteFile(fileVersion2);
				createSymlink(fileVersion2);
			}
			else {
				moveToConflictFile(fileVersion2);
				createSymlink(fileVersion2);
			}
		}
		else {
			createSymlink(fileVersion2);
		}			
		
		return new FileSystemActionResult();
	}

	@Override
	public String toString() {
		return "NewSymlinkFileSystemAction [file1=" + fileVersion1 + ", file2=" + fileVersion2 + "]";
	}				
}	
