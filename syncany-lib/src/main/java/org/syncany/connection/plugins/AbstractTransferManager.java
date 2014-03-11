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

/**
 * Implements basic functionality of a {@link TransferManager} which
 * can be implemented sub-classes.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class AbstractTransferManager implements TransferManager {
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
		StorageTestResult result = null;
		
		try {
			connect();
	
			if (repoExists()) {
				if (repoIsValid()) {
					result = StorageTestResult.REPO_EXISTS_BUT_INVALID;
				}
				else {
					result = StorageTestResult.REPO_EXISTS;
				}
			}
			else {
				if (repoHasWriteAccess()) {
					result = StorageTestResult.NO_REPO;
				}
				else {
					result = StorageTestResult.NO_REPO_CANNOT_CREATE;
				}
			}	
		}
		catch (StorageException e) {
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
