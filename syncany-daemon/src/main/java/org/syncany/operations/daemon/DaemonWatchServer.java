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

import org.syncany.config.ConfigException;
import org.syncany.config.UserConfig;
import org.syncany.config.to.DaemonConfigTO;
import org.syncany.config.to.FolderTO;
import org.syncany.operations.watch.WatchOperation;
import org.syncany.operations.watch.WatchOperationListener;
import org.syncany.operations.watch.WatchOperationOptions;

import com.google.common.collect.Maps;

/**
 * The watch server can manage many different {@link WatchOperation}s. When started
 * with {@link #start()} or {@link #reload()}, it first reads the daemon configuration file
 * and then runs new threads for each configured Syncany folder. Invalid or non-existing folders
 * are ignored.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class DaemonWatchServer implements WatchOperationListener {	
	private static final Logger logger = Logger.getLogger(DaemonWatchServer.class.getSimpleName());
	private static final String DAEMON_FILE = "daemon.xml";
	private static final String DEFAULT_FOLDER = "Syncany";
	
	private Map<File, WatchOperationThread> watchOperations;
	
	public DaemonWatchServer() {
		this.watchOperations = new TreeMap<File, WatchOperationThread>();
	}
	
	public void start() throws ConfigException {
		logger.log(Level.INFO, "Starting watch server ... ");
		reload();
	}
	
	public void reload() {		
		try {
			DaemonConfigTO daemonConfigTO = loadOrCreateConfig();
			
			Map<File, FolderTO> watchedFolders = getFolderMap(daemonConfigTO.getFolders());
			Map<File, FolderTO> newWatchedFolderTOs = determineNewWatchedFolderTOs(watchedFolders);
			List<File> removedWatchedFolderIds = determineRemovedWatchedFolderIds(watchedFolders);
			
			startWatchOperations(newWatchedFolderTOs);
			stopWatchOperations(removedWatchedFolderIds);			
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Cannot (re-)load config. Exception thrown.", e);
		}
	}

	public void stop() {
		logger.log(Level.INFO, "Stopping watch server ...  ");		
		Map<File, WatchOperationThread> copyOfWatchOperations = Maps.newHashMap(watchOperations);
		
		for (Map.Entry<File, WatchOperationThread> folderEntry : copyOfWatchOperations.entrySet()) {
			File localDir = folderEntry.getKey();
			WatchOperationThread watchOperationThread = folderEntry.getValue();
					
			logger.log(Level.INFO, "- Stopping watch operation at " + localDir + " ...");
			watchOperationThread.stop();
			
			watchOperations.remove(localDir);
		}
	}

	private void startWatchOperations(Map<File, FolderTO> newWatchedFolderTOs) throws ConfigException, ServiceAlreadyStartedException {
		for (Map.Entry<File, FolderTO> folderEntry : newWatchedFolderTOs.entrySet()) {
			File localDir = folderEntry.getKey();
			WatchOperationOptions watchOperationOptions = folderEntry.getValue().getWatchOptions();

			try {	
				logger.log(Level.INFO, "- Starting watch operation at " + localDir + " ...");
				
				WatchOperationThread watchOperationThread = new WatchOperationThread(localDir, watchOperationOptions, this);	
				watchOperationThread.start();

				watchOperations.put(localDir, watchOperationThread);
			}
			catch (Exception e) {
				logger.log(Level.SEVERE, "  + Cannot start watch operation at " + localDir + ". IGNORING.", e);
			}
		}
	}
	
	private void stopWatchOperations(List<File> removedWatchedFolderIds) {
		for (File localDir : removedWatchedFolderIds) {
			WatchOperationThread watchOperationThread = watchOperations.get(localDir);

			logger.log(Level.INFO, "- Stopping watch operation at " + localDir + " ...");
			watchOperationThread.stop();
			
			watchOperations.remove(localDir);
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
	
	private Map<File, FolderTO> determineNewWatchedFolderTOs(Map<File, FolderTO> watchedFolders) {
		Map<File, FolderTO> newWatchedFolderTOs = new TreeMap<File, FolderTO>();
		
		for (Map.Entry<File, FolderTO> folderEntry : watchedFolders.entrySet()) {
			File localDir = folderEntry.getKey();
			boolean isManaged = watchOperations.containsKey(localDir);
			
			if (!isManaged) {
				newWatchedFolderTOs.put(folderEntry.getKey(), folderEntry.getValue());
			}
		}
		
		return newWatchedFolderTOs;
	}

	private List<File> determineRemovedWatchedFolderIds(Map<File, FolderTO> watchedFolders) {
		List<File> removedWatchedFolderIds = new ArrayList<File>();
		
		for (File localDir : watchOperations.keySet()) {
			boolean isInConfig = watchedFolders.containsKey(localDir);
			
			if (!isInConfig) {
				removedWatchedFolderIds.add(localDir);
			}
		}
		
		return removedWatchedFolderIds;
	}

	private DaemonConfigTO loadOrCreateConfig() throws ConfigException {
		File configFile = new File(UserConfig.getUserConfigDir(), DAEMON_FILE);
		
		if (configFile.exists()) {
			return loadConfig(configFile);
		}
		else {
			return createAndWriteDefaultConfig(configFile);
		}
	}

	private DaemonConfigTO loadConfig(File configFile) throws ConfigException {
		return DaemonConfigTO.load(configFile);
	}

	private DaemonConfigTO createAndWriteDefaultConfig(File configFile) {
		File defaultFolder = new File(System.getProperty("user.home"), DEFAULT_FOLDER);
		
		FolderTO defaultFolderTO = new FolderTO();
		defaultFolderTO.setPath(defaultFolder.getAbsolutePath());
		
		ArrayList<FolderTO> folders = new ArrayList<>();
		folders.add(defaultFolderTO);
		
		DaemonConfigTO defaultDaemonConfigTO = new DaemonConfigTO();
		defaultDaemonConfigTO.setFolders(folders);
		
		try {
			DaemonConfigTO.save(defaultDaemonConfigTO, configFile);
		}
		catch (Exception e) {
			// Don't care!
		}
		
		return defaultDaemonConfigTO;
	}
	
	@Override
	public void onUploadStart(int fileCount) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUploadFile(String fileName, int fileNumber) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onIndexStart(int fileCount) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onIndexFile(String fileName, int fileNumber) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDownloadStart(int fileCount) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDownloadFile(String fileName, int fileNumber) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUploadEnd() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onIndexEnd() {
		// TODO Auto-generated method stub
		
	}

}
