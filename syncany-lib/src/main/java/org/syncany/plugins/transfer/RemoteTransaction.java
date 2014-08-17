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
package org.syncany.plugins.transfer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config;
import org.syncany.plugins.StorageException;
import org.syncany.plugins.StorageMoveException;
import org.syncany.plugins.transfer.files.RemoteFile;
import org.syncany.plugins.transfer.files.TempRemoteFile;
import org.syncany.plugins.transfer.files.TransactionRemoteFile;
import org.syncany.plugins.transfer.to.ActionTO;
import org.syncany.plugins.transfer.to.TransactionTO;

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
	private TransactionTO transactionTO;
	
	public RemoteTransaction(Config config, TransferManager transferManager) {
		this.config = config;
		this.transferManager = transferManager;
		
		this.transactionTO = new TransactionTO(config.getMachineName());
	}
	
	/**
	 * Adds a file to this transaction. Generates a temporary file to store it.
	 */
	public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
		TempRemoteFile temporaryRemoteFile = new TempRemoteFile();

		logger.log(Level.INFO, "Adding file to transaction: " + localFile);
		logger.log(Level.INFO, " -> Temp. remote file: " + temporaryRemoteFile + ", final location: " + remoteFile);
		
		ActionTO action = new ActionTO();
		action.setType(ActionTO.TYPE_UPLOAD);
		action.setLocalTempLocation(localFile);
		action.setRemoteLocation(remoteFile);
		action.setRemoteTempLocation(temporaryRemoteFile);
		transactionTO.addAction(action);
	}
	
	/**
	 * Adds the deletion of a file to this transaction. Generates a temporary file
	 * to store it while the transaction is being finalized.
	 */
	public void delete(RemoteFile remoteFile) throws StorageException {
		TempRemoteFile temporaryRemoteFile = new TempRemoteFile();
		
		logger.log(Level.INFO, "Adding file to transaction for deletion: " + remoteFile);
		logger.log(Level.INFO, " -> Temp. remote file: " + temporaryRemoteFile);
		
		ActionTO action = new ActionTO();
		action.setType(ActionTO.TYPE_DELETE);
		action.setRemoteLocation(remoteFile);
		action.setRemoteTempLocation(temporaryRemoteFile);
		transactionTO.addAction(action);
		
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
		
		for (ActionTO action : transactionTO.getActions()) {

			RemoteFile tempRemoteFile = action.getTempRemoteFile();
			if (action.getType().equals(ActionTO.TYPE_UPLOAD)) {
				File localFile = action.getLocalTempLocation();
				logger.log(Level.INFO, "- Uploading {0} to temp. file {1} ...", new Object[] { localFile, tempRemoteFile });
				transferManager.upload(localFile, tempRemoteFile);
			}
			else if (action.getType().equals(ActionTO.TYPE_DELETE)) {
				RemoteFile remoteFile = action.getRemoteFile();
				logger.log(Level.INFO, "- Moving {0} to temp. file {1} ...", new Object[] { remoteFile, tempRemoteFile });
				try {
					transferManager.move(remoteFile, tempRemoteFile);
				}
				catch (StorageMoveException e) {
					logger.log(Level.INFO, "- Move to delete file failed because the remoteFile does not exist.", e);
				}
			}
		}
		
		for (ActionTO action : transactionTO.getActions()) {
			if (action.getType().equals(ActionTO.TYPE_UPLOAD)) {
				RemoteFile tempRemoteFile = action.getTempRemoteFile();
				RemoteFile finalRemoteFile = action.getRemoteFile();
				logger.log(Level.INFO, "- Moving temp. file {0} to final location {1} ...", new Object[] { tempRemoteFile, finalRemoteFile });
				transferManager.move(tempRemoteFile, finalRemoteFile);
			}
		}
		
		// After this deletion, the transaction is final!
		logger.log(Level.INFO, "- Deleting remote transaction file {0} ...", remoteTransactionFile);
		transferManager.delete(remoteTransactionFile);

		for (ActionTO action : transactionTO.getActions()) {
			if (action.getType().equals(ActionTO.TYPE_DELETE)){
				RemoteFile tempRemoteFile = action.getTempRemoteFile();
				logger.log(Level.INFO, "- Moving deleting temp. file {0}  ...", new Object[] { tempRemoteFile });
				transferManager.delete(tempRemoteFile);
			}
		}
		
		
		localTransactionFile.delete();
		
		logger.log(Level.INFO, "Succesfully committed transaction.");
	}
	
	private File writeLocalTransactionFile() throws StorageException {
		try {
			File localTransactionFile = config.getCache().createTempFile("transaction");
		
			try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(localTransactionFile), "UTF-8"))) {
				Serializer serializer = new Persister();
				serializer.write(transactionTO, out);
			}
			
			logger.log(Level.INFO, "Wrote transaction manifest to temporary file: " + localTransactionFile);
			
			return localTransactionFile;
		}
		catch (Exception e) {
			throw new StorageException("Could not create temporary file for transaction", e);
		}
	}
}
