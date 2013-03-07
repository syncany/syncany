/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.index.requests;

import java.util.logging.Level;
import org.syncany.config.Profile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;
import org.syncany.Constants;

/**
 *
 * @author Philipp C. Heckel
 */
public class CheckIndexRequest extends IndexRequest {
    private static final Logger logger = Logger.getLogger(CheckIndexRequest.class.getSimpleName());    
    
    private File file;

    public CheckIndexRequest(File file) {
        this.file = file;
    }

    public File getFile() {
		return file;
	}

	@Override
    public void process() {
        // .ignore file
        if (file.getName().startsWith(Constants.FILE_IGNORE_PREFIX)) {
            return;
        }
        
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Indexer: Checking file {0} ... ", file.getAbsolutePath());
        }
                
        // Ignore if non-existant
        if (!file.exists()) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Indexer: File does not exist: {0}. IGNORING.", file.getAbsolutePath());
            }

            return;
        }

        // Folder
        if (file.isDirectory()) {
            processFolder();
        }

        // File
        else if (file.isFile()) {
            processFile();
        }
        
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Checking file DONE: {0}", file.getAbsolutePath());
        }        
    }

    private void processFolder() {
        CloneFile dbFile = db.getFolder(file);

        // Matching DB entry found
        if (dbFile != null) {
            logger.info("Folder FOUND in DB. Nothing to do.");
            return;
        }

        // Add as new
        logger.info("Folder NOT found in DB. Adding as new file.");
        new NewIndexRequest(file, null).process();
    }

    private void processFile() {
        // Find file in DB
        CloneFile dbFile = db.getFile(file);
        
        // Matching DB entry found; Now check filesize and time
        if (dbFile != null) {
            boolean isSameFile = Math.abs(file.lastModified() - dbFile.getLastModified().getTime()) < 500
                && file.length() == dbFile.getFileSize();

            if (isSameFile) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info("File found in DB. Same modified date, same size. Nothing to do!");
                }
                
                return;
            }

            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "File found, but modified date or size differs. Indexing as CHANGED file.");
                logger.log(Level.INFO, "-> fs = ({0}, {1}), org.syncany.db = ({2}, {3})", new Object[]{file.lastModified(), file.length(), dbFile.getLastModified().getTime(), dbFile.getFileSize()});
            }
            
            new NewIndexRequest(file, dbFile).process();
            return;
        }

        // No match in DB found, try to find a 'close' entry with matching checksum
        else if (dbFile == null) {
            // Find checksum of file; 
            byte[] checksum;
            
            try {
                // TODO This is inefficient, if the file is 'new', since the NewIndexRequest (below)
                // TODO does create checksums for all the chunks again!
                checksum = Profile.getInstance().getRepository().getChunker().createChecksum(file);
            }
            catch (FileNotFoundException e) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.log(Level.WARNING, "Could not create checksum of "+file+". File not found. IGNORING.", e);
                }
                
                return;                
            }
            
            catch (IOException e) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.log(Level.WARNING, "Could not create checksum of "+file+". File not found. IGNORING.", e);
                }
                
                return;                
            }            
            
            // Guess nearest version (by checksum and name)
            CloneFile guessedPreviousVersion = db.getNearestFile(file, checksum);

            if (guessedPreviousVersion != null) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, "Previous version GUESSED by checksum and name: {0}; Updating DB ...", guessedPreviousVersion.getFile().getAbsolutePath());
                }
                
                new MoveIndexRequest(guessedPreviousVersion, file).process();
                return;
            }

            else {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info("No previous version found. Adding new file ...");
                }
                
                new NewIndexRequest(file, null).process();
                return;
            }
        }
  
    }
}
