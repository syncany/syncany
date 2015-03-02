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
package org.syncany.operations.daemon;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.config.ConfigException;
import org.syncany.config.ConfigHelper;
import org.syncany.config.DaemonConfigHelper;
import org.syncany.config.LocalEventBus;
import org.syncany.config.to.DaemonConfigTO;
import org.syncany.config.to.FolderTO;
import org.syncany.operations.daemon.Watch.SyncStatus;
import org.syncany.operations.daemon.messages.AddWatchManagementRequest;
import org.syncany.operations.daemon.messages.AddWatchManagementResponse;
import org.syncany.operations.daemon.messages.BadRequestResponse;
import org.syncany.operations.daemon.messages.DaemonReloadedExternalEvent;
import org.syncany.operations.daemon.messages.ListWatchesManagementRequest;
import org.syncany.operations.daemon.messages.ListWatchesManagementResponse;
import org.syncany.operations.daemon.messages.RemoveWatchManagementRequest;
import org.syncany.operations.daemon.messages.RemoveWatchManagementResponse;
import org.syncany.operations.daemon.messages.api.FolderRequest;
import org.syncany.operations.daemon.messages.api.ManagementRequest;
import org.syncany.operations.daemon.messages.api.ManagementRequestHandler;
import org.syncany.operations.daemon.messages.api.Response;
import org.syncany.operations.watch.WatchOperation;
import org.syncany.operations.watch.WatchOperationOptions;

import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;

/**
 * The watch server can manage many different {@link WatchOperation}s. When started
 * with {@link #start()} or {@link #reload()}, it first reads the daemon configuration file
 * and then runs new threads for each configured Syncany folder. Invalid or non-existing folders
 * are ignored.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class WatchServer {
	private static final Logger logger = Logger.getLogger(WatchServer.class.getSimpleName());

	private DaemonConfigTO daemonConfig;
	private Map<File, WatchRunner> watchOperations;
	private LocalEventBus eventBus;

	public WatchServer() {
		this.daemonConfig = null;
		this.watchOperations = new TreeMap<File, WatchRunner>();

		this.eventBus = LocalEventBus.getInstance();
		this.eventBus.register(this);
	}

	public void start(DaemonConfigTO daemonConfigTO) {
		reload(daemonConfigTO);
	}

	public void reload(DaemonConfigTO daemonConfigTO) {
		logger.log(Level.INFO, "Starting/reloading watch server ... ");

		// Update config
		daemonConfig = daemonConfigTO;

		// Restart threads
		try {
			Map<File, FolderTO> watchedFolders = getFolderMap(daemonConfigTO.getFolders());

			stopAllWatchOperations();
			startWatchOperations(watchedFolders);

			fireDaemonReloadedEvent();
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Cannot (re-)load config. Exception thrown.", e);
		}
	}

	public void stop() {
		logger.log(Level.INFO, "Stopping watch server ...  ");
		Map<File, WatchRunner> copyOfWatchOperations = Maps.newHashMap(watchOperations);

		for (Map.Entry<File, WatchRunner> folderEntry : copyOfWatchOperations.entrySet()) {
			File localDir = folderEntry.getKey();
			WatchRunner watchOperationThread = folderEntry.getValue();

			logger.log(Level.INFO, "- Stopping watch operation at " + localDir + " ...");
			watchOperationThread.stop();

			watchOperations.remove(localDir);
		}
	}

	private void startWatchOperations(Map<File, FolderTO> newWatchedFolderTOs) throws ConfigException, ServiceAlreadyStartedException {
		for (Map.Entry<File, FolderTO> folderEntry : newWatchedFolderTOs.entrySet()) {
			File localDir = folderEntry.getKey();

			try {
				Config watchConfig = ConfigHelper.loadConfig(localDir);

				if (watchConfig != null) {
					logger.log(Level.INFO, "- Starting watch operation at " + localDir + " ...");

					WatchOperationOptions watchOptions = folderEntry.getValue().getWatchOptions();

					if (watchOptions == null) {
						watchOptions = new WatchOperationOptions();
					}

					WatchRunner watchRunner = new WatchRunner(watchConfig, watchOptions, daemonConfig.getPortTO());
					watchRunner.start();

					watchOperations.put(localDir, watchRunner);
				}
				else {
					logger.log(Level.INFO, "- CANNOT start watch, because no config found at " + localDir + " ...");
				}
			}
			catch (Exception e) {
				logger.log(Level.SEVERE, "  + Cannot start watch operation at " + localDir + ". IGNORING.", e);
			}
		}
	}

	/**
	 * Stops all watchOperations and verifies if
	 * they actually have stopped.
	 */
	private void stopAllWatchOperations() {
		for (File localDir : watchOperations.keySet()) {
			WatchRunner watchOperationThread = watchOperations.get(localDir);

			logger.log(Level.INFO, "- Stopping watch operation at " + localDir + " ...");
			watchOperationThread.stop();
		}

		// Check if watch operations actually have stopped.
		while (watchOperations.keySet().size() > 0) {
			Map<File, WatchRunner> watchOperationsCopy = new TreeMap<File, WatchRunner>(watchOperations);

			for (File localDir : watchOperationsCopy.keySet()) {
				WatchRunner watchOperationThread = watchOperationsCopy.get(localDir);

				if (watchOperationThread.hasStopped()) {
					logger.log(Level.INFO, "- Watch operation at " + localDir + " has stopped");
					watchOperations.remove(localDir);
				}
			}
		}
	}

	private Map<File, FolderTO> getFolderMap(List<FolderTO> watchedFolders) {
		Map<File, FolderTO> watchedFolderTOs = new TreeMap<File, FolderTO>();

		for (FolderTO folderTO : watchedFolders) {
			if (folderTO.isEnabled()) {
				watchedFolderTOs.put(new File(folderTO.getPath()), folderTO);
			}
		}

		return watchedFolderTOs;
	}

	private void fireDaemonReloadedEvent() {
		logger.log(Level.INFO, "Firing daemon-reloaded event ...");
		eventBus.post(new DaemonReloadedExternalEvent());
	}

	@Subscribe
	public void onFolderRequestReceived(FolderRequest folderRequest) {
		File rootFolder = new File(folderRequest.getRoot());

		if (!watchOperations.containsKey(rootFolder)) {
			eventBus.post(new BadRequestResponse(folderRequest.getId(), "Unknown root folder."));
		}
	}
	
	@Subscribe
	public void onManagementRequestReceived(ManagementRequest managementRequest) {
		logger.log(Level.INFO, "Received " + managementRequest);

		try {
			ManagementRequestHandler handler = ManagementRequestHandler.createManagementRequestHandler(managementRequest);
			Response response = handler.handleRequest(managementRequest);

			if (response != null) {
				eventBus.post(response);
			}
		}
		catch (ClassNotFoundException e) {
			logger.log(Level.FINE, "No handler found for management request class " + managementRequest.getClass() + ". Ignoring."); // Not logging 'e'!
		}
		catch (Exception e) {
			logger.log(Level.FINE, "Failed to process request", e);
			eventBus.post(new BadRequestResponse(managementRequest.getId(), "Invalid request."));
		}
	}

	@Subscribe
	public void onListWatchesRequestReceived(ListWatchesManagementRequest request) {
		List<Watch> watchList = new ArrayList<Watch>();

		for (File watchFolder : watchOperations.keySet()) {
			boolean syncRunning = watchOperations.get(watchFolder).isSyncRunning();
			SyncStatus syncStatus = (syncRunning) ? SyncStatus.SYNCING : SyncStatus.IN_SYNC;

			watchList.add(new Watch(watchFolder, syncStatus));
		}

		eventBus.post(new ListWatchesManagementResponse(request.getId(), watchList));
	}

	@Subscribe
	public void onAddWatchRequestReceived(AddWatchManagementRequest request) {
		File rootFolder = request.getWatch();

		if (watchOperations.containsKey(rootFolder)) {
			eventBus.post(new AddWatchManagementResponse(AddWatchManagementResponse.ERR_ALREADY_EXISTS, request.getId(), "Watch already exists."));
		}
		else {
			try {
				boolean folderAdded = DaemonConfigHelper.addFolder(rootFolder);

				if (folderAdded) {
					eventBus.post(new AddWatchManagementResponse(AddWatchManagementResponse.OKAY, request.getId(), "Successfully added."));
				}
				else {
					eventBus.post(new AddWatchManagementResponse(AddWatchManagementResponse.ERR_ALREADY_EXISTS, request.getId(),
							"Watch already exists (inactive/disabled)."));
				}
			}
			catch (ConfigException e) {
				logger.log(Level.WARNING, "Error adding watch to daemon config.", e);
				eventBus.post(new AddWatchManagementResponse(AddWatchManagementResponse.ERR_OTHER, request.getId(), "Error adding to config: "
						+ e.getMessage()));
			}
		}
	}
	
	@Subscribe
	public void onRemoveWatchRequestReceived(RemoveWatchManagementRequest request) {
		File rootFolder = request.getWatch();

		if (!watchOperations.containsKey(rootFolder)) {
			eventBus.post(new RemoveWatchManagementResponse(RemoveWatchManagementResponse.ERR_DOES_NOT_EXIST, request.getId(), "Watch does not exist."));
		}
		else {
			try {
				boolean folderRemoved = DaemonConfigHelper.removeFolder(rootFolder);

				if (folderRemoved) {
					eventBus.post(new RemoveWatchManagementResponse(RemoveWatchManagementResponse.OKAY, request.getId(), "Successfully removed."));
				}
				else {
					eventBus.post(new RemoveWatchManagementResponse(RemoveWatchManagementResponse.ERR_DOES_NOT_EXIST, request.getId(),
							"Watch does not exist (inactive/disabled)."));
				}
			}
			catch (ConfigException e) {
				logger.log(Level.WARNING, "Error removing watch from daemon config.", e);
				eventBus.post(new RemoveWatchManagementResponse(RemoveWatchManagementResponse.ERR_OTHER, request.getId(), "Error removing to config: "
						+ e.getMessage()));
			}
		}
	}
}
