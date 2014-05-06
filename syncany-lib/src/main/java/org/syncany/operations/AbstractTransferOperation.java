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

import org.syncany.config.Config;
import org.syncany.connection.plugins.ActionRemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;

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
	protected TransferManager transferManager;
	protected ActionFileHandler actionHandler;

	public AbstractTransferOperation(Config config, String operationName) {
		super(config);

		this.transferManager = config.getPlugin().createTransferManager(config.getConnection());
		this.actionHandler = new ActionFileHandler(transferManager, operationName, config.getMachineName());
	}
	
	protected void startOperation() throws Exception {
		actionHandler.start();
	}
	
	protected void finishOperation() throws StorageException {
		actionHandler.finish();
		
		disconnectTransferManager();
		clearCache();
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
