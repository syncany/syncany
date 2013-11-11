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
package org.syncany.connection.plugins.unreliable_local;

import java.io.File;
import java.util.Map;

import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.local.LocalTransferManager;
import org.syncany.connection.plugins.unreliable_local.UnreliableLocalConnection.UnreliableLocalOperationStatus;

/**
 *
 * @author Philipp C. Heckel
 */
public class UnreliableLocalTransferManager extends LocalTransferManager {
	private int operationCounter;
	
    public UnreliableLocalTransferManager(UnreliableLocalConnection connection) {
        super(connection);   
        this.operationCounter = 0;
    }
    
    private boolean isNextOperationSuccessful(String operationDescription) {
    	UnreliableLocalOperationStatus operationSuccess = getConnection().getOperationStatusList().get(operationCounter);
    	
    	if (operationSuccess == UnreliableLocalOperationStatus.SUCCESS) {
    		System.out.println("Operation "+operationCounter+" successful:     "+operationDescription);
        	
    		increateOperationCounter();
        	return true;
    	}
    	else {
    		System.out.println("Operation "+operationCounter+" NOT successful: "+operationDescription);

    		increateOperationCounter();
    		return false;
    	}    	
    }
    
    private void increateOperationCounter() {
    	operationCounter = (operationCounter + 1) % getConnection().getOperationStatusList().size();
    }
    
    @Override
    public UnreliableLocalConnection getConnection() {
    	return (UnreliableLocalConnection) super.getConnection();
    }

    @Override
    public void connect() throws StorageException {
    	String operationName = "connect";
    	
    	if (isNextOperationSuccessful(operationName)) {
    		super.connect();
    	}
    	else {
    		throw new StorageException("Operation failed: "+operationName);
    	}
    }

    @Override
    public void disconnect() throws StorageException {
    	String operationName = "disconnect";

    	if (isNextOperationSuccessful(operationName)) {
    		super.disconnect();
    	}
    	else {
    		throw new StorageException("Operation failed: "+operationName);
    	}
    }

    @Override
    public void init() throws StorageException {
    	String operationName = "init";

    	if (isNextOperationSuccessful(operationName)) {
    		super.init();
    	}
    	else {
    		throw new StorageException("Operation failed: "+operationName);
    	}
    }
    
    @Override
    public void download(RemoteFile remoteFile, File localFile) throws StorageException {
    	String operationName = "download("+remoteFile.getName()+", "+localFile.getAbsolutePath()+")";

    	if (isNextOperationSuccessful(operationName)) {
    		super.download(remoteFile, localFile);
    	}
    	else {
    		throw new StorageException("Operation failed: "+operationName);
    	}
    }

    @Override
    public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
    	String operationName = "upload("+localFile.getAbsolutePath()+", "+remoteFile.getName()+")";

    	if (isNextOperationSuccessful(operationName)) {
    		super.upload(localFile, remoteFile);
    	}
    	else {
    		throw new StorageException("Operation failed: "+operationName);
    	}
    }

    @Override
    public boolean delete(RemoteFile remoteFile) throws StorageException {
    	String operationName = "delete("+remoteFile.getName()+")";

    	if (isNextOperationSuccessful(operationName)) {
    		return super.delete(remoteFile);
    	}
    	else {
    		throw new StorageException("Operation failed: "+operationName);
    	}
    }

    @Override
    public Map<String, RemoteFile> list() throws StorageException {
        return list(null);
    }

    @Override
    public Map<String, RemoteFile> list(final String namePrefix) throws StorageException {
    	String operationName = "list("+namePrefix+")";

    	if (isNextOperationSuccessful(operationName)) {
    		return super.list(namePrefix);
    	}
    	else {
    		throw new StorageException("Operation failed: "+operationName);
    	}
    }
}
