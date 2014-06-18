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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.config.ConfigException;
import org.syncany.config.ConfigHelper;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.daemon.messages.BadRequestResponse;
import org.syncany.operations.daemon.messages.GetDatabaseVersionHeadersRequest;
import org.syncany.operations.daemon.messages.GetDatabaseVersionHeadersResponse;
import org.syncany.operations.daemon.messages.GetFileHistoryRequest;
import org.syncany.operations.daemon.messages.GetFileHistoryResponse;
import org.syncany.operations.daemon.messages.GetFileTreeRequest;
import org.syncany.operations.daemon.messages.GetFileTreeResponse;
import org.syncany.operations.daemon.messages.WatchEventResponse;
import org.syncany.operations.daemon.messages.WatchRequest;
import org.syncany.operations.watch.WatchOperation;
import org.syncany.operations.watch.WatchOperationListener;
import org.syncany.operations.watch.WatchOperationOptions;

import com.google.common.eventbus.Subscribe;

/**
 * The watch operation thread runs a {@link WatchOperation} in a thread. The 
 * underlying thred can be started using the {@link #start()} method, and stopped
 * gracefully using {@link #stop()}. 
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class WatchOperationThread implements WatchOperationListener {
	private static final Logger logger = Logger.getLogger(WatchOperationThread.class.getSimpleName());
	
	private Config config;
	private Thread watchThread;
	private WatchOperation watchOperation;
	private DaemonEventBus eventBus;
	
	private SqlDatabase localDatabase;

	public WatchOperationThread(File localDir, WatchOperationOptions watchOperationOptions) throws ConfigException {
		File configFile = ConfigHelper.findLocalDirInPath(localDir);
		
		if (configFile == null) {
			throw new ConfigException("Config file in folder " + localDir + " not found.");
		}
		
		this.config = ConfigHelper.loadConfig(configFile);
		this.watchOperation = new WatchOperation(config, watchOperationOptions, this);
		
		this.localDatabase = new SqlDatabase(config);
		
		this.eventBus = DaemonEventBus.getInstance();
		this.eventBus.register(this);
	}
	
	public void start() {
		watchThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					logger.log(Level.INFO, "STARTING watch at" + config.getLocalDir());
					
					watchOperation.execute();
					
					logger.log(Level.INFO, "STOPPED watch at " + config.getLocalDir());
				}
				catch (Exception e) {
					logger.log(Level.SEVERE, "ERROR while running watch at " + config.getLocalDir(), e);
				}
			}
		});
		
		watchThread.start();
	}

	public void stop() {
		watchOperation.stop();
		watchThread = null;
	}
	
	@Subscribe
	public void onRequestReceived(WatchRequest watchRequest) {		
		File requestRootFolder = new File(watchRequest.getRoot());
		boolean localDirMatches = requestRootFolder.equals(config.getLocalDir());
		
		if (localDirMatches) {
			logger.log(Level.INFO, "Received " + watchRequest);
			
			if (watchRequest instanceof GetFileTreeRequest) {
				handleGetFileTreeRequest((GetFileTreeRequest) watchRequest);			
			}
			else if (watchRequest instanceof GetFileHistoryRequest) {
				handleGetFileHistoryRequest((GetFileHistoryRequest) watchRequest);			
			}
			else if (watchRequest instanceof GetDatabaseVersionHeadersRequest) {
				handleGetDatabaseVersionHeadersRequest((GetDatabaseVersionHeadersRequest) watchRequest);			
			}
			else {
				eventBus.post(new BadRequestResponse(watchRequest.getId(), "Invalid watch request for root."));
			}
		}		
	}

	private void handleGetFileTreeRequest(GetFileTreeRequest fileTreeRequest) {
		try {
			String prefixLikeQuery = fileTreeRequest.getPrefix() + "%";
			Date date = (fileTreeRequest.getDate() != null) ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ").parse(fileTreeRequest.getDate()) : null;
			
			Map<String, FileVersion> fileTree = localDatabase.getFileTree(prefixLikeQuery, date, false, (FileType[]) null);
			GetFileTreeResponse fileTreeResponse = new GetFileTreeResponse(fileTreeRequest.getId(), fileTreeRequest.getRoot(), fileTreeRequest.getPrefix(), new ArrayList<FileVersion>(fileTree.values()));
			
			eventBus.post(fileTreeResponse);	
		}
		catch (Exception e) {
			eventBus.post(new BadRequestResponse(fileTreeRequest.getId(), "Invalid request: " + e.getMessage()));
		}	
	}
	
	private void handleGetFileHistoryRequest(GetFileHistoryRequest fileHistoryRequest) {
		FileHistoryId fileHistoryId = FileHistoryId.parseFileId(fileHistoryRequest.getFileHistoryId());
		List<FileVersion> fileHistory = localDatabase.getFileHistory(fileHistoryId);
		GetFileHistoryResponse fileHistoryRespose = new GetFileHistoryResponse(fileHistoryRequest.getId(), fileHistoryRequest.getRoot(), fileHistory);
		
		eventBus.post(fileHistoryRespose);
	}
	
	private void handleGetDatabaseVersionHeadersRequest(GetDatabaseVersionHeadersRequest headersRequest) {
		List<DatabaseVersionHeader> databaseVersionHeaders = localDatabase.getLocalDatabaseBranch().getAll(); 
		GetDatabaseVersionHeadersResponse headersResponse = new GetDatabaseVersionHeadersResponse(headersRequest.getId(), headersRequest.getRoot(), databaseVersionHeaders);
		
		eventBus.post(headersResponse);
	}

	@Override
	public void onUploadStart(int fileCount) {
		String root = config.getLocalDir().getAbsolutePath();
		String action = "UPLOAD_START";
		
		eventBus.post(new WatchEventResponse(root, action));
	}

	@Override
	public void onUploadFile(String fileName, int fileNumber) {
		String root = config.getLocalDir().getAbsolutePath();
		String action = "UPLOAD_FILE";
		String subject = fileName;
		
		eventBus.post(new WatchEventResponse(root, action, subject));
	}

	@Override
	public void onUploadEnd() {
		String root = config.getLocalDir().getAbsolutePath();
		String action = "UPLOAD_END";
		
		eventBus.post(new WatchEventResponse(root, action));
	}

	@Override
	public void onIndexStart(int fileCount) {
		String root = config.getLocalDir().getAbsolutePath();
		String action = "INDEX_START";
		
		eventBus.post(new WatchEventResponse(root, action));
	}

	@Override
	public void onIndexFile(String fileName, int fileNumber) {
		String root = config.getLocalDir().getAbsolutePath();
		String action = "INDEX_FILE";
		String subject = fileName;
		
		eventBus.post(new WatchEventResponse(root, action, subject));
	}
	
	@Override
	public void onIndexEnd() {
		String root = config.getLocalDir().getAbsolutePath();
		String action = "INDEX_END";
		
		eventBus.post(new WatchEventResponse(root, action));
	}

	@Override
	public void onDownloadStart(int fileCount) {
		String root = config.getLocalDir().getAbsolutePath();
		String action = "DOWNLOAD_START";
		
		eventBus.post(new WatchEventResponse(root, action));
	}

	@Override
	public void onDownloadFile(String fileName, int fileNumber) {
		String root = config.getLocalDir().getAbsolutePath();
		String action = "DOWNLOAD_FILE";
		String subject = fileName;
		
		eventBus.post(new WatchEventResponse(root, action, subject));
	}
}
