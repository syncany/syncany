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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;

/**
 * Represents the remote storage.
 * Processes upload and download requests asynchronously.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Uploader {
    private static final Logger logger = Logger.getLogger(Uploader.class.getSimpleName());
    
    private Connection connection;
    private TransferManager transferManager;
    private BlockingQueue<UploadRequest> queue;
    private Thread worker;

    public Uploader(Connection connection) {
    	this.connection = connection;
        this.queue = new LinkedBlockingQueue<UploadRequest>(11);

        this.worker = null; // cmp. method 'start'
    }   

    public synchronized void start() {
        if (worker != null)
            return;
        
        transferManager = connection.createTransferManager();
        
        worker = new Thread(new Worker(), Uploader.class.getSimpleName());        
        worker.start();
    }

    public synchronized void stop() {
        if (worker == null || worker.isInterrupted())
            return;

        worker.interrupt(); 
        worker = null;
    }

    public synchronized void queue(File localFile) throws InterruptedException {
    	queue(localFile, new RemoteFile(localFile.getName()));
    }
    
    public synchronized void queue(File localFile, RemoteFile remoteFile) throws InterruptedException {
    	logger.log(Level.INFO, "QUEUING {0}", localFile);
        queue.put(new UploadRequest(localFile, remoteFile));
    }
    
    public boolean isQueueEmtpy(){
    	return queue.isEmpty();
    }

	public int queueSize() {
		return queue.size();
	}        
    
    private class UploadRequest {
        private File localFile;
        private RemoteFile remoteFile;
        
        public UploadRequest(File localFileToUpload, RemoteFile remoteFile) {
            this.localFile = localFileToUpload;
            this.remoteFile = remoteFile;
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
            if (this.localFile != other.localFile && (this.localFile == null || !this.localFile.equals(other.localFile))) {
                return false;
            }
            return true;
        }
                
	}

	private class Worker implements Runnable {
		@Override
		public void run() {
			try {
				UploadRequest currentUploadRequest;

				while (null != (currentUploadRequest = queue.take())) {
					logger.log(Level.INFO, "Processing queue.. size: " + queue.size());
					processRequest(currentUploadRequest);
				}
																																				 
			}
            catch (InterruptedException iex) {
                iex.printStackTrace();
            }
        }

        private void processRequest(UploadRequest uploadRequest) {
        	logger.log(Level.INFO, "Processing upload request {0}", uploadRequest.localFile);
            
        	// TODO Caching for performance           
        	
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Uploader: Uploading chunk {0} to {1} ...", new Object[] { uploadRequest.localFile, uploadRequest.remoteFile });
            }
            
            try {
                transferManager.upload(uploadRequest.localFile, uploadRequest.remoteFile);
            } 
            catch (StorageException ex) {
            	logger.log(Level.SEVERE, "Uploader: Upload FAILED for chunk {0} to {1} ...", new Object[] { uploadRequest.localFile, uploadRequest.remoteFile });
                return; // TODO and now?
            }

            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Uploader: Uploading chunk {0} to {1} ...", new Object[] { uploadRequest.localFile, uploadRequest.remoteFile });
            }
        }
    }
}
