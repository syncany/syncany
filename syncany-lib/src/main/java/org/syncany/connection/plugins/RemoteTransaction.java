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
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a transaction in a remote system. It will keep track of
 * what files are to be added and ensures atomic operation.
 * 
 * @author Pim Otte
 *
 */
public class RemoteTransaction {
	private TransferManager transferManger;
	private Map<File, RemoteFile> temporaryLocations;
	private Map<RemoteFile, RemoteFile> finalLocations;
	
	public RemoteTransaction(TransferManager transferManager) {
		this.transferManger = transferManager;
		temporaryLocations = new HashMap<File, RemoteFile>();
		finalLocations = new HashMap<RemoteFile, RemoteFile>();
	}
	/**
	 * Adds a file to this transaction. Generates a temporary file to store it.
	 */
	public void add(File localFile, RemoteFile remoteFile) {
		
	}
	/**
	 * Moves all files to the temporary remote location. If
	 * no errors occur, all files are moved to their final location.
	 */
	public void commit() throws StorageException {
		
	}

}
