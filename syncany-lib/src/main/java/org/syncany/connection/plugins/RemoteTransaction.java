/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.connection.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config;

/**
 * This class represents a transaction in a remote system. It will keep track of
 * what files are to be added and ensures atomic operation.
 * 
 * @author Pim Otte
 */
public class RemoteTransaction {
	private static final Logger logger = Logger.getLogger(RemoteTransaction.class.getSimpleName());
	
	private TransferManager transferManager;
	private Config config;
	private Map<File, TempRemoteFile> localToTempRemoteFileMap;
	private Map<TempRemoteFile, RemoteFile> tempToTargetRemoteFileMap;
	
	public RemoteTransaction(Config config, TransferManager transferManager) {
		this.config = config;
		this.transferManager = transferManager;

		this.localToTempRemoteFileMap = new HashMap<File, TempRemoteFile>();
		this.tempToTargetRemoteFileMap = new HashMap<TempRemoteFile, RemoteFile>();
	}
	
	/**
	 * Adds a file to this transaction. Generates a temporary file to store it.
	 */
	public void add(File localFile, RemoteFile remoteFile) throws StorageException {
		TempRemoteFile temporaryRemoteFile = new TempRemoteFile(localFile);

		logger.log(Level.INFO, "Adding file to transaction: " + localFile);
		logger.log(Level.INFO, " -> Temp. remote file: " + temporaryRemoteFile + ", final location: " + remoteFile);
		
		localToTempRemoteFileMap.put(localFile, temporaryRemoteFile);
		tempToTargetRemoteFileMap.put(temporaryRemoteFile, remoteFile);
	}
	
	/**
	 * Moves all files to the temporary remote location. If
	 * no errors occur, all files are moved to their final location.
	 */
	public void commit() throws StorageException {
		logger.log(Level.INFO, "Starting TX.commit() ...");

		File localTransactionFile = writeLocalTransactionFile();
		RemoteFile remoteTransactionFile = new TransactionRemoteFile(this);
		
		transferManager.upload(localTransactionFile, remoteTransactionFile);
		
		for (File localFile : localToTempRemoteFileMap.keySet()) {
			transferManager.upload(localFile, localToTempRemoteFileMap.get(localFile));
		}
		
		for (RemoteFile temporaryFile : tempToTargetRemoteFileMap.keySet()) {
			transferManager.move(temporaryFile, tempToTargetRemoteFileMap.get(temporaryFile));
		}
		
		transferManager.delete(remoteTransactionFile);
		localTransactionFile.delete();
		
		logger.log(Level.INFO, "Succesfully committed transaction.");
	}
	
	private File writeLocalTransactionFile() throws StorageException {
		try {
			File localTransactionFile = File.createTempFile("transaction-", "", config.getCacheDir());
		
			try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(localTransactionFile), "UTF-8"))) {
				Serializer serializer = new Persister();
				serializer.write(new TransactionTO(config.getMachineName(), tempToTargetRemoteFileMap), out);
			}
			
			logger.log(Level.INFO, "Wrote transaction manifest to temporary file: " + localTransactionFile);
			
			return localTransactionFile;
		}
		catch (Exception e) {
			throw new StorageException("Could not create temporary file for transaction", e);
		}
	}
}
