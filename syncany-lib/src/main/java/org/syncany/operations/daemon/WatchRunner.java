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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config;
import org.syncany.config.ConfigException;
import org.syncany.config.to.PortTO;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.ObjectId;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.Assembler;
import org.syncany.operations.Downloader;
import org.syncany.operations.daemon.messages.AlreadySyncingResponse;
import org.syncany.operations.daemon.messages.BadRequestResponse;
import org.syncany.operations.daemon.messages.GetDatabaseVersionHeadersFolderRequest;
import org.syncany.operations.daemon.messages.GetDatabaseVersionHeadersFolderResponse;
import org.syncany.operations.daemon.messages.GetFileFolderRequest;
import org.syncany.operations.daemon.messages.GetFileFolderResponse;
import org.syncany.operations.daemon.messages.GetFileFolderResponseInternal;
import org.syncany.operations.daemon.messages.GetFileHistoryFolderRequest;
import org.syncany.operations.daemon.messages.GetFileHistoryFolderResponse;
import org.syncany.operations.daemon.messages.WatchEventFolderResponse;
import org.syncany.operations.daemon.messages.api.FolderRequest;
import org.syncany.operations.daemon.messages.api.FolderRequestHandler;
import org.syncany.operations.daemon.messages.api.Response;
import org.syncany.operations.watch.WatchOperation;
import org.syncany.operations.watch.WatchOperationListener;
import org.syncany.operations.watch.WatchOperationOptions;
import org.syncany.operations.watch.WatchOperationResult;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.util.StringUtil;

import com.google.common.eventbus.Subscribe;

/**
 * The watch operation thread runs a {@link WatchOperation} in a thread. The 
 * underlying thred can be started using the {@link #start()} method, and stopped
 * gracefully using {@link #stop()}. 
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class WatchRunner implements WatchOperationListener {
	private static final Logger logger = Logger.getLogger(WatchRunner.class.getSimpleName());
	
	private Config config;
	private PortTO portTO;
	private Thread watchThread;
	private WatchOperation watchOperation;
	private WatchOperationResult watchOperationResult;
	private LocalEventBus eventBus;
	
	private SqlDatabase localDatabase;

	public WatchRunner(Config config, WatchOperationOptions watchOperationOptions, PortTO portTO) throws ConfigException {
		this.config = config;
		this.portTO = portTO;
		this.watchOperation = new WatchOperation(config, watchOperationOptions, this);
		
		this.localDatabase = new SqlDatabase(config);
		
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
		return (watchOperationResult != null);
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
					eventBus.post(response);
					
					watchOperation.resume();
				}
				else {
					logger.log(Level.WARNING, "FolderRequest discarded : ", folderRequest);
					eventBus.post(new AlreadySyncingResponse(folderRequest.getId(), "FolderRequest discarded."));
				}
			}
			catch (Exception e) {
				eventBus.post(new BadRequestResponse(folderRequest.getId(), "Invalid request."));
			}
			
			/*
			else if (folderRequest instanceof GetFileHistoryFolderRequest) {
				handleGetFileHistoryRequest((GetFileHistoryFolderRequest) folderRequest);			
			}
			else if (folderRequest instanceof GetFileFolderRequest) {
				handleGetFileRequest((GetFileFolderRequest) folderRequest);
			}
			else if (folderRequest instanceof GetDatabaseVersionHeadersFolderRequest) {
				handleGetDatabaseVersionHeadersRequest((GetDatabaseVersionHeadersFolderRequest) folderRequest);			
			}
			else {
				
			}*/
		}		
	}

	private void handleGetFileRequest(GetFileFolderRequest fileRequest) {
		try {
			FileHistoryId fileHistoryId = FileHistoryId.parseFileId(fileRequest.getFileHistoryId());
			long version = fileRequest.getVersion();
			
			FileVersion fileVersion = localDatabase.getFileVersion(fileHistoryId, version);			
			FileContent fileContent = localDatabase.getFileContent(fileVersion.getChecksum(), true);
			Map<ChunkChecksum, MultiChunkId> multiChunks = localDatabase.getMultiChunkIdsByChecksums(fileContent.getChunks());
						
			TransferManager transferManager = config.getTransferPlugin().createTransferManager(config.getConnection());			
			Downloader downloader = new Downloader(config, transferManager);
			Assembler assembler = new Assembler(config, localDatabase);
			
			downloader.downloadAndDecryptMultiChunks(new HashSet<MultiChunkId>(multiChunks.values()));
			
			File tempFile = assembler.assembleToCache(fileVersion);			
			String tempFileToken = StringUtil.toHex(ObjectId.secureRandomBytes(40));
			
			GetFileFolderResponse fileResponse = new GetFileFolderResponse(fileRequest.getId(), fileRequest.getRoot(), tempFileToken);
			GetFileFolderResponseInternal fileResponseInternal = new GetFileFolderResponseInternal(fileResponse, tempFile);

			eventBus.post(fileResponseInternal);
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Cannot reassemble file.", e);
			eventBus.post(new BadRequestResponse(fileRequest.getId(), "Cannot reassemble file."));
		}		
	}
	
	private void handleGetFileHistoryRequest(GetFileHistoryFolderRequest fileHistoryRequest) {
		FileHistoryId fileHistoryId = FileHistoryId.parseFileId(fileHistoryRequest.getFileHistoryId());
		List<FileVersion> fileHistory = localDatabase.getFileHistory(fileHistoryId);
		GetFileHistoryFolderResponse fileHistoryRespose = new GetFileHistoryFolderResponse(fileHistoryRequest.getId(), fileHistoryRequest.getRoot(), fileHistory);
		
		eventBus.post(fileHistoryRespose);
	}
	
	private void handleGetDatabaseVersionHeadersRequest(GetDatabaseVersionHeadersFolderRequest headersRequest) {
		List<DatabaseVersionHeader> databaseVersionHeaders = localDatabase.getNonEmptyDatabaseVersionHeaders(); 
		GetDatabaseVersionHeadersFolderResponse headersResponse = new GetDatabaseVersionHeadersFolderResponse(headersRequest.getId(), headersRequest.getRoot(), databaseVersionHeaders);
		
		eventBus.post(headersResponse);
	}

	@Override
	public void onUploadStart(int fileCount) {
		String root = config.getLocalDir().getAbsolutePath();
		String action = "UPLOAD_START";
		
		eventBus.post(new WatchEventFolderResponse(root, action));
	}

	@Override
	public void onUploadFile(String fileName, int fileNumber) {
		String root = config.getLocalDir().getAbsolutePath();
		String action = "UPLOAD_FILE";
		String subject = fileName;
		
		eventBus.post(new WatchEventFolderResponse(root, action, subject));
	}

	@Override
	public void onUploadEnd() {
		String root = config.getLocalDir().getAbsolutePath();
		String action = "UPLOAD_END";
		
		eventBus.post(new WatchEventFolderResponse(root, action));
	}

	@Override
	public void onIndexStart(int fileCount) {
		String root = config.getLocalDir().getAbsolutePath();
		String action = "INDEX_START";
		
		eventBus.post(new WatchEventFolderResponse(root, action));
	}

	@Override
	public void onIndexFile(String fileName, int fileNumber) {
		String root = config.getLocalDir().getAbsolutePath();
		String action = "INDEX_FILE";
		String subject = fileName;
		
		eventBus.post(new WatchEventFolderResponse(root, action, subject));
	}
	
	@Override
	public void onIndexEnd() {
		String root = config.getLocalDir().getAbsolutePath();
		String action = "INDEX_END";
		
		eventBus.post(new WatchEventFolderResponse(root, action));
	}

	@Override
	public void onDownloadStart(int fileCount) {
		String root = config.getLocalDir().getAbsolutePath();
		String action = "DOWNLOAD_START";
		
		eventBus.post(new WatchEventFolderResponse(root, action));
	}

	@Override
	public void onDownloadFile(String fileName, int fileNumber) {
		String root = config.getLocalDir().getAbsolutePath();
		String action = "DOWNLOAD_FILE";
		String subject = fileName;
		
		eventBus.post(new WatchEventFolderResponse(root, action, subject));
	}
}
