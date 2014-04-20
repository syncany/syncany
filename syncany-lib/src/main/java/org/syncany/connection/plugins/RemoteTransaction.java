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
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.chunk.Transformer;
import org.syncany.config.Config;


/**
 * This class represents a transaction in a remote system. It will keep track of
 * what files are to be added and ensures atomic operation.
 * 
 * @author Pim Otte
 *
 */
public class RemoteTransaction {
	private static final Logger logger = Logger.getLogger(RemoteTransaction.class.getSimpleName());
	
	private TransferManager transferManager;
	private Config config;
	private Map<File, RemoteFile> temporaryLocations;
	private Map<RemoteFile, RemoteFile> finalLocations;
	
	public RemoteTransaction(Config config, TransferManager transferManager) {
		this.transferManager = transferManager;
		temporaryLocations = new HashMap<File, RemoteFile>();
		finalLocations = new HashMap<RemoteFile, RemoteFile>();
		this.config = config;
	}
	
	/**
	 * Adds a file to this transaction. Generates a temporary file to store it.
	 */
	public void add(File localFile, RemoteFile remoteFile) throws StorageException {
		logger.log(Level.INFO, "Adding file to transaction: " + localFile);
		RemoteFile temporaryFile = new TempRemoteFile(localFile);
		temporaryLocations.put(localFile, temporaryFile);
		finalLocations.put(temporaryFile, remoteFile);
	}
	
	/**
	 * Moves all files to the temporary remote location. If
	 * no errors occur, all files are moved to their final location.
	 */
	public void commit() throws StorageException {
		File localTransactionFile = writeLocalTransactionFile();
		RemoteFile remoteTransactionFile = new TransactionRemoteFile(this);
		transferManager.upload(localTransactionFile, remoteTransactionFile);
		
		for (File localFile : temporaryLocations.keySet()) {
			transferManager.upload(localFile, temporaryLocations.get(localFile));
		}
		
		for (RemoteFile temporaryFile : finalLocations.keySet()) {
			transferManager.move(temporaryFile, finalLocations.get(temporaryFile));
		}
		
		transferManager.delete(remoteTransactionFile);
		localTransactionFile.delete();
		logger.log(Level.INFO, "Succesfully committed transaction.");
	}
	
	private File writeLocalTransactionFile() throws StorageException {
		File localTransactionFile;
		PrintWriter out;
		try {
			localTransactionFile = File.createTempFile("transaction-", "", config.getCacheDir());
				
			out = new PrintWriter(new OutputStreamWriter(
						new FileOutputStream(localTransactionFile), "UTF-8"));

		}
		catch (IOException e) {
			throw new StorageException("Could not create temporary file for transaction", e);
		}
		
		try {
			Serializer serializer = new Persister();
			serializer.write(new TransactionTO(config.getMachineName(),finalLocations), out);
		}
		catch (Exception e) {
			throw new StorageException("Could not serialize transaction manifest", e);
		}
		
		logger.log(Level.INFO, "Wrote transaction manifest to temporary file: " + localTransactionFile);
		
		return localTransactionFile;
	}
}
