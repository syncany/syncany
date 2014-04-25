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
		Map<String, TransactionRemoteFile> transactionFiles = list(TransactionRemoteFile.class);
		for (TransactionRemoteFile transaction : transactionFiles.values()) {
			Map<RemoteFile, RemoteFile> finalLocations = null;
			String machineName = null;
			try {
				File transactionFile = File.createTempFile("transaction-", "", connection.getConfig().getCacheDir());
				// Download transaction file
				download(transaction, transactionFile);
				String transactionFileStr = FileUtils.readFileToString(transactionFile);
				// Deserialize it
				Serializer serializer = new Persister();
				TransactionTO transactionTO = serializer.read(TransactionTO.class, transactionFileStr);
				// Extract final locations
				finalLocations = transactionTO.getFinalLocations();
				machineName = transactionTO.getMachineName();
				transactionFile.delete();
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new StorageException("Failed to read transactionFile", e);
			}
			boolean cleaned = false;
			if (connection.getConfig().getMachineName().equals(machineName)) {
				cleaned = cleanup(transaction, finalLocations);
			}
			if (finalLocations != null && !cleaned) {
				filesInTransaction.addAll(finalLocations.values());
			}
		}
		
		return filesInTransaction;
	}
	/**
	 * This method removes all files related to an unfinished transaction.
	 */
	protected boolean cleanup(TransactionRemoteFile transaction, Map<RemoteFile, RemoteFile> finalLocations) throws StorageException {
		for (RemoteFile temporaryLocation : finalLocations.keySet()) {
			delete(temporaryLocation);
			delete(finalLocations.get(temporaryLocation));
		}
		return delete(transaction);
	}
}
