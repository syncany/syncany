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

import java.util.List;

import org.syncany.communication.CommunicationController;
import org.syncany.communication.CommunicationController.SyncanyStatus;
import org.syncany.config.Profile;
import org.syncany.db.CloneFile;
import org.syncany.index.requests.CheckIndexRequest;
import org.syncany.index.requests.DeleteIndexRequest;
import org.syncany.index.requests.IndexRequest;
import org.syncany.index.requests.MoveIndexRequest;
import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.syncany.Application;
import org.syncany.Constants;
import org.syncany.db.Database;
import org.syncany.util.FileLister;
import org.syncany.util.FileUtil;
import org.syncany.util.FileLister.FileListerAdapter;

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
    private BlockingQueue<IndexRequest> queue;
    private Thread worker;

    private Indexer() {
        if (logger.isLoggable(Level.INFO)) {
            logger.info("Creating indexer ...");
        }
        
        this.db = Database.getInstance();
        this.queue = new LinkedBlockingQueue<IndexRequest>();
        this.worker = null; // cp. start()
    }
    
    public BlockingQueue<IndexRequest> getQueue() {
		return queue;
	}

	public static synchronized Indexer getInstance() {
        if (instance == null) {
            instance = new Indexer();
        }
        
        return instance;
    }

    public synchronized void start() {
        // Already running!
        if (worker != null) {
            return;
        }
        
        // Start it
        if (logger.isLoggable(Level.INFO)) {
            logger.info("Starting indexer thread ...");
        }
        
        worker = new Thread(new IndexWorker(), "Indexer");
        worker.start();
    }

    public synchronized void stop() {
        if (worker == null) {
            return;
        }
        
        if (logger.isLoggable(Level.INFO)) {
            logger.info("Stopping indexer thread ...");
        }
        
        worker.interrupt();
        worker = null;
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
        // setStatus to inSync = up-to-date
        CommunicationController.getInstance().updateStatus(SyncanyStatus.inSync);
    }
    
    public void index(File file) {
        new CheckIndexRequest(file).process();
    }
    
    /**
     * Check database to find matches for the given file. If no matches
     * or previous versions are found, the file is re-indexed completely.
     * 
     * @param file
     */
    public void queue(File file) {
        queue.add(new CheckIndexRequest(file));
    }

    /**
     * Adjusts the entry of a file that has been moved.
     * @param fromFile
     * @param toFile
     */
    public void queueMoved(File fromFile, File toFile) {
        queue.add(new MoveIndexRequest(fromFile, toFile));
    }

    public void queueDeleted(File file) {
        queue.add(new DeleteIndexRequest(file));
    }

    private class IndexWorker implements Runnable {
        @Override
        public void run() {
            try {
                IndexRequest req;
                
                while (null != (req = queue.take())) {
                    if (logger.isLoggable(Level.INFO)) {
                        logger.log(Level.INFO, "Processing request {0}", req);
                    }
                    
                    req.process();
                }
            }
            catch (InterruptedException ex) {
                logger.log(Level.WARNING, "Indexer interrupted. EXITING.");
                return;
            }
        }
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
