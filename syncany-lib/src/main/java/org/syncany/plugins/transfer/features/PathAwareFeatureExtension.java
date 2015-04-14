/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
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
package org.syncany.plugins.transfer.features;

import java.util.Map;

import org.syncany.plugins.transfer.FileType;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransferManager;

/**
 * The path aware feature extension must be defined in the {@link PathAware}
 * feature in order to extend a {@link TransferManager} that was marked as 'path aware'
 * with the required methods to manage the subfolders. 
 * 
 * <p>This extension creates, removes and lists contents in these subfolders.
 * 
 * @see PathAware
 * @see PathAwareFeatureTransferManager
 * @author Christian Roth <christian.roth@port17.de>
 */
public interface PathAwareFeatureExtension extends FeatureExtension {
	/**
	 * Creates the given sub path / folder on the remote storage.
	 *  
	 * @param path Path/folder to be created
	 * @return True if successful, false otherwise 
	 */
	public boolean createPath(String path) throws StorageException;

	/**
	 * Deleted the given sub path / folder on the remote storage.
	 *  
	 * @param path Path/folder to be deleted
	 * @return True if successful, false otherwise 
	 */
	public boolean removeFolder(String path) throws StorageException;
	
	/**
	 * Lists the contents of the given sub path / folder on the remote storage.
	 *  
	 * @param path Path/folder to be listed
	 * @return Filename/type list for the given subfolder 
	 */
	public Map<String, FileType> listFolder(String path) throws StorageException;
}
