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

import org.syncany.config.Config;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.Assembler;
import org.syncany.util.NormalizedPath;

public abstract class FileCreatingFileSystemAction extends FileSystemAction {
	private SqlDatabase localDatabase;
	private Assembler assembler;
	
	public FileCreatingFileSystemAction(Config config, MemoryDatabase winningDatabase, FileVersion file1, FileVersion file2) {
		super(config, winningDatabase, file1, file2);		
		
		this.localDatabase = new SqlDatabase(config);
		this.assembler = new Assembler(config, localDatabase, winningDatabase);
	}

	protected void createFileFolderOrSymlink(FileVersion reconstructedFileVersion) throws Exception {
		if (reconstructedFileVersion.getType() == FileType.FILE) {
			createFile(reconstructedFileVersion);
		}
		else if (reconstructedFileVersion.getType() == FileType.FOLDER) {
			createFolder(reconstructedFileVersion);
		}
		else if (reconstructedFileVersion.getType() == FileType.SYMLINK) {
			createSymlink(reconstructedFileVersion);
		}
		else {
			logger.log(Level.INFO, "     - Unknown file type: " + reconstructedFileVersion.getType());
			throw new Exception("Unknown file type: " + reconstructedFileVersion.getType());
		}
	}

	protected void createFolder(FileVersion targetFileVersion) throws Exception {
		NormalizedPath targetDirPath = new NormalizedPath(config.getLocalDir(), targetFileVersion.getPath());
		
		logger.log(Level.INFO, "     - Creating folder at " + targetFileVersion + " ...");
		
		try {
			// Clean filename
			if (targetDirPath.hasIllegalChars()) {
				targetDirPath = targetDirPath.toCreatable("filename conflict", true);
				logger.log(Level.INFO, "     - Had illegal chars, cleaned to "+targetDirPath);
			}

			// Try creating it
			createFolder(targetDirPath);
			setFileAttributes(targetFileVersion, targetDirPath.toFile());
		}
		catch (Exception e) {
			throw new RuntimeException("What to do here?!", e);
		}
	}

	protected void createFile(FileVersion reconstructedFileVersion) throws Exception {
		File reconstructedFileInCache = assembleFileToCache(reconstructedFileVersion);		
		moveFileToFinalLocation(reconstructedFileInCache, reconstructedFileVersion);	
	}
	
	protected File assembleFileToCache(FileVersion reconstructedFileVersion) throws Exception {
		File reconstructedFileInCache = assembler.assembleToCache(reconstructedFileVersion);
		 
		setFileAttributes(reconstructedFileVersion, reconstructedFileInCache);
		setLastModified(reconstructedFileVersion, reconstructedFileInCache);
		
		return reconstructedFileInCache;
	}	
}
