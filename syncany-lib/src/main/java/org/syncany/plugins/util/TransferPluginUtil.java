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
package org.syncany.plugins.util;

import org.syncany.plugins.annotations.PluginManager;
import org.syncany.plugins.annotations.PluginSettings;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.TransferSettings;

/**
 * @author Christian Roth <christian.roth@port17.de>
 * @version 0.0.1
 */

public abstract class TransferPluginUtil {

	public static Class<? extends TransferSettings> getTransferSettingsClass(Class<? extends TransferPlugin> transferPluginClass)
			throws StorageException {

		PluginSettings settings = transferPluginClass.getAnnotation(PluginSettings.class);

		if (settings == null) {
			throw new StorageException("There are no transfer settings attached to that plugin (" + transferPluginClass.getName() + ")");
		}

		return settings.value();

	}

	public static Class<? extends TransferManager> getTransferManagerClass(Class<? extends TransferPlugin> transferPluginClass)
			throws StorageException {

		PluginManager manager = transferPluginClass.getAnnotation(PluginManager.class);

		if (manager == null) {
			throw new StorageException("There is no transfer manager attached to that plugin (" + transferPluginClass.getName() + ")");
		}

		return manager.value();

	}

}
