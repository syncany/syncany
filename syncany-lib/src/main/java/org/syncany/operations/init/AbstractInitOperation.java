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
package org.syncany.operations.init;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.config.LocalEventBus;
import org.syncany.config.to.ConfigTO;
import org.syncany.operations.Operation;
import org.syncany.operations.daemon.messages.ShowMessageExternalEvent;
import org.syncany.plugins.Plugins;
import org.syncany.plugins.UserInteractionListener;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.TransferManagerFactory.TransferManagerBuilder;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.plugins.transfer.features.ReadAfterWriteConsistent;
import org.syncany.util.EnvironmentUtil;

/**
 * The abstract init operation implements common functions of the {@link InitOperation}
 * and the {@link ConnectOperation}. Its sole purpose is to avoid duplicate code in these
 * similar operations.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class AbstractInitOperation extends Operation {
	protected static final Logger logger = Logger.getLogger(AbstractInitOperation.class.getSimpleName());

	protected UserInteractionListener listener;
	protected LocalEventBus eventBus;

	public AbstractInitOperation(Config config, UserInteractionListener listener) {
		super(config);

		this.listener = listener;
		this.eventBus = LocalEventBus.getInstance();
	}

	protected File createAppDirs(File localDir) throws IOException {
		if (localDir == null) {
			throw new RuntimeException("Unable to create app dir, local dir is null.");
		}

		File appDir = new File(localDir, Config.DIR_APPLICATION);
		File logDir = new File(appDir, Config.DIR_LOG);
		File cacheDir = new File(appDir, Config.DIR_CACHE);
		File databaseDir = new File(appDir, Config.DIR_DATABASE);
		File stateDir = new File(appDir, Config.DIR_STATE);

		appDir.mkdir();
		logDir.mkdir();
		cacheDir.mkdir();
		databaseDir.mkdir();
		stateDir.mkdir();

		if (EnvironmentUtil.isWindows()) {
			Files.setAttribute(Paths.get(appDir.getAbsolutePath()), "dos:hidden", true);
		}

		return appDir;
	}

	protected void deleteAppDirs(File localDir) throws IOException {
		File appDir = new File(localDir, Config.DIR_APPLICATION);
		File logDir = new File(appDir, Config.DIR_LOG);
		File cacheDir = new File(appDir, Config.DIR_CACHE);
		File databaseDir = new File(appDir, Config.DIR_DATABASE);

		for (File log : logDir.listFiles()) {
			log.delete();
		}

		for (File cache : cacheDir.listFiles()) {
			cache.delete();
		}

		for (File db : databaseDir.listFiles()) {
			db.delete();
		}

		for (File file : appDir.listFiles()) {
			file.delete();
		}

		logDir.delete();
		cacheDir.delete();
		databaseDir.delete();
		appDir.delete();
	}

	protected void fireNotifyCreateMaster() {
		eventBus.post(new ShowMessageExternalEvent("Creating master key from password (this might take a while) ..."));
	}

	protected TransferManager createTransferManagerFromNullConfig(ConfigTO configTo) throws IllegalAccessException,
					InvocationTargetException, InstantiationException, NoSuchMethodException, StorageException {

		// Init plugin and transfer manager
		TransferPlugin plugin = Plugins.get(configTo.getTransferSettings().getType(), TransferPlugin.class);

		TransferSettings transferSettings = configTo.getTransferSettings();
		transferSettings.setUserInteractionListener(listener);
		TransferManager transferManager = plugin.createTransferManager(transferSettings, config);

		// constructor is not visible and config seems to be null at this point, hence we cannot use the build method here
		Constructor<TransferManagerBuilder> tmbConstructor = TransferManagerBuilder.class.getDeclaredConstructor(Config.class, TransferManager.class);
		tmbConstructor.setAccessible(true);

		return tmbConstructor.newInstance(config, transferManager)
						.withFeature(ReadAfterWriteConsistent.class)
						.asDefault();
	}
}
