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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.config.ConfigException;
import org.syncany.config.ConfigHelper;
import org.syncany.database.FileVersion;
import org.syncany.operations.daemon.messages.BadRequestResponse;
import org.syncany.operations.daemon.messages.FileFrameResponse;
import org.syncany.operations.daemon.messages.GetFileRequest;
import org.syncany.operations.daemon.messages.GetFileResponse;
import org.syncany.operations.daemon.messages.GetFileTreeRequest;
import org.syncany.operations.daemon.messages.GetFileTreeResponse;
import org.syncany.operations.daemon.messages.WatchEventResponse;
import org.syncany.operations.daemon.messages.WatchRequest;
import org.syncany.operations.watch.WatchOperation;
import org.syncany.operations.watch.WatchOperationListener;
import org.syncany.operations.watch.WatchOperationOptions;
import org.syncany.util.FileUtil;

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
	private static final int MAX_FRAME_LENGTH = 512*1024;
	
	private Config config;
	private Thread watchThread;
	private WatchOperation watchOperation;
	private DaemonEventBus eventBus;

	public WatchOperationThread(File localDir, WatchOperationOptions watchOperationOptions) throws ConfigException {
		File configFile = ConfigHelper.findLocalDirInPath(localDir);
		
		if (configFile == null) {
			throw new ConfigException("Config file in folder " + localDir + " not found.");
		}
		
		this.config = ConfigHelper.loadConfig(configFile);
		this.watchOperation = new WatchOperation(config, watchOperationOptions, this);
		
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
				handleFileTreeRequest((GetFileTreeRequest) watchRequest);			
			}
			else if (watchRequest instanceof GetFileRequest) {
				handleGetRequest((GetFileRequest) watchRequest);			
			}
			else {
				eventBus.post(new BadRequestResponse(watchRequest.getId(), "Invalid watch request for root."));
			}
		}		
	}

	private void handleGetRequest(GetFileRequest getRequest) {
		String requestedFileStr = getRequest.getFile();		
		File requestedFile = FileUtil.getCanonicalFile(new File(config.getLocalDir(), requestedFileStr));
		String mimeType = findMimeType(requestedFile);
		
		long length = requestedFile.length();
		int frames = (int) Math.ceil((double) length / MAX_FRAME_LENGTH);
		
		try (InputStream fileInputStream = new FileInputStream(requestedFile)) {
			eventBus.post(new GetFileResponse(getRequest.getId(), requestedFile.getName(), length, frames, mimeType));
			
			int read = -1;
			byte[] buffer = new byte[MAX_FRAME_LENGTH];
			int frameNumber = 0;
			
			while (-1 != (read = fileInputStream.read(buffer))) {
				FileFrameResponse fileDataResponse = new FileFrameResponse(getRequest.getId(), frameNumber++, ByteBuffer.wrap(buffer, 0, read));
				eventBus.post(fileDataResponse);
			}
		}
		catch (Exception e) {
			eventBus.post(new BadRequestResponse(getRequest.getId(), "Error while reading file data."));
		}
	}
	
	private String findMimeType(File file) {
		try {
			return Files.probeContentType(Paths.get(file.getAbsolutePath()));
		}
		catch (IOException e) {
			return "application/octet-stream";
		}
	}

	private void handleFileTreeRequest(GetFileTreeRequest fileTreeRequest) {
		Map<String, FileVersion> fileTree = watchOperation.getLocalDatabase().getCurrentFileTree(fileTreeRequest.getPrefix());
		GetFileTreeResponse fileTreeResponse = new GetFileTreeResponse(fileTreeRequest.getId(), fileTreeRequest.getRoot(), new ArrayList<FileVersion>(fileTree.values()));
		
		eventBus.post(fileTreeResponse);	
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
