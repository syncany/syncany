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
import java.util.logging.Level;
import java.util.logging.Logger;

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
	public StorageTestResult test() {
		logger.log(Level.INFO, "Performing plugin repo test ...");
		StorageTestResult result = null;
		
		try {
			logger.log(Level.INFO, "- Running connect() ...");
			connect();
	
			logger.log(Level.INFO, "- Running repoExists() ...");
			if (repoExists()) {
				logger.log(Level.INFO, "- Repo exists, running repoIsValid() ...");

				if (repoIsValid()) {
					logger.log(Level.INFO, "- Repo is valid: REPO_EXISTS.");
					result = StorageTestResult.REPO_EXISTS;
				}
				else {
					logger.log(Level.INFO, "- Repo is NOT valid: REPO_EXISTS_BUT_INVALID.");
					result = StorageTestResult.REPO_EXISTS_BUT_INVALID;
				}
			}
			else {
				logger.log(Level.INFO, "- Repo does NOT exist, running repoHasWriteAccess() ...");

				if (repoHasWriteAccess()) {
					logger.log(Level.INFO, "- Has write access: NO_REPO.");
					result = StorageTestResult.NO_REPO;
				}
				else {
					logger.log(Level.INFO, "- No write access: NO_REPO_CANNOT_CREATE.");
					result = StorageTestResult.NO_REPO_CANNOT_CREATE;
				}
			}	
		}
		catch (StorageException e) {
			logger.log(Level.INFO, "- Exception when testing repo: NO_CONNECTION.", e);
			result = StorageTestResult.NO_CONNECTION;
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
}
