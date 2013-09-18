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
package org.syncany.connection.plugins.rest;

import java.io.File;
import java.util.Date;
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
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.util.FileUtil;

/**
 *
 * @author oubou68, pheckel
 */
public abstract class RestTransferManager extends AbstractTransferManager {
    private static final Logger logger = Logger.getLogger(RestTransferManager.class.getSimpleName());
    private static final int CACHE_LIST_TIME = 60000;
    
    private RestStorageService service;
    private StorageBucket bucket;
    
    private Long cachedListUpdated;
    /**
     * Used for the upload function to determine whether a file is already uploaded
     */
    private Map<String, RemoteFile> cachedList;

    public RestTransferManager(RestConnection connection) {
        super(connection);
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
    public void download(RemoteFile remoteFile, File localFile) throws StorageException {
        connect();

        File tempFile = null;

        try {
            // Download
            StorageObject fileObj = service.getObject(bucket.getName(), remoteFile.getName());

            logger.log(Level.FINE, "- Downloading from bucket "+bucket.getName()+": "+fileObj+" ...");            
            tempFile = createTempFile(remoteFile.getName());            
            FileUtil.writeToFile(fileObj.getDataInputStream(), tempFile);

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

        try {
            // Skip if file exists
            Map<String, RemoteFile> list = getList(false);
            
            if (list.containsKey(remoteFile.getName())) {
                return;
            }

            // Read file entirely
            byte[] fileBytes = FileUtil.readFile(localFile); // TODO [medium] WARNING! Read ENTIRE file!

            StorageObject fileObject = new StorageObject(remoteFile.getName(), fileBytes);
            
            logger.log(Level.FINE, "- Uploading to bucket "+bucket.getName()+": "+fileObject+" ...");
            service.putObject(bucket.getName(), fileObject);
            
            // Add to cache
            cachedList.put(remoteFile.getName(), remoteFile);

        } catch (Exception ex) {
            Logger.getLogger(RestTransferManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public boolean delete(RemoteFile remoteFile) throws StorageException {
        connect();

        try {
            service.deleteObject(bucket.getName(), remoteFile.getName());
            return true;
        }
        catch (ServiceException ex) {
            Logger.getLogger(RestTransferManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public Map<String, RemoteFile> list() throws StorageException {
        return list(null);
    }

    @Override
    public Map<String, RemoteFile> list(String namePrefix) throws StorageException {
        connect();

        try {
            Map<String, RemoteFile> completeList = getList(true);
            
            if (namePrefix == null) {
                return completeList;
            }
            
            // Filtered (prefix given)
            Map<String, RemoteFile> filteredList = new HashMap<String, RemoteFile>();

            for (Map.Entry<String, RemoteFile> e : completeList.entrySet()) {
                if (e.getKey().startsWith(namePrefix)) {
                    filteredList.put(e.getKey(), e.getValue());
                }
            }
            
            return filteredList;

        }
        catch (ServiceException ex) {
            Logger.getLogger(RestTransferManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new StorageException(ex);
        }
    }
    
    private synchronized Map<String, RemoteFile> getList(boolean forceRefresh) throws ServiceException {
        // Used cached list
        if (!forceRefresh && cachedListUpdated != null && cachedListUpdated+CACHE_LIST_TIME > System.currentTimeMillis()) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "getList(): using cached list from {0}", new Date(cachedListUpdated));
            }
            
            return cachedList;
        }
        
        // Refresh cache
        Map<String, RemoteFile> list = new HashMap<String, RemoteFile>();
        String bucketName = bucket.getName();
        StorageObject[] objects = service.listObjects(bucketName);

        for (StorageObject obj : objects) {
            list.put(obj.getName(), new RemoteFile(obj.getName(), obj));
        }
        
        cachedList = list;
        cachedListUpdated = System.currentTimeMillis();        
        
        return list;
    }
}
