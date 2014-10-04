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
import org.syncany.config.LocalEventBus;
import org.syncany.config.to.DaemonConfigTO;
import org.syncany.config.to.FolderTO;
import org.syncany.operations.ChangeSet;
import org.syncany.operations.daemon.messages.BadRequestResponse;
import org.syncany.operations.daemon.messages.DownEndSyncExternalEvent;
import org.syncany.operations.daemon.messages.ListWatchesManagementRequest;
import org.syncany.operations.daemon.messages.ListWatchesManagementResponse;
import org.syncany.operations.daemon.messages.api.FolderRequest;
import org.syncany.operations.daemon.messages.api.ManagementRequest;
import org.syncany.operations.down.DownOperationResult;
import org.syncany.operations.watch.WatchOperation;
import org.syncany.operations.watch.WatchOperationOptions;
import org.syncany.util.StringUtil;

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
		this.daemonConfig = daemonConfigTO;
		
		// Restart threads
		try {
			Map<File, FolderTO> watchedFolders = getFolderMap(daemonConfigTO.getFolders());
			
			stopAllWatchOperations();
			startWatchOperations(watchedFolders);
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
	
	@Subscribe
	public void onManagementRequestReceived(ManagementRequest request) {
		if (request instanceof ListWatchesManagementRequest) {
			processListWatchesRequest((ListWatchesManagementRequest) request);
		}		
	}

	@Subscribe
	public void onFolderRequestReceived(FolderRequest folderRequest) {
		File rootFolder = new File(folderRequest.getRoot());
		
		if (!watchOperations.containsKey(rootFolder)) {
			eventBus.post(new BadRequestResponse(folderRequest.getId(), "Unknown root folder."));
		}
	}
	
	private void processListWatchesRequest(ListWatchesManagementRequest request) {
		eventBus.post(new ListWatchesManagementResponse(request.getId(), new ArrayList<File>(watchOperations.keySet())));
	}
	
	@Subscribe
	public void onPostDownOperation(DownEndSyncExternalEvent downEndSyncEvent) {
		if (daemonConfig.getHooks() != null) {
			String runAfterSyncCommand = daemonConfig.getHooks().getRunAfterDownCommand();
			
			if (runAfterSyncCommand != null) {
				DownOperationResult downOperationResult = downEndSyncEvent.getResult();
				ChangeSet changeSet = downOperationResult.getChangeSet();
				
				List<String> changeMessageParts = new ArrayList<>();
				
				if (changeSet.getNewFiles().size() > 0) {
					changeMessageParts.add(changeSet.getNewFiles().size() + " file(s) added");
				}
				
				if (changeSet.getChangedFiles().size() > 0) {
					changeMessageParts.add(changeSet.getChangedFiles().size() + " file(s) changed");
				}
				
				if (changeSet.getDeletedFiles().size() > 0) {
					changeMessageParts.add(changeSet.getDeletedFiles().size() + " file(s) deleted");
				}
				
				String changedMessage = StringUtil.join(changeMessageParts, ", ");
				
				String escapedSubject = changedMessage.replace("\"", "\\\"");			
				runAfterSyncCommand = runAfterSyncCommand.replace("%subject", escapedSubject);
							
				try {
					logger.log(Level.INFO, "Running command: " + runAfterSyncCommand);
					
					List<String> commandArgsList = StringUtil.splitCommandLineArgs(runAfterSyncCommand);
					String[] commandArgs = commandArgsList.toArray(new String[0]);
				    
					Runtime.getRuntime().exec(commandArgs);
				}
				catch (Exception e) {
					logger.log(Level.WARNING, "Cannot run sync after command: " + runAfterSyncCommand, e);
				}
			}
		}
	}	
}
