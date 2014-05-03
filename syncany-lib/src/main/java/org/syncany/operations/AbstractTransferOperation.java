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

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.connection.plugins.ActionRemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;

public abstract class AbstractTransferOperation extends Operation {
	private static final Logger logger = Logger.getLogger(AbstractTransferOperation.class.getSimpleName());

	protected TransferManager transferManager;
	protected ActionRemoteFile actionFile;

	public AbstractTransferOperation(Config config, String operationName) {
		super(config);

		this.transferManager = config.getPlugin().createTransferManager(config.getConnection());
		this.actionFile = createActionFile(operationName);
	}

	protected ActionRemoteFile createActionFile(String operationName) {
		try {
			return new ActionRemoteFile(operationName, config.getMachineName(), System.currentTimeMillis());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected void deleteActionFile() throws StorageException {
		logger.log(Level.INFO, "Deleting action file: " + actionFile);
		transferManager.delete(actionFile);
	}

	protected void uploadActionFile() throws Exception {
		logger.log(Level.INFO, "Uploading action file: " + actionFile);

		File tempActionFile = config.getCache().createTempFile(actionFile.getName());
		transferManager.upload(tempActionFile, actionFile);
	}

	protected void disconnectTransferManager() {
		try {
			transferManager.disconnect();
		}
		catch (StorageException e) {
			// Don't care!
		}
	}

	protected void clearCache() {
		config.getCache().clear();
	}

	protected void finishOperation() throws StorageException {
		deleteActionFile();
		disconnectTransferManager();
		clearCache();
	}
}
