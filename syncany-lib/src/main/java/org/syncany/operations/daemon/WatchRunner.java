/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com>
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
package org.syncany.operations.daemon;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config;
import org.syncany.config.ConfigException;
import org.syncany.config.LocalEventBus;
import org.syncany.config.to.PortTO;
import org.syncany.operations.daemon.messages.AlreadySyncingResponse;
import org.syncany.operations.daemon.messages.BadRequestResponse;
import org.syncany.operations.daemon.messages.api.FolderRequest;
import org.syncany.operations.daemon.messages.api.FolderRequestHandler;
import org.syncany.operations.daemon.messages.api.Response;
import org.syncany.operations.watch.WatchOperation;
import org.syncany.operations.watch.WatchOperationOptions;
import org.syncany.operations.watch.WatchOperationResult;

import com.google.common.eventbus.Subscribe;

/**
 * The watch operation thread runs a {@link WatchOperation} in a thread. The
 * underlying thred can be started using the {@link #start()} method, and stopped
 * gracefully using {@link #stop()}.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class WatchRunner {
	private static final Logger logger = Logger.getLogger(WatchRunner.class.getSimpleName());

	private Config config;
	private PortTO portTO;
	private Thread watchThread;
	private WatchOperation watchOperation;
	private WatchOperationResult watchOperationResult;
	private LocalEventBus eventBus;

	public WatchRunner(Config config, WatchOperationOptions watchOperationOptions, PortTO portTO) throws ConfigException {
		this.config = config;
		this.portTO = portTO;
		this.watchOperation = new WatchOperation(config, watchOperationOptions);

		this.eventBus = LocalEventBus.getInstance();
		this.eventBus.register(this);
	}

	public void start() {
		watchThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					logger.log(Level.INFO, "STARTING watch at" + config.getLocalDir());
					watchOperationResult = null;

					// Write port to portFile
					File portFile = config.getPortFile();

					portFile.createNewFile();
					portFile.deleteOnExit();

					new Persister().write(portTO, portFile);

					// Start operation (blocks!)
					watchOperationResult = watchOperation.execute();

					logger.log(Level.INFO, "STOPPED watch at " + config.getLocalDir());
				}
				catch (Exception e) {
					logger.log(Level.SEVERE, "ERROR while running watch at " + config.getLocalDir(), e);
				}
			}
		}, "WR/" + config.getLocalDir().getName());

		watchThread.start();
	}

	public void stop() {
		watchOperation.stop();
		config.getPortFile().delete();

		watchThread = null;
	}

	public boolean hasStopped() {
		return watchOperationResult != null;
	}

	public boolean isSyncRunning() {
		return watchOperation.isSyncRunning();
	}

	@Subscribe
	public void onRequestReceived(FolderRequest folderRequest) {
		File requestRootFolder = new File(folderRequest.getRoot());
		boolean localDirMatches = requestRootFolder.equals(config.getLocalDir());

		if (localDirMatches) {
			logger.log(Level.INFO, "Received " + folderRequest);

			try {
				if (!watchOperation.isSyncRunning() && !watchOperation.isSyncRequested()) {
					watchOperation.pause();

					FolderRequestHandler handler = FolderRequestHandler.createFolderRequestHandler(folderRequest, config);
					Response response = handler.handleRequest(folderRequest);

					if (response != null) {
						eventBus.post(response);
					}

					watchOperation.resume();
				}
				else {
					logger.log(Level.WARNING, "FolderRequest discarded : ", folderRequest);
					eventBus.post(new AlreadySyncingResponse(folderRequest.getId(), "FolderRequest discarded."));
				}
			}
			catch (Exception e) {
				logger.log(Level.FINE, "Failed to process request", e);
				eventBus.post(new BadRequestResponse(folderRequest.getId(), "Invalid request."));
			}
		}
	}
}
