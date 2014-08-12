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

import org.syncany.plugins.Plugin;
import org.syncany.plugins.transfer.files.RemoteFile;

/**
 * The transfer plugin is a special plugin responsible for transferring files
 * to the remote storage. Implementations must provide implementations for
 * {@link TransferPlugin} (this class), {@link TransferSettings} (connection 
 * details) and {@link TransferManager} (transfer methods).
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class TransferPlugin extends Plugin {
	public TransferPlugin(String pluginId) {
		super(pluginId);
	}

	/**
	 * Creates an empty plugin-specific {@link TransferSettings} object.
	 * 
	 * <p>The created instance must be filled with sensible connection details
	 * and then initialized with the <tt>init()</tt> method.
	 */
	public abstract TransferSettings createSettings();
	
	/**
	 * Creates an initialized {@link TransferManager} object using the given
	 * connection details.
	 * 
	 * <p>The created instance can be used to upload/download/delete {@link RemoteFile}s
	 * and query the remote storage for a file list. 
	 */
	public abstract TransferManager createTransferManager(TransferSettings connection);
}
