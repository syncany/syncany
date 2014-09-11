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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.chunk.Transformer;
import org.syncany.config.Config;
import org.syncany.plugins.transfer.files.RemoteFile;
import org.syncany.plugins.transfer.files.TempRemoteFile;
import org.syncany.plugins.transfer.files.TransactionRemoteFile;
import org.syncany.plugins.transfer.to.ActionTO;
import org.syncany.plugins.transfer.to.TransactionTO;

/**
 * The TransactionAwareTransferManager adds all functionality regarding transactions
 * to existing transfer managers. 
 * 
 * @author Pim Otte
 */
public class TransactionAwareTransferManager implements TransferManager {
	private static final Logger logger = Logger.getLogger(TransactionAwareTransferManager.class.getSimpleName());

	private TransferManager underlyingTransferManager;
	private Config config;

	public TransactionAwareTransferManager(TransferManager underlyingTransferManager, Config config) {
		this.underlyingTransferManager = underlyingTransferManager;
		this.config = config;
	}

	@Override
	public void connect() throws StorageException {
		underlyingTransferManager.connect();
	}

	@Override
	public void disconnect() throws StorageException {
		underlyingTransferManager.disconnect();
	}

	@Override
	public void init(final boolean createIfRequired) throws StorageException {
		underlyingTransferManager.init(createIfRequired);
	}

	@Override
	public void download(final RemoteFile remoteFile, final File localFile) throws StorageException {
		try {
			underlyingTransferManager.download(remoteFile, localFile);
		}
		catch (StorageFileNotFoundException e) {
			downloadDeletedTempFileInTransaction(remoteFile, localFile);
		}
	}

	/**
	 * Downloads all transaction files and looks for the corresponding temporary file 
	 * for the given remote file. If there is a temporary file, the file is downloaded 
	 * instead of the original file. 
	 * 
	 * <p>This method is <b>expensive</b>, but it is only called by {@link #download(RemoteFile, File) download()}
	 * if a file does not exist.
	 */
	private void downloadDeletedTempFileInTransaction(RemoteFile remoteFile, File localFile) throws StorageException {
		logger.log(Level.INFO, "File {0} not found, checking if it is being deleted ...", remoteFile.getName());

		Set<TransactionTO> transactions = retrieveRemoteTransactions().keySet();
		TempRemoteFile tempRemoteFile = null;

		// Find file: If the file is being deleted and the name matches, download temporary file instead.
		for (TransactionTO transaction : transactions) {
			for (ActionTO action : transaction.getActions()) {
				if (action.getType().equals(ActionTO.TYPE_DELETE) && action.getRemoteFile().equals(remoteFile)) {
					tempRemoteFile = action.getTempRemoteFile();
					break;
				}
			}
		}

		// Download file, or throw exception
		if (tempRemoteFile != null) {
			logger.log(Level.INFO, "-> File {0} in process of being deleted; downloading corresponding temp. file {1} ...",
					new Object[] { remoteFile.getName(), tempRemoteFile.getName() });

			underlyingTransferManager.download(tempRemoteFile, localFile);
		}
		else {
			logger.log(Level.WARNING, "-> File {0} does not exist and is not in any transaction. Throwing exception.", remoteFile.getName());
			throw new StorageFileNotFoundException("File " + remoteFile.getName() + " does not exist and is not in any transaction");
		}
	}

	@Override
	public void move(final RemoteFile sourceFile, final RemoteFile targetFile) throws StorageException {
		underlyingTransferManager.move(sourceFile, targetFile);
	}

	@Override
	public void upload(final File localFile, final RemoteFile remoteFile) throws StorageException {
		underlyingTransferManager.upload(localFile, remoteFile);
	}

	@Override
	public boolean delete(final RemoteFile remoteFile) throws StorageException {
		return underlyingTransferManager.delete(remoteFile);
	}

	@Override
	public <T extends RemoteFile> Map<String, T> list(final Class<T> remoteFileClass) throws StorageException {
		return addAndFilterFilesInTransaction(remoteFileClass, underlyingTransferManager.list(remoteFileClass));
	}

	/**
	 * Checks if any transactions of the local machine were not completed and performs
	 * a rollback if any transactions were found. The rollback itself is performed in 
	 * a transaction.
	 * 
	 * <p>The method uses {@link #retrieveRemoteTransactions()} to download all transaction
	 * files and then rolls back the local machines's transactions: 
	 * 
	 * <ul>
	 *  <li>Files in the transaction marked "UPLOAD" are deleted.</li>
	 *  <li>Files in the transaction marked "DELETE" are moved back to their original place.</li>
	 * </ul>
	 */
	public void cleanTransactions() throws StorageException {
		Objects.requireNonNull(config, "Cannot clean transactions if config is null.");

		Map<TransactionTO, TransactionRemoteFile> transactions = retrieveRemoteTransactions();
		RemoteTransaction rollbackTransaction = new RemoteTransaction(config, this);

		for (TransactionTO potentiallyCancelledTransaction : transactions.keySet()) {
			boolean isCancelledOwnTransaction = potentiallyCancelledTransaction.getMachineName().equals(config.getMachineName());

			// If this transaction is from our machine, delete all permanent or temporary files in this transaction.
			if (isCancelledOwnTransaction) {
				rollbackSingleTransaction(rollbackTransaction, potentiallyCancelledTransaction, transactions.get(potentiallyCancelledTransaction));
			}
		}

		// Execute transaction (if it isn't empty)
		if (!rollbackTransaction.isEmpty()) {
			logger.log(Level.INFO, "Clean TX: Committing rollback transaction ...");
			rollbackTransaction.commit();
		}
		else {
			logger.log(Level.INFO, "Clean TX: No stale transactions found. No cleansing necessary.");
		}
	}

	/**
	 * Removes temporary files on the offsite storage that are not listed in any
	 * of the {@link TransactionRemoteFile}s available remotely. 
	 * 
	 * <p>Temporary files might be left over from unfinished transactions.
	 */
	public void removeUnreferencedTemporaryFiles() throws StorageException {
		// Retrieve all transactions
		Map<TransactionTO, TransactionRemoteFile> transactions = retrieveRemoteTransactions();
		Collection<TempRemoteFile> tempRemoteFiles = list(TempRemoteFile.class).values();

		// Find all remoteFiles that are referenced in a transaction
		Set<TempRemoteFile> tempRemoteFilesInTransactions = new HashSet<TempRemoteFile>();

		for (TransactionTO transaction : transactions.keySet()) {
			for (ActionTO action : transaction.getActions()) {
				tempRemoteFilesInTransactions.add(action.getTempRemoteFile());
			}
		}

		// Consider just those files that are not referenced and delete them.
		tempRemoteFiles.removeAll(tempRemoteFilesInTransactions);

		for (TempRemoteFile unreferencedTempRemoteFile : tempRemoteFiles) {
			logger.log(Level.INFO, "Unreferenced temporary file found. Deleting {0}", unreferencedTempRemoteFile);
			underlyingTransferManager.delete(unreferencedTempRemoteFile);
		}

	}

	/**
	 * This method is called when the machine wants to rollback one of their own transactions.
	 * 
	 * @param rollbackTransaction is the transaction that composes the rollback.
	 * @param cancelledTransaction is the transaction that is cancelled.
	 * @param remoteCancelledTransaction is the remote file location of the cancelled transaction. 
	 *        This file will be deleted as part of the rollback. 
	 */
	private void rollbackSingleTransaction(RemoteTransaction rollbackTransaction, TransactionTO cancelledTransaction,
			TransactionRemoteFile remoteCancelledTransaction) throws StorageException {

		TransactionTO cancelledOwnTransaction = cancelledTransaction;
		addRollbackActionsToTransaction(rollbackTransaction, cancelledOwnTransaction.getActions());

		// Get corresponding remote file of transaction and delete it.
		TransactionRemoteFile cancelledOwnTransactionFile = remoteCancelledTransaction;
		rollbackTransaction.delete(cancelledOwnTransactionFile);

		// Nicer debug output
		if (logger.isLoggable(Level.INFO)) {
			logger.log(Level.INFO, "Unfinished transaction " + cancelledOwnTransactionFile + ". Rollback necessary!");

			for (ActionTO action : cancelledOwnTransaction.getActions()) {
				logger.log(Level.INFO, "- Needs to be rolled back: " + action);
			}
		}
	}

	/**
	 * Adds the opposite actions (rollback actions) for the given unfinished actions 
	 * to the rollback transaction.  
	 */
	private void addRollbackActionsToTransaction(RemoteTransaction rollbackTransaction, List<ActionTO> unfinishedActions) throws StorageException {
		for (ActionTO action : unfinishedActions) {
			switch (action.getType()) {
			case ActionTO.TYPE_UPLOAD:
				rollbackTransaction.delete(action.getRemoteFile());
				rollbackTransaction.delete(action.getTempRemoteFile());

				break;

			case ActionTO.TYPE_DELETE:
				try {
					logger.log(Level.INFO, "- Rollback action: Moving " + action.getTempRemoteFile().getName() + " to "
							+ action.getRemoteFile().getName());
					move(action.getTempRemoteFile(), action.getRemoteFile());
				}
				catch (StorageMoveException e) {
					logger.log(Level.WARNING, "Restoring deleted file failed. This might be a problem if the original: " + action.getRemoteFile()
							+ " also does not exist.");
				}

				break;

			default:
				throw new RuntimeException("Transaction contains invalid type: " + action.getType() + ". This should not happen.");
			}
		}
	}

	@Override
	public StorageTestResult test(boolean testCreateTarget) {
		return underlyingTransferManager.test(testCreateTarget);
	}

	@Override
	public boolean testTargetExists() throws StorageException {
		return underlyingTransferManager.testTargetExists();
	}

	@Override
	public boolean testTargetCanWrite() throws StorageException {
		return underlyingTransferManager.testTargetCanWrite();
	}

	@Override
	public boolean testTargetCanCreate() throws StorageException {
		return underlyingTransferManager.testTargetCanCreate();
	}

	@Override
	public boolean testRepoFileExists() throws StorageException {
		return underlyingTransferManager.testRepoFileExists();
	}

	/**
	 * Returns a list of remote files, excluding the files in transactions. 
	 * The method is used to hide unfinished transactions from other clients.
	 */
	protected <T extends RemoteFile> Map<String, T> addAndFilterFilesInTransaction(Class<T> remoteFileClass, Map<String, T> remoteFiles)
			throws StorageException {

		Map<String, T> filteredFiles = new HashMap<String, T>();

		Set<TransactionTO> transactions = new HashSet<TransactionTO>();
		Set<RemoteFile> dummyDeletedFiles = new HashSet<RemoteFile>();
		Set<RemoteFile> filesToIgnore = new HashSet<RemoteFile>();

		// Ignore files currently listed in a transaction,
		// unless we are listing transaction files

		boolean ignoreFilesInTransactions = !remoteFileClass.equals(TransactionRemoteFile.class);

		if (ignoreFilesInTransactions) {
			transactions = retrieveRemoteTransactions().keySet();
			filesToIgnore = getFilesInTransactions(transactions);
			dummyDeletedFiles = getDummyDeletedFiles(transactions);
		}

		for (RemoteFile deletedFile : dummyDeletedFiles) {
			if (deletedFile.getClass().equals(remoteFileClass)) {
				T concreteDeletedFile = remoteFileClass.cast(deletedFile);
				filteredFiles.put(concreteDeletedFile.getName(), concreteDeletedFile);
			}
		}

		for (String fileName : remoteFiles.keySet()) {
			if (!filesToIgnore.contains(remoteFiles.get(fileName))) {
				filteredFiles.put(fileName, remoteFiles.get(fileName));
			}
		}

		return filteredFiles;
	}

	/**
	 * Returns a Set of all files that are not temporary, but are listed in a 
	 * transaction file. These belong to an unfinished transaction and should be ignored.
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

	private Set<RemoteFile> getDummyDeletedFiles(Set<TransactionTO> transactions) throws StorageException {
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

	private Map<TransactionTO, TransactionRemoteFile> retrieveRemoteTransactions() throws StorageException {
		Map<String, TransactionRemoteFile> transactionFiles = list(TransactionRemoteFile.class);
		Map<TransactionTO, TransactionRemoteFile> transactions = new HashMap<TransactionTO, TransactionRemoteFile>();

		for (TransactionRemoteFile transaction : transactionFiles.values()) {
			try {
				File transactionFile = createTempFile("transaction");

				// Download transaction file
				download(transaction, transactionFile);

				Transformer transformer = config == null ? null : config.getTransformer();
				TransactionTO transactionTO = TransactionTO.load(transformer, transactionFile);

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

	/**
	 * Creates a temporary file, either using the config (if initialized) or 
	 * using the global temporary directory.
	 */
	protected File createTempFile(String name) throws IOException {
		// TODO [low] duplicate code with AbstractTransferManager

		if (config == null) {
			return File.createTempFile(String.format("temp-%s-", name), ".tmp");
		}
		else {
			return config.getCache().createTempFile(name);
		}
	}
}
