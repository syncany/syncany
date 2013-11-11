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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.local.LocalTransferManager;

/**
 *
 * @author Philipp C. Heckel
 */
public class UnreliableLocalTransferManager extends LocalTransferManager {
	private int totalOperationCounter;
	private Map<String, Integer> typeOperationCounters;
	private List<String> failingOperationPatterns;	
	
    public UnreliableLocalTransferManager(UnreliableLocalConnection connection) {
        super(connection);   
        
        this.totalOperationCounter = 0;
        this.typeOperationCounters = new HashMap<String, Integer>();
        this.failingOperationPatterns = connection.getFailingOperationPatterns();
    }
    
    private boolean isNextOperationSuccessful(String operationType, String operationDescription) {
    	// Increase absolute/overall operation counter
    	totalOperationCounter++;
    	
    	// Increase type-relative operation counter
    	Integer typeOperationCounter = typeOperationCounters.get(operationType);
    	
    	typeOperationCounter = (typeOperationCounter != null) ? typeOperationCounter + 1 : 1;    	
    	typeOperationCounters.put(operationType, typeOperationCounter);
    	
    	// Construct operation line
    	String operationLine = String.format("abs=%d rel=%d op=%s %s", totalOperationCounter, typeOperationCounter, operationType, operationDescription);
    	
    	// Check if it fails
    	for (String failingOperationPattern : failingOperationPatterns) {
    		if (operationLine.matches(".*"+failingOperationPattern+".*")) {
        		System.out.println("Operation NOT successful: "+operationLine);
        		return false;    			
    		}
    	}
    	
    	System.out.println("Operation successful:     "+operationLine);
        return true;
    }
    
    @Override
    public UnreliableLocalConnection getConnection() {
    	return (UnreliableLocalConnection) super.getConnection();
    }

    @Override
    public void connect() throws StorageException {
    	String operationType = "connect";
    	String operationDescription = "connect";
    	
    	if (isNextOperationSuccessful(operationType, operationDescription)) {
    		super.connect();
    	}
    	else {
    		throw new StorageException("Operation failed: "+operationDescription);
    	}
    }

    @Override
    public void disconnect() throws StorageException {
    	String operationType = "disconnect";
    	String operationDescription = "disconnect";

    	if (isNextOperationSuccessful(operationType, operationDescription)) {
    		super.disconnect();
    	}
    	else {
    		throw new StorageException("Operation failed: "+operationDescription);
    	}
    }

    @Override
    public void init() throws StorageException {
    	String operationType = "init";
    	String operationDescription = "init";

    	if (isNextOperationSuccessful(operationType, operationDescription)) {
    		super.init();
    	}
    	else {
    		throw new StorageException("Operation failed: "+operationDescription);
    	}
    }
    
    @Override
    public void download(RemoteFile remoteFile, File localFile) throws StorageException {
    	String operationType = "download";
    	String operationDescription = "download("+remoteFile.getName()+", "+localFile.getAbsolutePath()+")";

    	if (isNextOperationSuccessful(operationType, operationDescription)) {
    		super.download(remoteFile, localFile);
    	}
    	else {
    		throw new StorageException("Operation failed: "+operationDescription);
    	}
    }

    @Override
    public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
    	String operationType = "upload";
    	String operationDescription = "upload("+localFile.getAbsolutePath()+", "+remoteFile.getName()+")";

    	if (isNextOperationSuccessful(operationType, operationDescription)) {
    		super.upload(localFile, remoteFile);
    	}
    	else {
    		throw new StorageException("Operation failed: "+operationDescription);
    	}
    }

    @Override
    public boolean delete(RemoteFile remoteFile) throws StorageException {
    	String operationType = "delete";
    	String operationDescription = "delete("+remoteFile.getName()+")";

    	if (isNextOperationSuccessful(operationType, operationDescription)) {
    		return super.delete(remoteFile);
    	}
    	else {
    		throw new StorageException("Operation failed: "+operationDescription);
    	}
    }

    @Override
    public Map<String, RemoteFile> list() throws StorageException {
        return list(null);
    }

    @Override
    public Map<String, RemoteFile> list(final String namePrefix) throws StorageException {
    	String operationType = "list";
    	String operationDescription = "list("+namePrefix+")";

    	if (isNextOperationSuccessful(operationType, operationDescription)) {
    		return super.list(namePrefix);
    	}
    	else {
    		throw new StorageException("Operation failed: "+operationDescription);
    	}
    }
}
