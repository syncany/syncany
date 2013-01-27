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
package org.syncany.connection;

import java.io.File;
import java.util.Comparator;
import java.util.Date;
import org.syncany.watch.remote.files.RemoteFile;
import org.syncany.db.CloneChunk;
import org.syncany.db.CloneFile;
import org.syncany.chunk.MultiChunk;
import org.syncany.communication.CommunicationController;
import org.syncany.communication.CommunicationController.SyncanyStatus;
import org.syncany.config.Profile;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.db.CloneFile.SyncStatus;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.syncany.db.Database;
import org.syncany.exceptions.StorageException;

/**
 * Represents the remote storage.
 * Processes upload and download requests asynchronously.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Uploader {
    private static final int CACHE_FILE_LIST = 60000;
    
    private static final Logger logger = Logger.getLogger(Uploader.class.getSimpleName());
    
    private Profile profile;
    private TransferManager transfer;
    private PriorityBlockingQueue<UploadRequest> queue;
    private Thread worker;

    private Map<String, RemoteFile> fileList;
    private Date cacheLastUpdate;
    
    private UploaderComparator uploaderComparator;
    
    public Uploader(Profile profile) {
        this.profile = profile;
        this.uploaderComparator = new UploaderComparator();
        this.queue = new PriorityBlockingQueue<UploadRequest>(11, uploaderComparator);

        this.worker = null; // cmp. method 'start'
    }   

    public synchronized void start() {
        if (worker != null)
            return;
        
        transfer = profile.getRepository().getConnection().createTransferManager();
        
        worker = new Thread(new Worker(), "UploaderWorker");        
        worker.start();
    }

    public synchronized void stop() {
        if (worker == null || worker.isInterrupted())
            return;

        worker.interrupt(); // why not stop?
        worker = null;
    }

    public synchronized void queue(MultiChunk metaChunk, Set<CloneFile> updatedFiles) {
    	logger.log(Level.INFO, "QUEUING {0}", metaChunk);
        queue.put(new UploadRequest(metaChunk, updatedFiles));
    }
    
    public boolean isEmtpy(){
    	return queue.isEmpty();
    }
    
    private class UploadRequest {
        private MultiChunk metaChunk;
        private Set<CloneFile> updatedFiles;

        public UploadRequest(MultiChunk metaChunk, Set<CloneFile> updatedFiles) {
            this.metaChunk = metaChunk;
            this.updatedFiles = updatedFiles;
        }

        public Set<CloneFile> getUpdatedFiles() {
            return updatedFiles;
        }

        public MultiChunk getMetaChunk() {
            return metaChunk;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final UploadRequest other = (UploadRequest) obj;
            if (this.metaChunk != other.metaChunk && (this.metaChunk == null || !this.metaChunk.equals(other.metaChunk))) {
                return false;
            }
            return true;
        }
                
    }

    private class Worker implements Runnable {
        @Override
        public void run() {
            try {
                UploadRequest req;

                while (null != (req = queue.take())) {
                	System.out.println("Processing Queue.. size: "+queue.size());
                	
                	System.out.println("Uploader Thread-ID: "+Thread.currentThread().getId());
                	
                    processRequest(req);
                }
            }
            catch (InterruptedException iex) {
                iex.printStackTrace();
            }
        }

        private void processRequest(UploadRequest req) {
        	// Tray updating status icon was here
        	CommunicationController.getInstance().updateStatus(SyncanyStatus.updating);
        	
        	logger.log(Level.INFO, "PROCESSING UPLOAD REQUEST {0}", req.getMetaChunk());
        	System.out.println("Uploader Thread-ID: "+Thread.currentThread().getId());
            
            // Get file list (to check if chunks already exist)
            if (cacheLastUpdate == null || fileList == null || System.currentTimeMillis()-cacheLastUpdate.getTime() > CACHE_FILE_LIST) {
                try { 
                	System.out.println("checking if file exists..");
                	
                    fileList = transfer.list();
                    
                    System.out.println("Got list of remote files, list.size: "+fileList.size());
                }
                catch (StorageException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    return;
                    // TODO what to do here?
                }
            }                       
            
            System.out.println("Going on");
            
            MultiChunk metaChunk = req.getMetaChunk();            
            File localMetaChunkFile = Profile.getInstance().getCache().getMetaChunkFile(metaChunk);
            String remoteChunkFilename = CloneChunk.getFileName(null, metaChunk.getId());
                       
            // Chunk has been uploaded before; Skip upload
            if (fileList.containsKey(remoteChunkFilename)) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, "UploadManager: Chunk {0} already uploaded", remoteChunkFilename);
                }

                System.out.println("I'm here");
                
                for (CloneFile file : req.getUpdatedFiles()) {
                    // Update DB sync status
                    if (!file.isFolder()) {
                        file.setSyncStatus(CloneFile.SyncStatus.UPTODATE);
                        file.merge();

                        touch(file, SyncStatus.UPTODATE);
                    }
                }
                
                if (queue.isEmpty()) {
                	System.out.println("Up-to-date-state");
                	
                    // try up-to-date status icon
                	CommunicationController.getInstance().updateStatus(SyncanyStatus.inSync);
                }                
                
                return;
            }
            
            System.out.println("Uploader Thread-ID: "+Thread.currentThread().getId());
            
            System.out.println("Setting files to Syncing");
            CommunicationController.getInstance().updateStatus(SyncanyStatus.updating);
            
            // Set files to syncing
            for (CloneFile file : req.getUpdatedFiles()) {
                // Update DB sync status
                if (!file.isFolder()) {
                    file.setSyncStatus(CloneFile.SyncStatus.SYNCING);
                    file.merge();

                    touch(file, SyncStatus.SYNCING);
                }
            }                            

            System.out.println("Uploading it..");
            
            // Upload it!
            try {
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, "UploadManager: Uploading chunk {0} ...", remoteChunkFilename);
                }

                RemoteFile chunkFile = new RemoteFile(remoteChunkFilename);
                
                System.out.println("Starting upload..");
                transfer.upload(localMetaChunkFile, chunkFile);
                //transfer.upload(config.getCache().getCacheChunk(metaChunkFilename), chunkFile);

                System.out.println("Putting to cache-list");
                // Add to cache
                fileList.put(remoteChunkFilename, chunkFile);
            } 
            catch (StorageException ex) {
                logger.log(Level.SEVERE,"UploadManager: Uploading chunk "+remoteChunkFilename+" FAILED !!",ex);
                return; // TODO and now?
            }

            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "UploadManager: Chunk {0} uploaded", remoteChunkFilename);
            }

            // Update DB sync status
            for (CloneFile file : req.getUpdatedFiles()) {
                // Update DB sync status
                if (!file.isFolder()) {
                    file.setSyncStatus(CloneFile.SyncStatus.SYNCING);
                    file.merge();

                    touch(file, SyncStatus.SYNCING);
                }
            }         

            if (queue.isEmpty()) {
            	System.out.println("Up-to-date-state");
            	
            	// try up-to-date status icon
            	CommunicationController.getInstance().updateStatus(SyncanyStatus.inSync);
            }
        }

        
        // TODO delete or replace method
        private void touch(CloneFile file, SyncStatus syncStatus) {
            // Touch myself
            // desktop.touch(file.getFile()) was here
            
            // Touch parents
            Database db = Database.getInstance();
            
            CloneFile childCF = file;
            CloneFile parentCF;
            
            while (null != (parentCF = db.getParent(childCF))) {
                if (parentCF.getSyncStatus() != syncStatus) {
                    parentCF.setSyncStatus(syncStatus);
                    parentCF.merge();
                    
                    // desktop.touch(parentCF.getFile()) was here
                }

                childCF = parentCF;
            }
        }
    }
    
    /**
     * Sorts by size for files > 3MB and by name for the others.
     */
    private class UploaderComparator implements Comparator<UploadRequest> {
        @Override
        public int compare(UploadRequest r1, UploadRequest r2) {
            return 0; // TODO this is complicated...
        }        

        /*@Override
        public int compare(CloneFile c1, CloneFile c2) {
            // Files bigger than 3 MB go to the back
            if (c1.getFileSize() > 3*1024*1024 || c2.getFileSize() > 3*1024*1024) {
                if (c1.getFileSize() == c2.getFileSize()) {
                    return 0;
                }
                else if (c1.getFileSize() < c2.getFileSize()) {
                    return -1;
                }
                else {
                    return 1;
                }
            }
            
            // Smaller files by name
            return c1.getFile().compareTo(c2.getFile());
        }        */
    
    }
}
