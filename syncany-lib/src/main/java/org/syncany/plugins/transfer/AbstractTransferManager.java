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
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config;
import org.syncany.plugins.StorageException;
import org.syncany.plugins.StorageTestResult;
import org.syncany.plugins.transfer.files.RemoteFile;
import org.syncany.plugins.transfer.files.TransactionRemoteFile;
import org.syncany.plugins.transfer.to.ActionTO;
import org.syncany.plugins.transfer.to.TransactionTO;

/**
 * Implements basic functionality of a {@link TransferManager} which
 * can be implemented sub-classes.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class AbstractTransferManager implements TransferManager {
	private static final Logger logger = Logger.getLogger(AbstractTransferManager.class.getSimpleName());
	private TransferSettings settings;

	public AbstractTransferManager(TransferSettings settings) {
		this.settings = settings;
	}

	public TransferSettings getConnection() {
		return settings;
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
			logger.log(Level.INFO, "- Running connect() ...");
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

	public void cleanTransactions(Config config) throws StorageException {
		Map<TransactionTO, TransactionRemoteFile> transactions = getTransactionTOs();
		RemoteTransaction remoteTransaction = new RemoteTransaction(config, this);
		for (TransactionTO transaction : transactions.keySet()) {
			if (transaction.getMachineName().equals(config.getMachineName())) {
				// Delete all permanent or temporary files in this transaction.
				for (ActionTO action : transaction.getActions()) {
					remoteTransaction.delete(action.getRemoteFile());
					remoteTransaction.delete(action.getTempRemoteFile());
				}

				// Get corresponding remote file of transaction and delete it.
				remoteTransaction.delete(transactions.get(transaction));
			}
		}
		remoteTransaction.commit();
	}

	/**
	 * Returns a Set of all files that are not temporary, but are listed in a 
	 * transaction file. These belong to an unfinished transaction and should be
	 * ignored.
	 */
	protected Set<RemoteFile> getFilesInTransactions(Set<TransactionTO> transactions) throws StorageException {
		Set<RemoteFile> filesInTransaction = new HashSet<RemoteFile>();

		for (TransactionTO transaction : transactions) {
			for (ActionTO action : transaction.getActions()) {
				if (action.getType().equals(ActionTO.TYPE_UPLOAD)) {
					filesInTransaction.add(action.getRemoteFile());
				}
			}
		}

		return filesInTransaction;
	}

	protected Set<RemoteFile> getDummyDeletedFiles(Set<TransactionTO> transactions) throws StorageException {
		Set<RemoteFile> dummyDeletedFiles = new HashSet<RemoteFile>();

		for (TransactionTO transaction : transactions) {
			for (ActionTO action : transaction.getActions()) {
				if (action.getType().equals(ActionTO.TYPE_DELETE)) {
					dummyDeletedFiles.add(action.getRemoteFile());
				}
			}
		}

		return dummyDeletedFiles;
	}

	protected Map<TransactionTO, TransactionRemoteFile> getTransactionTOs() throws StorageException {
		Map<String, TransactionRemoteFile> transactionFiles = list(TransactionRemoteFile.class);
		Map<TransactionTO, TransactionRemoteFile> transactions = new HashMap<TransactionTO, TransactionRemoteFile>();
		for (TransactionRemoteFile transaction : transactionFiles.values()) {

			try {
				File transactionFile = createTempFile("transaction");

				// Download transaction file
				download(transaction, transactionFile);
				String transactionFileStr = FileUtils.readFileToString(transactionFile);

				// Deserialize it
				Serializer serializer = new Persister();
				TransactionTO transactionTO = serializer.read(TransactionTO.class, transactionFileStr);

				// Extract final locations
				transactions.put(transactionTO, transaction);
				transactionFile.delete();
			}
			catch (Exception e) {
				throw new StorageException("Failed to read transactionFile", e);
			}
		}
		return transactions;
	}
}
