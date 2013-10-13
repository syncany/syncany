package org.syncany.operations.actions;

import java.util.logging.Level;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;

public class ChangeFileSystemAction extends FileSystemAction {
	public ChangeFileSystemAction(Config config, FileVersion fromFileVersion, FileVersion toFileVersion, Database localDatabase, Database winningDatabase) {
		super(config, localDatabase, winningDatabase, fromFileVersion, toFileVersion);
	}
	
	@Override
	public void execute() throws Exception {
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
				createConflictFile(fileVersion2);
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
				
				createConflictFile(fileVersion2);
				createFileFolderOrSymlink(fileVersion2);	
			}
			else {
				logger.log(Level.INFO, "     - (7) Original does NOT match, target file does not exist: Creating file at: "+fileVersion2);
				createFileFolderOrSymlink(fileVersion2);	
			}
		}
	}

	@Override
	public String toString() {
		return "ChangeFileSystemAction [file1=" + fileVersion1 + ", file2=" + fileVersion2 + "]";
	}				
}	
