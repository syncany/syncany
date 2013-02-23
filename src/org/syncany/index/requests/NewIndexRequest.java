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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

import org.syncany.Constants;
import org.syncany.chunk.Chunk;
import org.syncany.chunk.CustomMultiChunk;
import org.syncany.chunk.MultiChunk;
import org.syncany.config.Profile;
import org.syncany.db.CloneChunk;
import org.syncany.db.CloneFile;
import org.syncany.db.CloneFile.Status;
import org.syncany.db.Database;

/**
 *
 * @author Philipp C. Heckel
 */
public class NewIndexRequest extends IndexRequest {
    private static final Logger logger = Logger.getLogger(NewIndexRequest.class.getSimpleName());
    private static final long METACHUNK_UNFINISHED_TIMEOUT = 2000;
    private static final Semaphore metaChunkMutex = new Semaphore(1);
    
    private static MultiChunk multiChunk;
    private static Profile metaChunkProfile;
    private static File metaChunkTempFile;
    private static Set<CloneFile> metaChunkCompletedFiles;
    
    private static Timer metaChunkTimeoutTimer = new Timer();
    private static TimerTask metaChunkTimoutTimerTask;    

    private File file;
    private CloneFile previousVersion;    

    public NewIndexRequest(File file, CloneFile previousVersion) {
        this.file = file;        
        this.previousVersion = previousVersion;        
    }

    @Override
    public void process() {
        // .ignore file
        if (file.getName().startsWith(Constants.FILE_IGNORE_PREFIX)) {
            return;
        }
        
        // File vanished
        if (!file.exists()) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Indexer: Error indexing file {0}: File does NOT exist. Ignoring.", file);
            }
            
            return;
        }
        
        // Create DB entry
        CloneFile newVersion = (previousVersion == null) ? addNewVersion() : addChangedVersion();                      
        
        // Process folder and files differently
        if (file.isDirectory()) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Indexer: Indexing new FOLDER {0} ...", file);
            }

            processFolder(newVersion);
        }
        
        else if (file.isFile()) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Indexer: Indexing new file {0} ...", file);
            }
            
            /*
            // check for lock
            if(!FileUtil.checkForWriteLock(file)){
            	logger.log(Level.INFO, "Indexer: File {0} has a write-lock: I''m skip''n it ...", file);
            	return;
            }*/
            
            processFile(newVersion);
        }
    }

    private CloneFile addNewVersion() {
        CloneFile newVersion = new CloneFile(file);        
        
        newVersion.setVersion(1);
        newVersion.setSyncStatus(CloneFile.SyncStatus.LOCAL);
        newVersion.setStatus(Status.NEW);
        newVersion.setUpdated(new Date());
        newVersion.setClientName(Profile.getInstance().getName());
        
        Database.getInstance().incrementVersion();
        
        return newVersion;
    }
    
    private CloneFile addChangedVersion() {        
        CloneFile newVersion = (CloneFile) previousVersion.clone();

        // added because of new Database
        newVersion.setPrevious(previousVersion);
        previousVersion.setNext(newVersion);
        
        newVersion.setVersion(previousVersion.getVersion()+1);
        newVersion.setUpdated(new Date());
        newVersion.setStatus(Status.CHANGED);
        newVersion.setSyncStatus(CloneFile.SyncStatus.LOCAL);
        newVersion.setClientName(Profile.getInstance().getName());
        newVersion.setChunks(new ArrayList<CloneChunk>()); // otherwise we would append!
        newVersion.setFileSize(file.length());
        newVersion.setLastModified(new Date(file.lastModified()));      
        
        Database.getInstance().incrementVersion();
        
        return newVersion;
    }
    
    // TODO: Check whether this method can be removed, because folder items are generated automatically in the tree!
    private void processFolder(CloneFile cf) {        
        // Add rest of the DB stuff 
        cf.setChecksum(null);
        cf.persist();
        
        // Analyze file tree (if directory) -- RECURSIVELY!!
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Indexer: Indexing CHILDREN OF {0} ...", file);
        }

        for (File child : file.listFiles()) {
            // Ignore .ignore files
            if (child.getName().startsWith(Constants.FILE_IGNORE_PREFIX)) {                
                continue; 
            }

            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Indexer: Parent: {0} / CHILD {1} ...", new Object[]{file, child});
            }
            
            // Do it!
            new NewIndexRequest(child, null).process();
        }
        
    }
    
    private void processFile(CloneFile cf) {
        try {
            // MUTEX
            metaChunkMutex.acquire();
            
            // 1. Chunk it!
            boolean fileHasNewChunks = false;
            Chunk chunk = null;
            
            // creating chunks
            Enumeration<Chunk> chunks = Profile.getInstance().getRepository().getChunker().createChunks(file);
            
            metaChunkProfile = Profile.getInstance();
            
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Indexer: Creating chunks from file {0} ...", file);
            }            
            
            while (chunks.hasMoreElements()) {
                chunk = chunks.nextElement();      
                
                // Check chunk index
                CloneChunk dbChunk = Database.getInstance().getChunk(chunk.getChecksum());
                
                // Chunk exists in index; use existing chunk
                if (dbChunk != null) {
                    //System.out.println("from cache: "+cc);
                    cf.addChunk(dbChunk);
                    continue;
                }
                
                //System.out.println("not from cache");
                
                // Chunk does NOT exist so far; create and pack in a meta chunk               
                        
                // Close metachunk if it is full
                // NOTE: this might also be done by the meta chunk timer!
                if (multiChunk != null && multiChunk.isFull()) {
                	logger.log(Level.INFO, "CLOSE METACHUNK INSIDE LOOP");
                    closeMetaChunk(false);
                }
                
                // Choose meta ID; by convention, the first chunk ID is the metaId
                // NOTE: this must be AFTER the closeMetaChunk()-call
                byte[] metaId;
                
                if (multiChunk != null && multiChunk.getId() != null) {
                    metaId = multiChunk.getId();
                }
                else {
                    metaId = chunk.getChecksum(); 
                }                
                
                // Open new metachunk
                if (multiChunk == null) {                   
                    int chunkSize = Profile.getInstance().getRepository().getChunkSize()*1024;

                    Cipher encCipher = Profile.getInstance().getRepository().getEncryption().createEncCipher(metaId);

                    metaChunkTempFile = Profile.getInstance().getCache().createTempFile("metachunk");
                    
                    multiChunk = 
                    		new CustomMultiChunk(metaId, chunkSize, new CipherOutputStream(new FileOutputStream(metaChunkTempFile), encCipher));
                    
                            //new FileOutputStream(metaChunkTempFile));                    
            //                            new GZIPOutputStream(new FileOutputStream(metaChunkTempFile)));                    
                    
                    
                    // FILES completed in this metachunk (-> can be uploaded)
                    metaChunkCompletedFiles = new HashSet<CloneFile>();

                    logger.info("Opened new metachunk "+Profile.getInstance().getCache().getMetaChunkFile(multiChunk));
                }     
                
                // Create chunk in DB and add to cache; DO create it!
                dbChunk = Database.getInstance().getChunk(metaId, chunk.getChecksum(), true);

                fileHasNewChunks = true;                
                cf.addChunk(dbChunk);
                
                // Now write it to the temp. metachunk
                multiChunk.write(chunk);
            } // while (all chunks)
            
                        
            // Last chunk of this file: The last chunk holds the file checksum
            if (chunk != null) {
                cf.setChecksum(chunk.getFileChecksum());                 
                
                // If this file had new chunks, 
                // add the file to the "completed in this metachunk" list
                if (multiChunk != null) {
                    metaChunkCompletedFiles.add(cf);
                }
            }    

            // Metachunk is open, file is persisted when the metachunk is persisted
            if (multiChunk != null) {
                // Reset timer
                if (metaChunkTimoutTimerTask != null) {
                    metaChunkTimoutTimerTask.cancel();
                }

                metaChunkTimoutTimerTask = new MetaChunkTimeoutTimerTask();
                metaChunkTimeoutTimer.schedule(metaChunkTimoutTimerTask, METACHUNK_UNFINISHED_TIMEOUT);            
            }
            
            // If file has no new chunks, persist it now (because it does not depend
            // on any metachunks)
            if (!fileHasNewChunks) {
                cf.merge(); 
            }

            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Indexer: Done indexing NEW file {0} ...", file);
            }                        
            
            // RELEASE MUTEX            
            metaChunkMutex.release();
            
        }  
        catch (Exception e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Could not index new file "+file+". IGNORING.", e);
            }
            
            return;            
        }
    }
    
    @Override
    public String toString() {
        return NewIndexRequest.class.getSimpleName()+
            "[" + "file=" + file + "]";
    }
    
    private static void closeMetaChunk(boolean timerTriggered) throws IOException {        
        logger.log(Level.INFO, "Metachunk {0} is FULL.", Profile.getInstance().getCache().getMetaChunkFile(multiChunk));

        // Close meta chunk and get ID (= last chunk's ID)
        multiChunk.close();

        // Rename to final 'temp' metachunk
        File metaChunkFile = Profile.getInstance().getCache().getMetaChunkFile(multiChunk);
        metaChunkTempFile.renameTo(metaChunkFile);

        Set<CloneFile> mergedFiles = new HashSet<CloneFile>();
        
        for (CloneFile cf2 : metaChunkCompletedFiles) {
        	cf2.merge();
            mergedFiles.add(cf2);
        }
           
        for (CloneFile cf2 : mergedFiles) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Indexer: Queuing file {0} at uploader ...", cf2.getFile());
            }                        
        }
        
        metaChunkProfile.getUploader().queue(multiChunk, mergedFiles);                            

        // Reset meta chunk
        logger.log(Level.INFO, "Metachunk flushed. NO OPEN Metachunk.");
        multiChunk = null;           
    }
    
    private class MetaChunkTimeoutTimerTask extends TimerTask {
        @Override
        public void run() {
            try {
                metaChunkMutex.acquire();   
                
                // Only run 'close', if there is an open meta chunk
                if (multiChunk != null) {
                	logger.log(Level.INFO, "CLOSING METACHUNK DUE TO TIMEOUT");
                    closeMetaChunk(true);
                }
                
                metaChunkMutex.release();
            }
            catch (Exception ex) {
                Logger.getLogger(NewIndexRequest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
}
