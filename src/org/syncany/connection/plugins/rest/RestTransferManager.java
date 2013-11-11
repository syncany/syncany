/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.connection.plugins.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestStorageService;
import org.jets3t.service.model.StorageBucket;
import org.jets3t.service.model.StorageObject;
import org.syncany.connection.plugins.AbstractTransferManager;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.util.FileUtil;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class RestTransferManager extends AbstractTransferManager {
    private static final String APPLICATION_CONTENT_TYPE = "application/x-syncany";
    private static final Logger logger = Logger.getLogger(RestTransferManager.class.getSimpleName());
    
    private RestStorageService service;
    private StorageBucket bucket;
    
    private String dataPath;

    public RestTransferManager(RestConnection connection) {
        super(connection);             
        this.dataPath = "data";
    }

    @Override
    public RestConnection getConnection() {
        return (RestConnection) super.getConnection();
    }

    @Override
    public void connect() throws StorageException {
        try {
            if (service == null) {
                service = createService();
            }

            if (bucket == null) {
                bucket = createBucket();
            }
        } catch (ServiceException ex) {
            throw new StorageException("Unable to connect to S3: " + ex.getMessage(), ex);
        }
    }

    protected abstract RestStorageService createService() throws ServiceException;

    protected abstract StorageBucket createBucket();

    @Override
    public void disconnect() throws StorageException {
        // Fressen.
    }
    
    @Override
    public void init() throws StorageException {
    	connect();
    	    	
    	try {
    		StorageObject dataPathFolder = new StorageObject(dataPath+"/"); // Slash ('/') makes it a folder
			service.putObject(bucket.getName(), dataPathFolder);
		}
    	catch (ServiceException e) {
    		throw new StorageException(e);
		}
    }

    @Override
    public void download(RemoteFile remoteFile, File localFile) throws StorageException {
        connect();

        File tempFile = null;
        String remotePath = getRemoteFilePath(remoteFile);        

        try {
            // Download
            StorageObject fileObj = service.getObject(bucket.getName(), remotePath);
            InputStream fileObjInputStream = fileObj.getDataInputStream();
            
            logger.log(Level.FINE, "- Downloading from bucket "+bucket.getName()+": "+fileObj+" ...");            
            tempFile = createTempFile(remoteFile.getName());            
            FileUtil.writeToFile(fileObjInputStream, tempFile);

            fileObjInputStream.close();
            
            // Move to final location
            if (localFile.exists()) {
                localFile.delete();
            }

            FileUtils.moveFile(tempFile, localFile);
        }
        catch (Exception ex) {
            if (tempFile != null) {
                tempFile.delete();
            }

            throw new StorageException("Unable to download file '" + remoteFile.getName(), ex);
        }
    }

    @Override
    public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
        connect();
        
        String remotePath = getRemoteFilePath(remoteFile);

        try {
            StorageObject fileObject = new StorageObject(remotePath);
            
            fileObject.setContentLength(localFile.length());
            fileObject.setContentType(APPLICATION_CONTENT_TYPE);
            fileObject.setDataInputStream(new FileInputStream(localFile));
            
            logger.log(Level.FINE, "- Uploading to bucket "+bucket.getName()+": "+fileObject+" ...");
            service.putObject(bucket.getName(), fileObject);
        }
        catch (Exception ex) {
            Logger.getLogger(RestTransferManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public boolean delete(RemoteFile remoteFile) throws StorageException {
        connect();

        String remotePath = getRemoteFilePath(remoteFile);

        try {
            service.deleteObject(bucket.getName(), remotePath);
            return true;
        }
        catch (ServiceException ex) {
            Logger.getLogger(RestTransferManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public Map<String, RemoteFile> list() throws StorageException {
        connect();
        
        try {
	        Map<String, RemoteFile> completeList = new HashMap<String, RemoteFile>();
	        String bucketName = bucket.getName();
	        StorageObject[] objects = service.listObjects(bucketName);
	
	        for (StorageObject obj : objects) {
	            completeList.put(obj.getName(), new RemoteFile(obj.getName(), obj));
	        }
	        
	        return completeList;
        }
        catch (ServiceException ex) {
            Logger.getLogger(RestTransferManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new StorageException(ex);
        }        
    }

    @Override
    public Map<String, RemoteFile> list(String namePrefix) throws StorageException {
    	Map<String, RemoteFile> completeList = list();

        // No filter
        if (namePrefix == null) {
            return completeList;
        }
                
        // Filtered (prefix given)
        else {
	        Map<String, RemoteFile> filteredList = new HashMap<String, RemoteFile>();
	
	        for (Map.Entry<String, RemoteFile> e : completeList.entrySet()) {
	            if (e.getKey().startsWith(namePrefix)) {
	                filteredList.put(e.getKey(), e.getValue());
	            }
	        }
	        
	        return filteredList;
        }
    }  
    
    private String getRemoteFilePath(RemoteFile remoteFile) {
    	if (remoteFile instanceof MultiChunkRemoteFile) {
    		return dataPath+"/"+remoteFile.getName();
    	}
    	else {
    		return remoteFile.getName();
    	}
	}
}
