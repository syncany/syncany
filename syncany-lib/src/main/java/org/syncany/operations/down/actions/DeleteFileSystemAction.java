/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
import org.syncany.database.MemoryDatabase;

public class DeleteFileSystemAction extends FileSystemAction {
	public DeleteFileSystemAction(Config config, FileVersion fromFileVersion, FileVersion toDeleteFileVersion, MemoryDatabase winningDatabase) {
		super(config, winningDatabase, fromFileVersion, toDeleteFileVersion);
	}
	
	@Override
	public FileSystemActionResult execute() throws Exception {
		// Special case: locally unknown file to be deleted (= nothing to do!)
		if (fileVersion1 == null) {
			if (fileExists(fileVersion2)) {
				moveToConflictFile(fileVersion2);				
			}
		}
		
		// Normal case: locally known file to be deleted
		else {
			if (!fileAsExpected(fileVersion1)) {
				if (fileExists(fileVersion1)) {
					moveToConflictFile(fileVersion2);	
				}
			}
		}
			
		deleteFile(fileVersion2);
		return new FileSystemActionResult();
	}

	@Override
	public String toString() {
		return "DeleteFileSystemAction [file1=" + fileVersion1 + ", file2=" + fileVersion2 + "]";
	}
}

