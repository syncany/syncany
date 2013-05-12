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
 * TODO Create listener for upload success and upload failure per file
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

    public synchronized boolean start() {
        if (isRunning()) {
            return false;
        }
        
        transferManager = connection.createTransferManager();
        
        worker = new Thread(new UploadWorker(), Uploader.class.getSimpleName());        
        worker.start();
        
        return true;
    }

    public synchronized boolean stop() {
        if (!isRunning()) {
            return false;
        }

        worker.interrupt(); 
        worker = null;
        
        return true;
    }
    
    public synchronized boolean isRunning() {
    	return worker != null && !worker.isInterrupted();
    }

    public synchronized void queue(File localFile) throws InterruptedException {
    	queue(localFile, new RemoteFile(localFile.getName()));
    }
    
    public synchronized void queue(File localFile, RemoteFile remoteFile) throws InterruptedException {
    	queue(localFile, remoteFile, null);
    }
    
    public synchronized void queue(File localFile, RemoteFile remoteFile, UploadListener uploadListener) throws InterruptedException {
    	logger.log(Level.INFO, "QUEUING {0}", localFile);
        queue.put(new UploadRequest(localFile, remoteFile, uploadListener));
    }
    
    public synchronized boolean isQueueEmtpy(){
    	return queue.isEmpty();
    }

	public synchronized int getQueueSize() {
		return queue.size();
	}       
	
	public interface UploadListener {
		public void onUploadStart(File localFile, RemoteFile remoteFile);
		public void onUploadSuccess(File localFile, RemoteFile remoteFile);
		public void onUploadFailure(File localFile, RemoteFile remoteFile, Throwable exception);
	}
    
    private class UploadRequest {
        private File localFile;
        private RemoteFile remoteFile;
        private UploadListener uploadListener;
        
        public UploadRequest(File localFileToUpload, RemoteFile remoteFile, UploadListener uploadListener) {
            this.localFile = localFileToUpload;
            this.remoteFile = remoteFile;
            this.uploadListener = uploadListener;
        }
	}

	private class UploadWorker implements Runnable {
		@Override
		public void run() {
			logger.log(Level.INFO, "Worker started.");
			
			try {
				UploadRequest currentUploadRequest;

				while (null != (currentUploadRequest = queue.take())) {
					logger.log(Level.INFO, "Processing queue.. size: " + queue.size());
					processRequest(currentUploadRequest);
				}
																																				 
			}
            catch (InterruptedException iex) {
            	logger.log(Level.INFO, "Worker interrupted.");
            }
        }

        private void processRequest(UploadRequest uploadRequest) {
        	logger.log(Level.INFO, "Uploader: Uploading {0} to {1} ...", new Object[] { uploadRequest.localFile, uploadRequest.remoteFile });
        	fireUploadStartEvent(uploadRequest);        	
            
            try {
                transferManager.upload(uploadRequest.localFile, uploadRequest.remoteFile);
                
                logger.log(Level.INFO, "Uploader: Successful upload {0} to {1} ...", new Object[] { uploadRequest.localFile, uploadRequest.remoteFile });
                fireUploadSuccessEvent(uploadRequest);                
            } 
            catch (StorageException ex) {
            	logger.log(Level.SEVERE, "Uploader: Upload FAILED for {0} to {1} ...", new Object[] { uploadRequest.localFile, uploadRequest.remoteFile });
            	fireUploadFailureEvent(uploadRequest, ex);
            }
        }
        
        private void fireUploadSuccessEvent(UploadRequest uploadRequest) {
        	if (uploadRequest.uploadListener != null) {
        		uploadRequest.uploadListener.onUploadSuccess(uploadRequest.localFile, uploadRequest.remoteFile);
        	}
        }
        
        private void fireUploadFailureEvent(UploadRequest uploadRequest, Throwable exception) {
        	if (uploadRequest.uploadListener != null) {
        		uploadRequest.uploadListener.onUploadFailure(uploadRequest.localFile, uploadRequest.remoteFile, exception);
        	}
        }
        
        private void fireUploadStartEvent(UploadRequest uploadRequest) {
        	if (uploadRequest.uploadListener != null) {
        		uploadRequest.uploadListener.onUploadStart(uploadRequest.localFile, uploadRequest.remoteFile);
        	}
        }        
    }
}
