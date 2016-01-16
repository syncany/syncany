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
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersionComparator;
import org.syncany.database.MemoryDatabase;

public class DeleteFileSystemAction extends FileSystemAction {
	public DeleteFileSystemAction(Config config, FileVersion fromFileVersion, FileVersion toDeleteFileVersion, MemoryDatabase winningDatabase) {
		super(config, winningDatabase, fromFileVersion, toDeleteFileVersion);
	}

	/**
	 * Deletes a file locally and/or creates a conflicting file if the
	 * file does not match the expectations. There are two major cases:
	 * 
	 * <p>Normal case: The file version of the to-be-deleted file is known. If the file is 
	 * as expected, it is deleted; if not and the file exists, a conflict file is created.
	 * 
	 * <p>Special case: The file version of the to-be-deleted file in unknown. 
	 * In case to-be-deleted-file exists locally, we need to compare it to
	 * the local file (especially its checksum!). The {@link FileVersionComparator}
	 * does, however, perform a cancelling test in which {@link FileVersion}s marked as
	 * 'DELETED' are not compared in detail (no checksum/attribute/etc. comparisons). To
	 * circumvent this behavior, we pretend the file has just changed and do the comparison.
	 * If the to-be-deleted file and file version are equal, the local file is deleted. 
	 * Otherwise, a conflict file is created. 
	 */
	@Override
	public FileSystemActionResult execute() throws Exception {
		// Special case: locally unknown file to be deleted		
		if (fileVersion1 == null) {
			if (fileExists(fileVersion2)) {
				FileVersion pretendChangedFileVersion = fileVersion2.clone();
				pretendChangedFileVersion.setStatus(FileStatus.CHANGED); 
				
				if (fileAsExpected(pretendChangedFileVersion)) {
					deleteFile(fileVersion2);					
				}
				else {
					moveToConflictFile(fileVersion2);					
				}
			}			
		}
		
		// Normal case: locally known file to be deleted
		else {
			if (!fileAsExpected(fileVersion1)) {
				if (fileExists(fileVersion1)) {
					moveToConflictFile(fileVersion2);	
				}
			}
			else {
				deleteFile(fileVersion1);	
			}
		}
					
		return new FileSystemActionResult();
	}

	@Override
	public String toString() {
		return "DeleteFileSystemAction [file1=" + fileVersion1 + ", file2=" + fileVersion2 + "]";
	}
}

