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
package org.syncany.watch.remote;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Profile;
import org.syncany.db.CloneChunk;
import org.syncany.db.CloneFile;
import org.syncany.db.CloneFile.Status;
import org.syncany.db.CloneFile.SyncStatus;
import org.syncany.db.Database;
import org.syncany.exceptions.InconsistentFileSystemException;
import org.syncany.index.Indexer;
import org.syncany.util.FileUtil;

/**
 *
 * @author Philipp C. Heckel
 */
public class ChangeManager {
    private static final Logger logger = Logger.getLogger(ChangeManager.class.getSimpleName());

    private List<FileHistoryPart> updatedFiles;

    private Database db;
    private Indexer indexer;

    public ChangeManager(Profile profile) {
        db = Database.getInstance();
        indexer = Indexer.getInstance();
    }

    public void processUpdates(UpdateQueue ul) throws InconsistentFileSystemException {
    	// set tray icon to UPDATING state was here
        
        // Reset
        updatedFiles = new ArrayList<FileHistoryPart>();
        
        PriorityQueue<FileHistoryPart> histories = ul.getQueue();
        ul.dump();
        
        FileHistoryPart history;
        
        while (null != (history = histories.poll())) {
        	System.out.println("Processing History..");
            processHistory(history);
        }
    }    
        
    private void processHistory(FileHistoryPart history) throws InconsistentFileSystemException {
        FileUpdate lastUpdate = history.getLastUpdate();                
        FileUpdate lastLocalUpdate = history.getLastLocalUpdate();     

        //System.err.println("last upd: "+lastUpdate+" -- lastlocal: "+lastLocalUpdate);
        
        // Do branch first
        if (history.getBranchHistory() != null) {
            processHistory(history.getBranchHistory());
        }

        // Skip unchanged entries
        if (lastLocalUpdate != null && lastLocalUpdate.equals(lastUpdate)) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "File {0} has not changed. Skipping.", history.getFileId());                    
            }

            return;
        }
        
        File rootFolder = Profile.getInstance().getRoot();
       
        
        // TODO: correct?
        
        // Check inconsistencies
        File newFile = new File(rootFolder+File.separator+lastUpdate.getPath()+File.separator+lastUpdate.getName());
        
        
        // changed due to profile deletion (nh)
        CloneFile lastLocalFile = db.getFileOrFolder(lastUpdate.getFileId());
        if ((lastLocalFile != null && lastLocalUpdate == null)
                || (lastLocalFile == null && lastLocalUpdate != null)
                || (lastLocalFile != null && lastLocalUpdate != null && lastLocalUpdate.getVersion() != lastLocalFile.getVersion())) {

            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Inconsistency: lastLocalFile = {0} vs. lastLocalUpdate = {1} ...", new Object[]{lastLocalFile, lastLocalUpdate});
            } 
            
            //indexer.index(lastLocalFile.getFile());
            //throw new InconsistentFileSystemException("File #"+lastLocalFile.getFileId()+" / "+lastLocalFile.getFile()+" is out-of-sync.");
        }               


        /// DELETE

        if (lastUpdate.getStatus() == Status.DELETED) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Deleting file {1} ...", new Object[]{lastUpdate});
            }                        

            if (lastLocalFile != null) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, "Deleting old file {0} ...", lastLocalFile.getFile());
                }                        

                if (newFile.exists()) {
                    // Regular file
                    if (!lastUpdate.isFolder()) {
                        // Check inconsistencies
                        if (lastLocalFile.getFile().length() != lastLocalFile.getFileSize()) {
                            indexer.index(lastLocalFile.getFile());
                            throw new InconsistentFileSystemException("Could not delete file #"+lastLocalFile.getFileId()+" / "+lastLocalFile.getFile()+". Out-of-sync.");
                        }

                        // Delete it!
                        if (!FileUtil.deleteVia(lastLocalFile.getFile())) {
                            indexer.index(lastLocalFile.getFile());
                            throw new InconsistentFileSystemException("Could not delete file #"+lastLocalFile.getFileId()+" / "+lastLocalFile.getFile()+". Out-of-sync.");
                        }                                   
                    }

                    // Folder
                    else {
                        // Try to delete; if not successful, that means there is stuff in the folder
                        // We treat this as new folder and re-index it!
                        if (lastLocalFile.getFile().list().length == 0) {
                            lastLocalFile.getFile().delete();                                   
                        }
                        else {
                            indexer.index(lastLocalFile.getFile());
                        }
                    }
                }
            }

            addToDB(history);                    
        }

        /// MERGE
        else if (lastUpdate.getStatus() == Status.MERGED) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Merged entry. Just add to DB.");
            }

            addToDB(history);                    
        }

        /// RENAME / MOVE
        else if (lastLocalFile != null && Arrays.equals(lastUpdate.getChecksum(), lastLocalFile.getChecksum())) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Renaming {0} to {1} ...", new Object[]{lastLocalFile.getFile(), newFile});
            }

            // Special case: source and dest file have the same name
            if (lastLocalFile.getFile().equals(newFile)) {
                // Nothing to do.
            }                                        

            // Folder
            else if (lastUpdate.isFolder()) {
                // Create destination directory (if non-existant)
                if (!newFile.exists()) {
                    if (!FileUtil.mkdirsVia(newFile)) {
                        throw new InconsistentFileSystemException("Could not rename file #"+lastLocalFile.getFileId()+" / "+lastLocalFile.getFile()+" to "+newFile+". Out-of-sync.");
                    }    
                }

                newFile.setLastModified(lastUpdate.getLastModified().getTime());                                

                // Delete source directory (if empty)
                if (lastLocalFile.getFile().exists()) {
                    if (lastLocalFile.getFile().list().length == 0) {
                        lastLocalFile.getFile().delete();                                   
                    }
                    else {
                        indexer.index(lastLocalFile.getFile());
                    }                                                                                    
                }                                
            }

            // Regular file        
            else {
                // File exists
                if (newFile.exists()) {
                    // TODO do checksum check instead?!
                    if (newFile.length() != lastUpdate.getFileSize() ||
                            newFile.lastModified() != lastUpdate.getLastModified().getTime()) {

                        throw new InconsistentFileSystemException("Could not rename file #"+lastLocalFile.getFileId()+" / "+lastLocalFile.getFile()+" to "+newFile+". Out-of-sync.");
                    }

                    // File exists. We don't have to do anything!
                }

                // File does not exist
                else {
                    // Create parent (if necessary)
                    if (!FileUtil.mkdirsVia(newFile.getParentFile())) {
                        throw new InconsistentFileSystemException("Could not create folder "+lastLocalFile.getFile().getParentFile()+". Out-of-sync.");
                    }

                    // Do rename!
                    if (!FileUtil.renameVia(lastLocalFile.getFile(), newFile)) {
                        throw new InconsistentFileSystemException("Could not rename file #"+lastLocalFile.getFileId()+" / "+lastLocalFile.getFile()+" to "+newFile+". Out-of-sync.");
                    }
                }
            }

            addToDB(history);                    
        }


        // NEW + CHANGE 

        else {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Processing new or changed file {0} ...", new Object[]{newFile});
            }
            
            // Folder
            if (lastUpdate.isFolder()) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, "Creating folder ID #{0} / {1} ...", new Object[]{lastUpdate.getFileId(), newFile});
                }                                   

                if (!newFile.exists()) {
                    if (!FileUtil.mkdirsVia(newFile)) {
                        throw new InconsistentFileSystemException("Could not create folder: "+newFile+"; Permission problem?");
                    }
                }

                addToDB(history);
            }

            // Is file: Download!
            else {
                boolean fileAssembled = false;
                File tempAssembledFile;                
                
                do {
                    // If the file exists, check whether or not it is the expected file
                    // or the destination file; or a conflict ...
                    if (newFile.exists()) {
                        // Is expected file (= the old file that will be updated)
                        if (lastLocalFile != null && lastLocalFile.getFileSize() == newFile.length()
                                && lastLocalFile.getLastModified().getTime() == newFile.lastModified()) {

                            // This is expected. 
                            // File will be downloaded below.

                            if (logger.isLoggable(Level.INFO)) {
                                logger.log(Level.INFO, "Expected file found. ID #{0} / {1}", new Object[]{lastUpdate.getFileId(), newFile});
                            }                         
                        }

                        // Is destination file (= the file that we were about to download)
                        else if (lastUpdate.getFileSize() == newFile.length() 
                                && lastUpdate.getLastModified().getTime() == newFile.lastModified()) {

                            // Assume this is the same file: just add to DB
                            if (logger.isLoggable(Level.INFO)) {
                                logger.log(Level.INFO, "File exists locally. Nothing to do. ID #{0} / {1}", new Object[]{lastUpdate.getFileId(), newFile});
                            }                    

                            addToDB(history);          
                            return;
                        }

                        // Some random file (= CONFLICT)
                        else {                       
                            File conflictFile = new File(rootFolder+File.separator+lastUpdate.getPath()+File.separator+lastUpdate.getConflictedCopyName());

                            if (logger.isLoggable(Level.INFO)) {
                                logger.log(Level.INFO, "- Conflict detected. Length or checksum mismatch. Creating {0} ...", new Object[]{conflictFile});
                            }


                            if (!FileUtil.renameVia(newFile, conflictFile)) {
                                throw new InconsistentFileSystemException("Could not apply update for file #"+lastUpdate.getFileId()+": "+lastUpdate+". Out-of-sync.");    
                            }

                            
                            if (true) {System.out.println("FIXME FIXME"); System.exit(0); } //indexer.queue(conflictFile);
                        }
                    }

                    // Instead of downloading, try to find the file
                    // locally; pick the chunks from other files if necessary.

                    // TODO implement this -- performance!


                    // Okay. Now download it!
                    if (logger.isLoggable(Level.INFO)) {
                        logger.log(Level.INFO, "Downloading file #{0} to {1} ...", new Object[]{lastUpdate.getFileId(), newFile});
                    }                    

                    // Get chunks 
                    NavigableMap<Long, FileUpdate> relevantUpdates;
                    List<String> chunkIdStrs = new ArrayList<String>();

                    if (lastLocalFile != null) {
                        List<CloneChunk> chunks = lastLocalFile.getChunks();

                        for (CloneChunk chunk : chunks) {
                            chunkIdStrs.add(chunk.getIdStr());
                        }

                        relevantUpdates = history.getHistory().subMap(lastLocalFile.getVersion(), false, history.getHistory().lastKey(), true);
                    }

                    else if (history.getFirstUpdate().getVersion() == 1) {
                        relevantUpdates = history.getHistory();
                    }
                    else {
                        throw new InconsistentFileSystemException("Incomplete update list: unable to determine chunk list of file #"+lastUpdate.getFileId());
                    }

                    for (FileUpdate u : relevantUpdates.values()) {
                        if (u.getChunksChanged().size() > 0) {
                            for (Map.Entry<Integer, String> changedChunk : u.getChunksChanged().entrySet()) {
                                chunkIdStrs.set(changedChunk.getKey(), changedChunk.getValue());
                            }
                        }

                        if (u.getChunksAdded().size() > 0) {
                            for (String chunkIdStr : u.getChunksAdded()) {
                                chunkIdStrs.add(chunkIdStr);
                            }
                        }
                        else if (u.getChunksRemoved() > 0) {
                            for (int i=0;i<u.getChunksRemoved();i++) {
                                chunkIdStrs.remove(chunkIdStrs.size()-1);
                            }
                        }                            
                    }

                    // Download and assemble to temp file
                    try {
                        tempAssembledFile = Profile.getInstance().getCache().createTempFile("assemble-"+lastUpdate.getName());
                    }                    
                    catch (Exception e) {
                        throw new InconsistentFileSystemException("Could not create temp file in cache.");
                    }

                   if (true) {System.out.println("FIXME FIXME"); System.exit(0); } //assembler.assembleFile(chunkIdStrs, tempAssembledFile);

                    if (!fileAssembled) {
                        fileAssembled = true;
                        continue;
                    }
                } while (!fileAssembled);
                     
                // Another consistency round
                File destFolder = newFile.getParentFile(); 
                
                if (!tempAssembledFile.setLastModified(lastUpdate.getLastModified().getTime())) {
                    tempAssembledFile.delete();
                    throw new InconsistentFileSystemException("Could not set last modified time for "+tempAssembledFile+". Out-of-sync?");
                }

                if (!FileUtil.mkdirsVia(destFolder)) {
                    tempAssembledFile.delete();
                    throw new InconsistentFileSystemException("Could not create folder: "+destFolder+"; Permission problem?");
                }
                
                if (newFile.exists()) {
                    FileUtil.deleteVia(newFile);
                }              

                if (!FileUtil.renameVia(tempAssembledFile, newFile)) {
                    tempAssembledFile.delete();
                    throw new InconsistentFileSystemException("Could not rename temp file "+tempAssembledFile+" to file "+newFile+"; Enough disk space?");
                }               

                // Now delete the old file
                if (lastLocalFile != null && lastLocalFile.getFile().exists() && !lastLocalFile.getFile().equals(newFile)) {
                    FileUtil.deleteVia(lastLocalFile.getFile());
                }

                addToDB(history);                    
            }
        }

        // Poke file
        if (newFile.exists()) {
           // desktop.touch(newFile) file was here
        }                

        // Notification
        updatedFiles.add(history);   
    }

    private void addToDB(FileHistoryPart history) throws InconsistentFileSystemException {
        // Prune conflicting stuff from DB
        if (history.getPruneHistory() != null) {
            for (FileUpdate pruneUpdate : history.getPruneHistory().getHistory().values()) {
            	// changed due to profile deletion
                CloneFile pruneFile = db.getFileOrFolder(pruneUpdate.getFileId(), pruneUpdate.getVersion());
                
                if (pruneFile == null) {
                    // Ignore non-existing entry.
                    continue;
                }
                
                // TODO Implement remove of db file clonefiles remove from tree!!!
                pruneFile.remove();
            }            
        }        
        
        // Add relevant updates to DB (= whole history or parts of it)
        NavigableMap<Long, FileUpdate> relevantUpdates = history.getNewLocalUpdates();

        for (FileUpdate u : relevantUpdates.values()) { 
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Adding to DB: {0}.", u);
            }              

            // changed due to elemination of profile class
            db.createFile(u, SyncStatus.UPTODATE);
        }
    }    
}
