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

import java.util.logging.Level;

import org.syncany.config.Config;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.MemoryDatabase;

public class ChangeFileSystemAction extends FileCreatingFileSystemAction {
	public ChangeFileSystemAction(Config config, FileVersion fromFileVersion, FileVersion toFileVersion, MemoryDatabase winningDatabase) {
		super(config, winningDatabase, fromFileVersion, toFileVersion);
	}
	
	@Override
	public void execute() throws InconsistentFileSystemException, Exception {
		boolean fromFileExists = fileExists(fileVersion1);
		boolean fromFileMatches = fromFileExists && fileAsExpected(fileVersion1);
		
		boolean toFileExists = fileExists(fileVersion2);
		boolean toFileMatches = toFileExists && fileAsExpected(fileVersion2);
		
		boolean filesAtSameLocation = fileVersion1.getPath().equals(fileVersion2.getPath());

		if (fromFileMatches && !toFileMatches) { // Normal case	
			// Original file matches, so we can delete it
			// Create conflict file for winning file, if it exists
			if (!toFileExists) {
				logger.log(Level.INFO, "     - (1) Original file matches, target file does NOT match: deleting original file, creating target file at: "+fileVersion2);
				
				deleteFile(fileVersion1);	
				createFileFolderOrSymlink(fileVersion2);						
			}
			else {
				logger.log(Level.INFO, "     - (2) Original file matches, target file does NOT match (EXISTS!): deleting original file, creating conflict file and creating target file at: "+fileVersion2);
				
				deleteFile(fileVersion1);	
				moveToConflictFile(fileVersion2);
				createFileFolderOrSymlink(fileVersion2);										
			}				
		}
		else if (fromFileMatches && toFileMatches) {
			// Original file matches, so we can delete it
			// Nothing to do for winning file, matches
			
			if (!filesAtSameLocation) {
				logger.log(Level.INFO, "     - (3) Original file matches, target file matches, and they are not the same: deleting orig. file, nothing else!");
				deleteFile(fileVersion1);
			}
			else {
				logger.log(Level.INFO, "     - (4) Original file matches, target file matches, but they are in the same location (!!): Do nothing!");				
			}
		}
		else if (!fromFileMatches && toFileMatches) {
			// Leave original file untouched. Will be untracked from now on
			// Nothing to do for winning file, matches
			
			logger.log(Level.INFO, "     - (5) Original does NOT match, target file matches: Leaving orig. file untouched. Do nothing!");				
		}
		else if (!fromFileMatches && !toFileMatches) {
			// Leave original file untouched. Will be untracked from now on
			// Create conflict file for winning file, if it exists
			
			if (toFileExists) {
				logger.log(Level.INFO, "     - (6) Original does NOT match, target file does NOT match, but exists: Creating conflict file, and creating file at: "+fileVersion2);
				
				moveToConflictFile(fileVersion2);
				createFileFolderOrSymlink(fileVersion2);	
			}
			else {
				if (fileVersion2.getStatus() == FileStatus.DELETED) {
					logger.log(Level.INFO, "     - (7) Original does NOT match, target file does not exist (and SHOUDN'T): Nothing to do!");					
				}
				else {
					logger.log(Level.INFO, "     - (8) Original does NOT match, target file does not exist: Creating file at: "+fileVersion2);
					createFileFolderOrSymlink(fileVersion2);
				}
			}
		}
	}

	@Override
	public String toString() {
		return "ChangeFileSystemAction [file1=" + fileVersion1 + ", file2=" + fileVersion2 + "]";
	}				
}	
