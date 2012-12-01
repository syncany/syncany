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
package org.syncany.watch.local;

import org.syncany.db.Database;
import org.syncany.index.Indexer;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.syncany.Constants;

/**
 *
 * @author oubou68, pheckel
 */
public abstract class LocalWatcher {
    protected static final Logger logger = Logger.getLogger(LocalWatcher.class.getSimpleName());
    protected static LocalWatcher instance;
    protected Indexer indexer;

    public LocalWatcher() {
        if (logger.isLoggable(Level.INFO)) {
            logger.info("Creating watcher ...");
        }

        initDependencies();
    }

    private void initDependencies() {
        indexer = Indexer.getInstance();
    }

    public void queueCheckFile(File file) {
        // Exclude ".ignore*" files from everything
        if (file.getName().startsWith(Constants.FILE_IGNORE_PREFIX)) {
            //logger.info("Watcher: Ignoring file "+file.getAbsolutePath());
            return;
        }
        
        // File vanished!
        if (!file.exists()) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Watcher: File {0} vanished. IGNORING.", file);
            }
            
            return;
        }

        // Add to queue
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Watcher: Checking new/modified file {0}", file);
        }
        
        //Database.getInstance().incrementVersion();

        indexer.queue(file);            
    }

    public void queueMoveFile(File fromFile, File toFile) {
        // Exclude ".ignore*" files from everything
        if (fromFile.getName().startsWith(Constants.FILE_IGNORE_PREFIX) || toFile.getName().startsWith(Constants.FILE_IGNORE_PREFIX)) {
            
            //logger.info("Watcher: Ignoring file "+fromFile.getAbsolutePath());
            return;
        }
        
        // File vanished!
        if (!toFile.exists()) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Watcher: File {0} vanished. IGNORING.", toFile);
            }
            
            return;
        }

        // Add to queue
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Watcher: Moving file {0} TO {1}", new Object[]{fromFile, toFile});
        }
        
       // Database.getInstance().incrementVersion();
        
        indexer.queueMoved(fromFile, toFile);
    }

    public void queueDeleteFile(File file) {
        // Exclude ".ignore*" files from everything
        if (file.getName().startsWith(Constants.FILE_IGNORE_PREFIX)) {
            //logger.info("Watcher: Ignoring file "+file.getAbsolutePath());
            return;
        }

        // Add to queue
        if (logger.isLoggable(Level.INFO)) {        
            logger.log(Level.INFO, "Watcher: Deleted file {0}", file);
        }
        
        //Database.getInstance().incrementVersion();
        
        indexer.queueDeleted(file);
    }

    public static synchronized LocalWatcher getInstance() {
        if (instance != null) {
            return instance;
        }

            
        instance = new CommonLocalWatcher(); 
        return instance;
    }

    public abstract void start();

    public abstract void stop();

    public abstract void watch();

    public abstract void unwatch();
}
