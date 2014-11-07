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
package org.syncany.operations;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.plugins.transfer.RetriableTransferManager;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransactionAwareTransferManager;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.files.ActionRemoteFile;

/**
 * Represents and is inherited by a transfer operation. Transfer operations are operations
 * that modify the repository and/or are relevant for the consistency of the local directory
 * or the remote repository.
 *
 * <p>This abstract class offers convenience methods to handle {@link ActionRemoteFile} as well
 * as to handle the connection and local cache.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class AbstractTransferOperation extends Operation {
	private static final Logger logger = Logger.getLogger(AbstractTransferOperation.class.getSimpleName());

	/**
	 * Defines the time after which old/outdated action files from other clients are
	 * deleted. This time must be significantly larger than the time action files are
	 * renewed by the {@link ActionFileHandler}.
	 *
	 * @see ActionFileHandler#ACTION_RENEWAL_INTERVAL
	 */
	private static final int ACTION_FILE_DELETE_TIME = ActionFileHandler.ACTION_RENEWAL_INTERVAL + 5 * 60 * 1000; // Minutes

	protected TransactionAwareTransferManager transferManager;
	protected ActionFileHandler actionHandler;

	public AbstractTransferOperation(Config config, String operationName) {
		super(config);

		// Do NOT reuse TransferManager for action file renewal; see #140

		try {
			this.actionHandler = new ActionFileHandler(createReliableTransferManager(config), operationName, config.getMachineName());
			this.transferManager = createReliableTransferManager(config);
		}
		catch (StorageException e) {
			logger.log(Level.SEVERE, "Unable to create AbstractTransferOperation: Unable to create TransferManager", e);
			throw new RuntimeException("Unable to create AbstractTransferOperation: Unable to create TransferManager: " + e.getMessage());
		}
	}

	private TransactionAwareTransferManager createReliableTransferManager(Config config) throws StorageException {
		return new TransactionAwareTransferManager(createRetriableTransferManager(config), config);
	}

	private TransferManager createRetriableTransferManager(Config config) throws StorageException {
		return new RetriableTransferManager(config.getTransferPlugin().createTransferManager(config.getConnection(), config));
	}

	protected void startOperation() throws Exception {
		actionHandler.start();
	}

	protected void finishOperation() throws StorageException {
		actionHandler.finish();

		cleanActionFiles();
		disconnectTransferManager();
		clearCache();
	}

	protected boolean otherRemoteOperationsRunning(String... operationIdentifiers) throws StorageException {
		logger.log(Level.INFO, "Looking for other running remote operations ...");
		Map<String, ActionRemoteFile> actionRemoteFiles = transferManager.list(ActionRemoteFile.class);

		boolean otherRemoteOperationsRunning = false;
		List<String> disallowedOperationIdentifiers = Arrays.asList(operationIdentifiers);

		for (ActionRemoteFile actionRemoteFile : actionRemoteFiles.values()) {
			String operationName = actionRemoteFile.getOperationName();
			String machineName = actionRemoteFile.getClientName();

			boolean isOwnActionFile = machineName.equals(config.getMachineName());
			boolean isOperationAllowed = !disallowedOperationIdentifiers.contains(operationName);
			boolean isOutdatedActionFile = isOutdatedActionFile(actionRemoteFile);

			if (!isOwnActionFile) {
				if (!isOutdatedActionFile) {
					if (isOperationAllowed) {
						logger.log(Level.INFO, "- Action file from other client, but allowed operation; not marking running; " + actionRemoteFile);
					}
					else {
						logger.log(Level.INFO, "- Action file from other client; --> marking operations running (!); " + actionRemoteFile);
						otherRemoteOperationsRunning = true;
					}
				}
				else {
					logger.log(Level.INFO, "- Action file outdated; ignoring " + actionRemoteFile);
				}
			}
		}

		return otherRemoteOperationsRunning;
	}

	private void cleanActionFiles() throws StorageException {
		logger.log(Level.INFO, "Cleaning own old action files ...");
		Map<String, ActionRemoteFile> actionRemoteFiles = transferManager.list(ActionRemoteFile.class);

		for (ActionRemoteFile actionRemoteFile : actionRemoteFiles.values()) {
			String machineName = actionRemoteFile.getClientName();

			boolean isOwnActionFile = machineName.equals(config.getMachineName());
			boolean isOutdatedActionFile = isOutdatedActionFile(actionRemoteFile);

			if (isOwnActionFile) {
				logger.log(Level.INFO, "- Deleting own action file " + actionRemoteFile + " ...");
				transferManager.delete(actionRemoteFile);
			}
			else if (isOutdatedActionFile) {
				logger.log(Level.INFO, "- Action file from other client is OUTDATED; deleting " + actionRemoteFile + " ...");
				transferManager.delete(actionRemoteFile);
			}
			else {
				logger.log(Level.INFO, "- Action file is current; ignoring " + actionRemoteFile + " ...");
			}
		}
	}

	private boolean isOutdatedActionFile(ActionRemoteFile actionFile) {
		// TODO [low] Even though this is UTC and the times frames are large, this might be an issue with different timezones or wrong system clocks
		return System.currentTimeMillis() - ACTION_FILE_DELETE_TIME > actionFile.getTimestamp();
	}

	private void disconnectTransferManager() {
		try {
			transferManager.disconnect();
		}
		catch (StorageException e) {
			// Don't care!
		}
	}

	private void clearCache() {
		config.getCache().clear();
	}
}
