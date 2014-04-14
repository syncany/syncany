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

import java.io.File;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.syncany.config.Config;
import org.syncany.database.FileVersion;
import org.syncany.database.MemoryDatabase;

public class RenameFileSystemAction extends FileSystemAction {
	public RenameFileSystemAction(Config config, FileVersion from, FileVersion to, MemoryDatabase winningDatabase) {
		super(config, winningDatabase, from, to);
	}

	@Override
	public void execute() throws Exception {		
		File fromFileOnDisk = getAbsolutePathFile(fileVersion1.getPath());
		File toFileOnDisk = getAbsolutePathFile(fileVersion2.getPath());			
		
		boolean fromFileExists = fromFileOnDisk.exists();
		boolean toFileExists = toFileOnDisk.exists();
		
		boolean fileRenamed = !toFileOnDisk.equals(fromFileOnDisk);
				
		if (fileRenamed) {
			if (fromFileExists && !toFileExists) { 
				if (fileAsExpected(fileVersion1)) { // << Expected case!
					logger.log(Level.INFO, "     - (1) Renaming file "+fromFileOnDisk+" to "+toFileOnDisk+" ...");				
					FileUtils.moveFile(fromFileOnDisk, toFileOnDisk);
				}
				else {
					logger.log(Level.INFO, "     - (2) Source file differs from what we expected.");
					throw new Exception("Source file differs from what we expected.");
				}
			}
			else if (fromFileExists && toFileExists) {
				if (fileAsExpected(fileVersion1)) {
					if (fileAsExpected(fileVersion2)) {
						logger.log(Level.INFO, "     - (3) File at destination is what was expected. Nothing to do for "+toFileOnDisk+" ...");
					}
					else {
						logger.log(Level.INFO, "     - (4) File at destination differs, creating conflict file for "+toFileOnDisk+" ...");
						
						moveToConflictFile(fileVersion2);
						FileUtils.moveFile(fromFileOnDisk, toFileOnDisk);
					}
				}
				else {
					logger.log(Level.INFO, "     - (5) Cannot rename because orig. file does not exist.");
					throw new Exception("Cannot rename because orig. file does not exist");
				}			
			}		
			else if (!fromFileExists && !toFileExists) {
				logger.log(Level.INFO, "     - (6) Cannot rename because orig. file does not exist.");
				throw new Exception("Cannot rename because orig. file does not exist");
			}
			else if (!fromFileExists && toFileExists) {
				if (fileAsExpected(fileVersion2)) {
					logger.log(Level.INFO, "     - (7) File at destination is what was expected. Nothing to do for "+toFileOnDisk+" ...");
				}
				else {
					logger.log(Level.INFO, "     - (8) Cannot rename because orig. file does not exist.");
					throw new Exception("Cannot rename because orig. file does not exist");
				}
			}	
		}
		
		// Set attributes
		setFileAttributes(fileVersion2); // TODO [low] check for fileAsExpected
		setLastModified(fileVersion2);
	}	
	
	@Override
	public String toString() {
		return "RenameFileSystemAction [file1=" + fileVersion1 + ", file2=" + fileVersion2 + "]";
	}

	
}