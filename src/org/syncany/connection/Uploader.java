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
import java.util.Date;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.chunk.MultiChunk;
import org.syncany.config.Profile;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.util.StringUtil;

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
    
    public Uploader(Profile profile) {
        this.profile = profile;
        this.queue = new PriorityBlockingQueue<UploadRequest>(11);

        this.worker = null; // cmp. method 'start'
    }   

    public synchronized void start() {
        if (worker != null)
            return;
        
        transfer = profile.getConnection().createTransferManager();
        
        worker = new Thread(new Worker(), "UploaderWorker");        
        worker.start();
    }

    public synchronized void stop() {
        if (worker == null || worker.isInterrupted())
            return;

        worker.interrupt(); // why not stop?
        worker = null;
    }

    public synchronized void queue(MultiChunk metaChunk) {
    	logger.log(Level.INFO, "QUEUING {0}", metaChunk);
        queue.put(new UploadRequest(metaChunk));
    }
    
    public boolean isEmtpy(){
    	return queue.isEmpty();
    }
    
    private class UploadRequest {
        private MultiChunk metaChunk;

        public UploadRequest(MultiChunk metaChunk) {
            this.metaChunk = metaChunk;
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
					System.out.println("Processing Queue.. size: "
							+ queue.size());

					System.out.println("Uploader Thread-ID: "
							+ Thread.currentThread().getId());

					processRequest(req);
				}
																																				 
			}
            catch (InterruptedException iex) {
                iex.printStackTrace();
            }
        }

        private void processRequest(UploadRequest req) {
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
            //FIXME
            File localMetaChunkFile = null;//Profile.getInstance().getCache().getMultiChunkFile(metaChunk);
            //FIXME
            String remoteChunkFilename = "multichunk-" + StringUtil.toHex(metaChunk.getId());
                       
            // Chunk has been uploaded before; Skip upload
            if (fileList.containsKey(remoteChunkFilename)) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, "UploadManager: Chunk {0} already uploaded", remoteChunkFilename);
                }
                
                if (queue.isEmpty()) {
                	System.out.println("Up-to-date-state");
                }                
                
                return;
            }
            
            System.out.println("Uploader Thread-ID: "+Thread.currentThread().getId());
            
            System.out.println("Setting files to Syncing");
                                    

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

            if (queue.isEmpty()) {
            	System.out.println("Up-to-date-state");
            	
            	// try up-to-date status icon
            }
        }

        
    }    
}
