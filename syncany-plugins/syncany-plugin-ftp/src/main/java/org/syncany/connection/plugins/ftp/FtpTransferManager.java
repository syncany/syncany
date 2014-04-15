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
package org.syncany.connection.plugins.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.syncany.connection.plugins.AbstractTransferManager;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.RepoRemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;

/**
 * Implements a {@link TransferManager} based on an FTP storage backend for the
 * {@link FtpPlugin}. 
 * 
 * <p>Using an {@link FtpConnection}, the transfer manager is configured and uses 
 * a well defined FTP folder to store the Syncany repository data. While repo and
 * master file are stored in the given folder, databases and multichunks are stored
 * in special sub-folders:
 * 
 * <ul>
 *   <li>The <tt>databases</tt> folder keeps all the {@link DatabaseRemoteFile}s</li>
 *   <li>The <tt>multichunks</tt> folder keeps the actual data within the {@link MultiChunkRemoteFile}s</li>
 * </ul>
 * 
 * <p>All operations are auto-connected, i.e. a connection is automatically
 * established. Connecting is retried a few times before throwing an exception.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FtpTransferManager extends AbstractTransferManager {
	private static final Logger logger = Logger.getLogger(FtpTransferManager.class.getSimpleName());

	private static final int CONNECT_RETRY_COUNT = 2;
	private static final int TIMEOUT_DEFAULT = 5000;
	private static final int TIMEOUT_CONNECT = 5000;
	private static final int TIMEOUT_DATA = 5000;

	private FTPClient ftp;
	private boolean ftpIsLoggedIn;

	private String repoPath;
	private String multichunkPath;
	private String databasePath;

	public FtpTransferManager(FtpConnection connection) {
		super(connection);

		this.ftp = new FTPClient();
		this.ftpIsLoggedIn = false;
		
		this.repoPath = connection.getPath().startsWith("/") ? connection.getPath() : "/" + connection.getPath();
		this.multichunkPath = repoPath + "/multichunks";
		this.databasePath = repoPath + "/databases";
	}

	@Override
	public FtpConnection getConnection() {
		return (FtpConnection) super.getConnection();
	}

	@Override
	public void connect() throws StorageException {
		for (int i = 0; i < CONNECT_RETRY_COUNT; i++) {
			try {
				if (ftp.isConnected() && ftpIsLoggedIn) {
					logger.log(Level.INFO, "FTP client already connected. Skipping connect().");
					return;
				}

				if (logger.isLoggable(Level.INFO)) {
					logger.log(Level.INFO, "FTP client connecting to {0}:{1} ...", new Object[] { getConnection().getHostname(), getConnection().getPort() });
				}

				ftp.setConnectTimeout(TIMEOUT_CONNECT);
				ftp.setDataTimeout(TIMEOUT_DATA);
				ftp.setDefaultTimeout(TIMEOUT_DEFAULT);

				ftp.connect(getConnection().getHostname(), getConnection().getPort());
				
				if (!ftp.login(getConnection().getUsername(), getConnection().getPassword())) {
					throw new StorageException("Invalid FTP login credentials. Cannot login.");
				}
				
				ftp.enterLocalPassiveMode();
				ftp.setFileType(FTPClient.BINARY_FILE_TYPE); // Important !!!

				ftpIsLoggedIn = true;				
				return; // no loop!
			}
			catch (Exception ex) {
				if (i == CONNECT_RETRY_COUNT - 1) {
					logger.log(Level.WARNING, "FTP client connection failed. Retrying failed.", ex);
					
					ftpIsLoggedIn = false;
					throw new StorageException(ex);
				}
				else {
					logger.log(Level.WARNING, "FTP client connection failed. Retrying " + (i + 1) + "/" + CONNECT_RETRY_COUNT + " ...", ex);
				}
			}
		}
	}

	@Override
	public void disconnect() {
		try {
			ftp.disconnect();
			ftpIsLoggedIn = false;
		}
		catch (Exception ex) {
			// Nothing
		}
	}

	@Override
	public void init(boolean createIfRequired) throws StorageException {
		connect();

		try {
			if (!repoExists() && createIfRequired) {
				ftp.mkd(repoPath);
			}
			
			ftp.mkd(multichunkPath);
			ftp.mkd(databasePath);
		}
		catch (IOException e) {
			forceFtpDisconnect();
			throw new StorageException("Cannot create directory " + multichunkPath + ", or " + databasePath, e);
		}
	}

	@Override
	public void download(RemoteFile remoteFile, File localFile) throws StorageException {
		connect();

		String remotePath = getRemoteFile(remoteFile);

		try {
			// Download file
			File tempFile = createTempFile(localFile.getName());
			OutputStream tempFOS = new FileOutputStream(tempFile);

			if (logger.isLoggable(Level.INFO)) {
				logger.log(Level.INFO, "FTP: Downloading {0} to temp file {1}", new Object[] { remotePath, tempFile });
			}

			ftp.retrieveFile(remotePath, tempFOS);

			tempFOS.close();

			// Move file
			if (logger.isLoggable(Level.INFO)) {
				logger.log(Level.INFO, "FTP: Renaming temp file {0} to file {1}", new Object[] { tempFile, localFile });
			}

			localFile.delete();
			FileUtils.moveFile(tempFile, localFile);
			tempFile.delete();
		}
		catch (IOException ex) {
			forceFtpDisconnect();

			logger.log(Level.SEVERE, "Error while downloading file " + remoteFile.getName(), ex);
			throw new StorageException(ex);
		}
	}

	@Override
	public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
		connect();

		String remotePath = getRemoteFile(remoteFile);
		String tempRemotePath = repoPath + "/temp-" + remoteFile.getName();

		try {
			// Upload to temp file
			InputStream fileFIS = new FileInputStream(localFile);

			if (logger.isLoggable(Level.INFO)) {
				logger.log(Level.INFO, "FTP: Uploading {0} to temp file {1}", new Object[] { localFile, tempRemotePath });
			}

			ftp.setFileType(FTPClient.BINARY_FILE_TYPE); // Important !!!

			if (!ftp.storeFile(tempRemotePath, fileFIS)) {
				throw new IOException("Error uploading file " + remoteFile.getName());
			}

			fileFIS.close();

			// Move
			if (logger.isLoggable(Level.INFO)) {
				logger.log(Level.INFO, "FTP: Renaming temp file {0} to file {1}", new Object[] { tempRemotePath, remotePath });
			}

			ftp.rename(tempRemotePath, remotePath);
		}
		catch (IOException ex) {
			forceFtpDisconnect();

			logger.log(Level.SEVERE, "Could not upload file " + localFile + " to " + remoteFile.getName(), ex);
			throw new StorageException(ex);
		}
	}

	@Override
	public boolean delete(RemoteFile remoteFile) throws StorageException {
		connect();

		String remotePath = getRemoteFile(remoteFile);

		try {
			logger.log(Level.INFO, "FTP: Deleting file " + remotePath + " ...");
			
			// Try deleting; returns 'false' if file does not exist
			if (ftp.deleteFile(remotePath)) {
				return true;
			}
			
			// Double check if above command returned 'false' (if non-existent file)
			String[] fileList = ftp.listNames(remotePath);			
			boolean remotePathDeleted = fileList != null && fileList.length == 0;
			
			return remotePathDeleted;
		}
		catch (IOException ex) {
			forceFtpDisconnect();
			
			logger.log(Level.SEVERE, "Could not delete file " + remoteFile.getName(), ex);
			throw new StorageException(ex);
		}
	}

	@Override
	public <T extends RemoteFile> Map<String, T> list(Class<T> remoteFileClass) throws StorageException {
		connect();

		try {
			// List folder
			String remoteFilePath = getRemoteFilePath(remoteFileClass);
			FTPFile[] ftpFiles = ftp.listFiles(remoteFilePath + "/");

			// Create RemoteFile objects
			Map<String, T> remoteFiles = new HashMap<String, T>();

			for (FTPFile file : ftpFiles) {
				try {
					T remoteFile = RemoteFile.createRemoteFile(file.getName(), remoteFileClass);
					remoteFiles.put(file.getName(), remoteFile);
				}
				catch (Exception e) {
					logger.log(Level.INFO, "Cannot create instance of " + remoteFileClass.getSimpleName() + " for file " + file + "; maybe invalid file name pattern. Ignoring file.");
				}
			}

			return remoteFiles;
		}
		catch (IOException ex) {
			forceFtpDisconnect();

			logger.log(Level.SEVERE, "Unable to list FTP directory.", ex);
			throw new StorageException(ex);
		}
	}

	private void forceFtpDisconnect() {
		try {
			ftp.disconnect();
		}
		catch (IOException e) {
			// Nothing
		}
	}

	private String getRemoteFile(RemoteFile remoteFile) {
		return getRemoteFilePath(remoteFile.getClass()) + "/" + remoteFile.getName();
	}

	private String getRemoteFilePath(Class<? extends RemoteFile> remoteFile) {
		if (remoteFile.equals(MultiChunkRemoteFile.class)) {
			return multichunkPath;
		}
		else if (remoteFile.equals(DatabaseRemoteFile.class)) {
			return databasePath;
		}
		else {
			return repoPath;
		}
	}
	
	@Override
	public boolean repoHasWriteAccess() throws StorageException {
		try {
			boolean createSuccessful = ftp.makeDirectory(repoPath);
			ftp.removeDirectory(repoPath);
			
			return createSuccessful;
		}
		catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean repoExists() throws StorageException {
		try {
			return ftp.changeWorkingDirectory(repoPath);
		}
		catch (Exception e) {
			return false;
		}
	}
	
	@Override
	public boolean repoIsValid() throws StorageException {				
		try {
			String[] listRepoFile = ftp.listNames(new RepoRemoteFile().getName());
			return (listRepoFile != null) ? listRepoFile.length == 0 : true;
		}
		catch (IOException e) {
			throw new StorageException(e);
		}		
	}
}
