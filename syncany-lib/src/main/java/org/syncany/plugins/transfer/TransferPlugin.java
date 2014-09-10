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
import org.syncany.plugins.StorageException;
import org.syncany.plugins.annotations.PluginManager;
import org.syncany.plugins.annotations.PluginSettings;
import org.syncany.plugins.transfer.files.RemoteFile;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The transfer plugin is a special plugin responsible for transferring files
 * to the remote storage. Implementations must provide implementations for
 * {@link TransferPlugin} (this class), {@link TransferSettings} (connection
 * details) and {@link TransferManager} (transfer methods).
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Christian Roth <christian.roth@port17.de>
 */
public abstract class TransferPlugin extends Plugin {

	private static final Logger logger = Logger.getLogger(TransferPlugin.class.getName());

	public TransferPlugin(String pluginId) {
		super(pluginId);
	}

	/**
	 * Creates an empty plugin-specific {@link TransferSettings} object.
	 *
	 * <p>The created instance must be filled with sensible connection details
	 * and then initialized with the <tt>init()</tt> method.
	*
	* @deprecated
	 */
	public final TransferSettings createSettings() throws StorageException {
		return createEmptySettings();
	}

	public final TransferSettings createEmptySettings() throws StorageException {
		final Class<? extends TransferSettings> transferSettings = getTransferSettingsClass();
		if (transferSettings == null) {
			throw new StorageException("TransferPlugin does not have any settings attached!");
		}

		try {
			return transferSettings.newInstance();
		}
		catch (InstantiationException | IllegalAccessException e) {
			throw new StorageException("Unable to create TransferSettings: " + e.getMessage());
		}
	}

	/**
	 * Creates an initialized {@link TransferManager} object using the given
	 * connection details.
	 *
	 * <p>The created instance can be used to upload/download/delete {@link RemoteFile}s
	 * and query the remote storage for a file list.
	 */
	public final <T extends TransferManager> T createTransferManager(TransferSettings connection) throws StorageException {
		if (!connection.isValid()) {
			throw new StorageException("Unable to create transfermanager: connection isn't valid (perhaps missing some mandatory fields?)");
		}

		final Class<? extends TransferSettings> transferSettings = getTransferSettingsClass();
		final Class<? extends TransferManager> transferManager = getTransferManagerClass();

		if (transferSettings == null) {
			throw new StorageException("Unable to create transfermanager: No settings class attached");
		}
		if (transferManager == null) {
			throw new StorageException("Unable to create transfermanager: No manager class attached");
		}

		try {
			return (T) transferManager.getConstructor(TransferSettings.class).newInstance(transferSettings.cast(connection));
		}
		catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			throw new StorageException("Unable to create TransferSettings: " + e.getMessage());
		}

	}

	private Class<? extends TransferSettings> getTransferSettingsClass() {

		PluginSettings[] annotations = this.getClass().getAnnotationsByType(PluginSettings.class);

		if (annotations.length != 1) {
			logger.log(Level.SEVERE, "TransferPlugin does not have any settings attached!");
			return null;
		}

		return annotations[0].value();

	}

	private Class<? extends TransferManager> getTransferManagerClass() {

		PluginManager[] annotations = this.getClass().getAnnotationsByType(PluginManager.class);

		if (annotations.length != 1) {
			logger.log(Level.SEVERE, "TransferPlugin does not have an manager attached!");
			return null;
		}

		return annotations[0].value();

	}

}
