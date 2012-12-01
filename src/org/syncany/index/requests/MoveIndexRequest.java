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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.syncany.Constants;
import org.syncany.db.CloneFile;
import org.syncany.db.CloneFile.Status;
import org.syncany.util.FileLister;
import org.syncany.util.FileUtil;

/**
 *
 * @author Philipp C. Heckel
 */
public class MoveIndexRequest extends IndexRequest {
    private static final Logger logger = Logger.getLogger(MoveIndexRequest.class.getSimpleName());
    
    private CloneFile dbFromFile;
    
//    private File fromRoot;
    private File fromFile;
    
//    private Folder toRoot;
    private File toFile;

    public MoveIndexRequest(File fromFile, File toFile) {
        super();

        this.fromFile = fromFile;
        this.toFile = toFile;
    }

    public MoveIndexRequest(CloneFile dbFromFile, File toFile) {
        this(dbFromFile.getFile(), toFile);

        this.dbFromFile = dbFromFile;
    }

    /*
     * TODO moving a file from one ROOT to another does not work (cp. database!)
     * 
     */
    @Override
    public void process() {
        // .ignore file
        if (fromFile.getName().startsWith(Constants.FILE_IGNORE_PREFIX) 
                || toFile.getName().startsWith(Constants.FILE_IGNORE_PREFIX)) {
            
            return;
        }
        
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Indexer: Updating moved file {0} TO {1}", new Object[]{fromFile.getAbsolutePath(), toFile.getAbsolutePath()});
        }
        
        // Look for file in DB
        if (dbFromFile == null) {
            dbFromFile = db.getFileOrFolder(fromFile);
            
            // No file found in DB.
            if (dbFromFile == null) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.log(Level.WARNING, "Indexer: Source file not found in DB ({0}). Indexing {1} as new file.", new Object[]{fromFile.getAbsolutePath(), toFile.getAbsolutePath()});
                }

                new CheckIndexRequest(toFile).process();
                return;
            }
        }

        // Parent 
        String relToParentFolder = FileUtil.getRelativeParentDirectory(Profile.getInstance().getRoot(), toFile);
        String absToParentFolder = FileUtil.getAbsoluteParentDirectory(toFile);

        // File found in DB.
        CloneFile dbToFile = (CloneFile) dbFromFile.clone();

        // Updated changes
//        dbToFile.setRoot(toRoot);
        dbToFile.setLastModified(new Date(toFile.lastModified()));
        dbToFile.setPath(relToParentFolder);
        dbToFile.setName(toFile.getName());
        dbToFile.setFileSize((toFile.isDirectory()) ? 0 : toFile.length());
        dbToFile.setVersion(dbToFile.getVersion()+1);
        dbToFile.setUpdated(new Date());
        dbToFile.setStatus(Status.RENAMED);
        dbToFile.setSyncStatus(CloneFile.SyncStatus.UPTODATE);
        dbToFile.setClientName(Profile.getInstance().getName());
        
        dbToFile.setPrevious(dbFromFile);
        dbFromFile.setNext(dbToFile);
        
        dbToFile.persist();
        dbFromFile.merge();
        
        // Notify file manager
        // desktop.touch(dbToFile.getFile()) was here
	    
        
        // Update children (if directory) -- RECURSIVELY !!
        if (dbToFile.isFolder()) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Indexer: Updating CHILDREN of {0} ...", toFile);
            }
            
            // Update database children
            final List<CloneFile> children = db.getChildren(dbFromFile);
            final Set<File> childrenFileList = new HashSet<File>();
            
            for (CloneFile child : children) {
                File childFromFile = child.getFile();
                File childToFile = new File(absToParentFolder+File.separator+toFile.getName()+File.separator+child.getName());

                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, "Indexer: Updating children of moved file {0} TO {1}", new Object[]{childFromFile.getAbsolutePath(), childToFile.getAbsolutePath()});
                }
                
                // Fill hash set
                childrenFileList.add(childToFile);
                
                // Do it!
                new MoveIndexRequest(childFromFile, childToFile).process();
            }
            
            // Find files recursively
            new FileLister(toFile, new FileLister.FileListerAdapter() {
                @Override
                public void proceedFile(File f) {
                    if (!childrenFileList.contains(f)) {
                        new CheckIndexRequest(f).process();
                    }
                }
            }).start();
        }
    }
}