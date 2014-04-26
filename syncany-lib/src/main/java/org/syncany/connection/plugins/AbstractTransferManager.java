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
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config;

/**
 * Implements basic functionality of a {@link TransferManager} which
 * can be implemented sub-classes.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class AbstractTransferManager implements TransferManager {
	private static final Logger logger = Logger.getLogger(AbstractTransferManager.class.getSimpleName());	
	private Connection connection;
	
	public AbstractTransferManager(Connection connection) {
		this.connection = connection;
	}

	public Connection getConnection() {
		return connection;
	}

	// TODO [low] This should be in AbstractTransferManager (or any other central place), this should use the Syncany cache folder
	protected File createTempFile(String name) throws IOException {
		return File.createTempFile(String.format("temp-%s-", name), ".tmp");
	}

	@Override
	public StorageTestResult test(boolean testCreateTarget) {
		logger.log(Level.INFO, "Performing storage test TM.test() ...");							
		StorageTestResult result = new StorageTestResult();
		
		try {
			connect();
	
			result.setTargetExists(testTargetExists());
			result.setTargetCanWrite(testTargetCanWrite());
			result.setRepoFileExists(testRepoFileExists());

			if (result.isTargetExists()) {
				result.setTargetCanCreate(true);
			}
			else {
				if (testCreateTarget) {
					result.setTargetCanCreate(testTargetCanCreate());
				}
				else {
					result.setTargetCanCreate(false);
				}
			}
			
			result.setTargetCanConnect(true);
		}
		catch (StorageException e) {
			result.setTargetCanConnect(false);
			result.setException(e);
			
			logger.log(Level.INFO, "-> Testing storage failed. Returning " + result, e);
		}
		finally {
			try {
				disconnect();
			}
			catch (StorageException e) {
				// Don't care
			}
		}
		
		return result;
	}
	
	/**
	 * Returns a Set of all files that are not temporary, but are listed in a 
	 * transaction file. These belong to an unfinished transaction and should be
	 * ignored.
	 */
	protected Set<RemoteFile> getFilesInTransactions() throws StorageException {
		Set<RemoteFile> filesInTransaction = new HashSet<RemoteFile>();
			
		for (TransactionTO transactionTO : getAllTransactions()) {
			filesInTransaction.addAll(transactionTO.getFinalLocations().values());
		}
		
		return filesInTransaction;
	}
	
	/**
	 * Returns the temporary location if a file is in a deleting transaction,
	 * otherwise returns null.
	 */
	protected RemoteFile getDeletedFile(RemoteFile deletedFile) throws StorageException {
		for (TransactionTO transactionTO : getAllTransactions()) {
			if (transactionTO.getDeletedLocations().containsKey(deletedFile)) {
				return transactionTO.getDeletedLocations().get(deletedFile);
			}
		}
		return null;
	}
	
	/**
	 * This method removes all files related to an unfinished transaction.
	 */
	protected boolean cleanup(TransactionRemoteFile transaction, TransactionTO transactionTO) throws StorageException {
		for (RemoteFile temporaryLocation : transactionTO.getFinalLocations().keySet()) {
			delete(temporaryLocation);
			delete(transactionTO.getFinalLocations().get(temporaryLocation));
		}
		
		return delete(transaction);
	}
	
	private Set<TransactionTO> getAllTransactions() throws StorageException {
		Set<TransactionTO> transactions = new HashSet();
		Map<String, TransactionRemoteFile> transactionFiles = list(TransactionRemoteFile.class);
		for (TransactionRemoteFile transaction : transactionFiles.values()) {
			try {
				File transactionFile = File.createTempFile("transaction-", "", connection.getConfig().getCacheDir());
				// Download transaction file
				download(transaction, transactionFile);
				String transactionFileStr = FileUtils.readFileToString(transactionFile);
				// Deserialize it
				Serializer serializer = new Persister();
				TransactionTO transactionTO = serializer.read(TransactionTO.class, transactionFileStr);
				
				boolean cleaned = false;
				if (connection.getConfig().getMachineName().equals(transactionTO.getMachineName())) {
					cleaned = cleanup(transaction, transactionTO);
				}
				
				if (!cleaned) {
					transactions.add(transactionTO);
				}
				// Extract final locations
				transactionFile.delete();
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new StorageException("Failed to read transactionFile", e);
			}
		}
		return transactions;
	}
}
