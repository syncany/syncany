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
package org.syncany.plugins.transfer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.syncany.config.Config;
import org.syncany.plugins.Plugin;
import org.syncany.plugins.transfer.files.RemoteFile;
import org.syncany.util.ReflectionUtil;

/**
 * The transfer plugin is a special plugin responsible for transferring files
 * to the remote storage. Implementations must provide implementations for
 * {@link TransferPlugin} (this class), {@link TransferSettings} (connection
 * details) and {@link TransferManager} (transfer methods).<br/><br/>
 *
 * <p>Plugins have to follow a naming convention:
 * <ul>
 *   <li>Package names have to be lower snaked cased</li>
 *   <li>Class names have to be camel cased</li>
 *   <li>Package names will be converted to class names by replacing underscores ('_') and uppercasing the
 *      subsequent character.</li>
 * </ul>
 *
 * <p>Example:</b> 
 * A plugin is called DummyPlugin, hence <i>org.syncany.plugins.dummy_plugin.DummyPluginTransferPlugin</i> is the
 * plugin's {@link TransferPlugin} class and <i>org.syncany.plugins.dummy_plugin.DummyPluginTransferSettings</i> is the
 * corresponding {@link TransferSettings} implementation.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Christian Roth <christian.roth@port17.de>
 */
public abstract class TransferPlugin extends Plugin {
	public TransferPlugin(String pluginId) {
		super(pluginId);
	}

	/**
	 * Creates an empty plugin-specific {@link org.syncany.plugins.transfer.TransferSettings} instance.
	 *
	 * @return Empty plugin-specific {@link org.syncany.plugins.transfer.TransferSettings} instance.
	 * @throws StorageException Thrown if no {@link org.syncany.plugins.transfer.TransferSettings} are attached to a
	 *         plugin using {@link org.syncany.plugins.transfer.PluginSettings}
	 */
	@SuppressWarnings("unchecked")
	public final <T extends TransferSettings> T createEmptySettings() throws StorageException {
		final Class<? extends TransferSettings> transferSettings = TransferPluginUtil.getTransferSettingsClass(this.getClass());

		if (transferSettings == null) {
			throw new StorageException("TransferPlugin does not have any settings attached!");
		}

		try {
			return (T) transferSettings.newInstance();
		}
		catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("Unable to create TransferSettings: " + e.getMessage());
		}
	}

	 /**
	 * Creates an initialized, plugin-specific {@link org.syncany.plugins.transfer.TransferManager} object using the given
	 * connection details.
	 *
	 * <p>The created instance can be used to upload/download/delete {@link RemoteFile}s
	 * and query the remote storage for a file list.
	 *
	 * @param transferSettings A valid {@link org.syncany.plugins.transfer.TransferSettings} instance.
	 * @param config A valid {@link org.syncany.config.Config} instance.
	 * @return A initialized, plugin-specific {@link org.syncany.plugins.transfer.TransferManager} instance.
	 * @throws StorageException Thrown if no (valid) {@link org.syncany.plugins.transfer.TransferManager} are attached to
	*  a plugin using {@link org.syncany.plugins.transfer.PluginManager}
	 */
	@SuppressWarnings("unchecked")
	public final <T extends TransferManager> T createTransferManager(TransferSettings transferSettings, Config config) throws StorageException {
		if (!transferSettings.isValid()) {
			throw new StorageException("Unable to create transfer manager: connection isn't valid (perhaps missing some mandatory fields?)");
		}

		final Class<? extends TransferSettings> transferSettingsClass = TransferPluginUtil.getTransferSettingsClass(this.getClass());
		final Class<? extends TransferManager> transferManagerClass = TransferPluginUtil.getTransferManagerClass(this.getClass());

		if (transferSettingsClass == null) {
			throw new RuntimeException("Unable to create transfer manager: No settings class attached");
		}

		if (transferManagerClass == null) {
			throw new RuntimeException("Unable to create transfer manager: No manager class attached");
		}

		try {
			Constructor<?> potentialConstructor = ReflectionUtil.getMatchingConstructorForClass(transferManagerClass, TransferSettings.class,
					Config.class);

			if (potentialConstructor == null) {
				throw new RuntimeException("Invalid arguments for constructor in pluginclass -- must be 2 and subclass of " + TransferSettings.class
						+ " and " + Config.class);
			}

			return (T) potentialConstructor.newInstance(transferSettingsClass.cast(transferSettings), config);
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException("Unable to create transfer settings: " + e.getMessage(), e);
		}
	}
}
