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

public class NewFileSystemAction extends FileCreatingFileSystemAction {

	public NewFileSystemAction(Config config, FileVersion newFileVersion, MemoryDatabase winningDatabase) {
		super(config, winningDatabase, null, newFileVersion);
	}
	
	@Override
	public void execute() throws Exception {
		if (fileExists(fileVersion2)) {
			if (fileAsExpected(fileVersion2)) {
				// Nothing to do
			}
			else {
				moveToConflictFile(fileVersion2);
				createFileFolderOrSymlink(fileVersion2);				
			}
		}
		else {
			createFileFolderOrSymlink(fileVersion2);
		}		
	}

	@Override
	public String toString() {
		return "NewFileSystemAction [file1=" + fileVersion1 + ", file2=" + fileVersion2 + "]";
	}
}

