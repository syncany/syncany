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
package org.syncany.index;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.Constants;
import org.syncany.config.Profile;
import org.syncany.db.CloneFile;
import org.syncany.db.Database;
import org.syncany.index.requests.CheckIndexRequest;
import org.syncany.index.requests.DeleteIndexRequest;
import org.syncany.util.FileLister;
import org.syncany.util.FileLister.FileListerAdapter;
import org.syncany.util.FileUtil;

/**
 * Indexes new and changed files and adds corresponding database entries
 * if necessary. The indexer is mainly called by the {@link Watcher} inside the
 * {@link Application} object.
 *
 * <p>It mainly consists of a request queue and one worker thread that handles
 * events such as new, changed, renamed or deleted files or folders.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Indexer {
    private static final Logger logger = Logger.getLogger(Indexer.class.getSimpleName());
    private static Indexer instance;
    
    private Database db;

    private Indexer() {
        if (logger.isLoggable(Level.INFO)) {
            logger.info("Creating indexer ...");
        }
        
        this.db = Database.getInstance();
    } 

	public static synchronized Indexer getInstance() {
        if (instance == null) {
            instance = new Indexer();
        }
        
        return instance;
    }

    public void index(Profile profile) {
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Reading folders in profile ''{0}'' ...", profile.getName());
        }
                
        File rootFolder = profile.getRoot();
        
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "- Folder {0} ...", rootFolder);
        }            
        
        
        // Check for files that do NOT exist anymore
        List<CloneFile> dbFiles = db.getFiles();
        
        // only iterates if db exists with files listed in there, otherwise does nothing        
        for (CloneFile dbFile : dbFiles) {
            if (!dbFile.getFile().exists()) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, "  File {0} does NOT exist anymore. Marking as deleted.", dbFile.getFile());
                }
                   
                new DeleteIndexRequest(dbFile).process();
            }
        }

        // checking all files of rootfolder
        new FileLister(rootFolder, new FileListerListenerImpl(this, true)).start();
        
        
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Startup indexing of profile {0} finished.", profile);
        }  
    }
    
    public void index(File file) {
        new CheckIndexRequest(file).process();
    }   
    
    public static class FileListerListenerImpl extends FileListerAdapter {
        private Indexer indexer;
        private boolean deleteIgnoreFiles;

        public FileListerListenerImpl(Indexer indexer, boolean deleteIgnoreFiles) {
            this.indexer = indexer;
            this.deleteIgnoreFiles = deleteIgnoreFiles;
        }

        @Override
        public void proceedFile(File file) {
            System.err.println(file.getAbsoluteFile());
            indexer.index(file);
        }

        @Override
        public void enterDirectory(File directory) {
            indexer.index(directory);
        }

        @Override
        public boolean directoryFilter(File directory) {
            if (directory.getName().startsWith(Constants.FILE_IGNORE_PREFIX)) {
                if (deleteIgnoreFiles) {
                    FileUtil.deleteRecursively(directory);
                }

                return false;
            }

            return true;
        }

        @Override
        public boolean fileFilter(File file) {
            if (file.getName().startsWith(Constants.FILE_IGNORE_PREFIX)) {
                if (deleteIgnoreFiles) {
                    FileUtil.deleteRecursively(file);
                }

                return false;
            }

            return true;
        }
    }
}
