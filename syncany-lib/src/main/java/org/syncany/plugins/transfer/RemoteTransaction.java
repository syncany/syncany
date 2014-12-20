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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.chunk.Transformer;
import org.syncany.config.Config;
import org.syncany.config.LocalEventBus;
import org.syncany.operations.daemon.messages.UpUploadFileInTransactionSyncExternalEvent;
import org.syncany.operations.daemon.messages.UpUploadFileSyncExternalEvent;
import org.syncany.plugins.transfer.files.RemoteFile;
import org.syncany.plugins.transfer.files.TempRemoteFile;
import org.syncany.plugins.transfer.files.TransactionRemoteFile;
import org.syncany.plugins.transfer.to.ActionTO;
import org.syncany.plugins.transfer.to.ActionTO.ActionStatus;
import org.syncany.plugins.transfer.to.ActionTO.ActionType;
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

	private LocalEventBus eventBus;

	public RemoteTransaction(Config config, TransferManager transferManager) {
		this(config, transferManager, new TransactionTO(config.getMachineName()));
	}

	public RemoteTransaction(Config config, TransferManager transferManager, TransactionTO transactionTO) {
		this.config = config;
		this.transferManager = transferManager;
		this.transactionTO = transactionTO;
		this.eventBus = LocalEventBus.getInstance();
	}

	/**
	 * Returns whether the transaction is empty.
	 */
	public boolean isEmpty() {
		return transactionTO.getActions().size() == 0;
	}

	/**
	 * Adds a file to this transaction. Generates a temporary file to store it.
	 */
	public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
		TempRemoteFile temporaryRemoteFile = new TempRemoteFile(remoteFile);

		logger.log(Level.INFO, "- Adding file to TX for UPLOAD: " + localFile + " -> Temp. remote file: " + temporaryRemoteFile
				+ ", final location: " + remoteFile);

		ActionTO action = new ActionTO();
		action.setType(ActionType.UPLOAD);
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
		TempRemoteFile temporaryRemoteFile = new TempRemoteFile(remoteFile);

		logger.log(Level.INFO, "- Adding file to TX for DELETE: " + remoteFile + "-> Temp. remote file: " + temporaryRemoteFile);

		ActionTO action = new ActionTO();
		action.setType(ActionType.DELETE);
		action.setRemoteLocation(remoteFile);
		action.setRemoteTempLocation(temporaryRemoteFile);

		transactionTO.addAction(action);
	}

	/**
	 * Commits this transaction by performing the required upload and
	 * delete operations. The method first moves all files to the temporary
	 * remote location. If no errors occur, all files are moved to their
	 * final location.
	 *
	 * <p>The method first writes a {@link TransactionRemoteFile} containing
	 * all actions to be performed and then uploads this file. Then it uploads
	 * new files (added by {@link #upload(File, RemoteFile) upload()} and moves
	 * deleted files to a temporary location (deleted by {@link #delete(RemoteFile) delete()}.
	 *
	 * <p>If this was successful, the transaction file is deleted and the
	 * temporary files. After deleting the transaction file, the transaction
	 * is successfully committed.
	 */
	public void commit() throws StorageException {
		logger.log(Level.INFO, "Starting TX.commit() ...");

		if (isEmpty()) {
			logger.log(Level.INFO, "- Empty transaction, not committing anything.");
			return;
		}

		File localTransactionFile = writeLocalTransactionFile();
		TransactionRemoteFile remoteTransactionFile = uploadTransactionFile(localTransactionFile);

		commit(localTransactionFile, remoteTransactionFile);
	}

	/**
	 * Does exactly the same as the paramterless version, except it does not create and upload the transactionfile. Instead
	 * it uses the files that are passed. Used for resuming existing transactions. Only call this function if resuming
	 * cannot cause invalid states.
	 */
	public void commit(File localTransactionFile, TransactionRemoteFile remoteTransactionFile) throws StorageException {
		logger.log(Level.INFO, "- Starting to upload data in commit.");

		uploadAndMoveToTempLocation();
		moveToFinalLocation();

		deleteTransactionFile(localTransactionFile, remoteTransactionFile);
		deleteTempRemoteFiles();
	}

	/**
	 * This method serializes the current state of the {@link RemoteTransaction} to a file.
	 * 
	 * @param transactionFile The file where the transaction will be written to.
	 */
	public void writeToFile(Transformer transformer, File transactionFile) throws StorageException {
		try {
			transactionTO.save(transformer, transactionFile);
			logger.log(Level.INFO, "Wrote transaction manifest to temporary file: " + transactionFile);
		}
		catch (Exception e) {
			throw new StorageException("Could not write transaction to file: " + transactionFile, e);
		}
	}

	private File writeLocalTransactionFile() throws StorageException {
		try {
			File localTransactionFile = config.getCache().createTempFile("transaction");
			writeToFile(config.getTransformer(), localTransactionFile);

			return localTransactionFile;
		}
		catch (Exception e) {
			throw new StorageException("Could not create temporary file for transaction", e);
		}
	}

	private TransactionRemoteFile uploadTransactionFile(File localTransactionFile) throws StorageException {
		TransactionRemoteFile remoteTransactionFile = new TransactionRemoteFile(this);

		eventBus.post(new UpUploadFileSyncExternalEvent(config.getLocalDir().getAbsolutePath(), remoteTransactionFile.getName()));

		logger.log(Level.INFO, "- Uploading remote transaction file {0} ...", remoteTransactionFile);
		transferManager.upload(localTransactionFile, remoteTransactionFile);

		return remoteTransactionFile;
	}

	private void uploadAndMoveToTempLocation() throws StorageException {
		TransactionStats stats = gatherTransactionStats();
		int uploadFileIndex = 0;

		for (ActionTO action : transactionTO.getActions()) {
			if (action.getStatus().equals(ActionStatus.UNSTARTED)) {
				// If we are resuming, this has not been started yet.
				RemoteFile tempRemoteFile = action.getTempRemoteFile();

				if (action.getType().equals(ActionType.UPLOAD)) {
					File localFile = action.getLocalTempLocation();
					long localFileSize = localFile.length();

					eventBus.post(new UpUploadFileInTransactionSyncExternalEvent(config.getLocalDir().getAbsolutePath(), ++uploadFileIndex,
							stats.totalUploadFileCount, localFileSize, stats.totalUploadSize));

					logger.log(Level.INFO, "- Uploading {0} to temp. file {1} ...", new Object[] { localFile, tempRemoteFile });
					transferManager.upload(localFile, tempRemoteFile);
					action.setStatus(ActionStatus.STARTED);
				}
				else if (action.getType().equals(ActionType.DELETE)) {
					RemoteFile remoteFile = action.getRemoteFile();

					try {
						logger.log(Level.INFO, "- Moving {0} to temp. file {1} ...", new Object[] { remoteFile, tempRemoteFile });
						transferManager.move(remoteFile, tempRemoteFile);
					}
					catch (StorageMoveException e) {
						logger.log(Level.INFO, "  -> FAILED (don't care!), because the remoteFile does not exist: " + remoteFile);
					}
					action.setStatus(ActionStatus.STARTED);
				}
			}
		}
	}

	private TransactionStats gatherTransactionStats() {
		TransactionStats stats = new TransactionStats();

		for (ActionTO action : transactionTO.getActions()) {
			if (action.getType().equals(ActionType.UPLOAD)) {
				stats.totalUploadFileCount++;
				stats.totalUploadSize += action.getLocalTempLocation().length();
			}
		}

		return stats;
	}

	private void moveToFinalLocation() throws StorageException {
		for (ActionTO action : transactionTO.getActions()) {
			if (action.getType().equals(ActionType.UPLOAD)) {
				RemoteFile tempRemoteFile = action.getTempRemoteFile();
				RemoteFile finalRemoteFile = action.getRemoteFile();

				logger.log(Level.INFO, "- Moving temp. file {0} to final location {1} ...", new Object[] { tempRemoteFile, finalRemoteFile });
				transferManager.move(tempRemoteFile, finalRemoteFile);
				action.setStatus(ActionStatus.DONE);
			}
		}
	}

	private void deleteTransactionFile(File localTransactionFile, TransactionRemoteFile remoteTransactionFile) throws StorageException {
		// After this deletion, the transaction is final!
		logger.log(Level.INFO, "- Deleting remote transaction file {0} ...", remoteTransactionFile);

		transferManager.delete(remoteTransactionFile);
		localTransactionFile.delete();

		logger.log(Level.INFO, "END of TX.commmit(): Succesfully committed transaction.");
	}

	private void deleteTempRemoteFiles() throws StorageException {
		// Actually deleting remote files is done after finishing the transaction, because
		// it cannot be rolled back! If this fails, the temporary files will eventually
		// be cleaned up by CleanUp and download will not download these, because
		// they are not in any transaction file.

		boolean success = true;
		for (ActionTO action : transactionTO.getActions()) {
			if (action.getStatus().equals(ActionStatus.STARTED)) {
				// If we are resuming, this action has not been comopleted.
				if (action.getType().equals(ActionType.DELETE)) {
					RemoteFile tempRemoteFile = action.getTempRemoteFile();

					logger.log(Level.INFO, "- Deleting temp. file {0}  ...", new Object[] { tempRemoteFile });
					try {
						transferManager.delete(tempRemoteFile);
					}
					catch (Exception e) {
						logger.log(Level.INFO, "Failed to delete: " + tempRemoteFile, " because of: " + e);
						success = false;
					}
					action.setStatus(ActionStatus.DONE);
				}
			}
		}

		if (success) {
			logger.log(Level.INFO, "END of TX.delTemp(): Sucessfully deleted final files.");
		}
		else {
			logger.log(Level.INFO, "END of TX.delTemp(): Did not succesfully delete all files!");
		}
	}

	private class TransactionStats {
		private long totalUploadSize;
		private int totalUploadFileCount;
	}
}
