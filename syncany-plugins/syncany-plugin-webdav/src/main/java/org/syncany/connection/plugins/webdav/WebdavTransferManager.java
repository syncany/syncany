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
package org.syncany.connection.plugins.webdav;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.syncany.connection.plugins.AbstractTransferManager;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.util.FileUtil;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.github.sardine.impl.SardineException;
import com.github.sardine.impl.SardineImpl;

public class WebdavTransferManager extends AbstractTransferManager {
	private static final String APPLICATION_CONTENT_TYPE = "application/x-syncany";
	private static final int HTTP_NOT_FOUND = 404;
	private static final Logger logger = Logger.getLogger(WebdavTransferManager.class.getSimpleName());

	private Sardine sardine;

	private String repoPath;
	private String multichunkPath;
	private String databasePath;

	public WebdavTransferManager(WebdavConnection connection) {
		super(connection);

		this.repoPath = connection.getUrl().replaceAll("/$", "") + "/";
		this.multichunkPath = repoPath + "multichunks/";
		this.databasePath = repoPath + "databases/";
	}

	@Override
	public WebdavConnection getConnection() {
		return (WebdavConnection) super.getConnection();
	}

	@Override
	public void connect() throws StorageException {
		if (sardine == null) {
			logger.log(Level.INFO, "WebDAV: Connect called. Creating Sardine ...");
			
			if (getConnection().isSecure()) {
				final SSLSocketFactory sslSocketFactory = getConnection().getSslSocketFactory();

				sardine = new SardineImpl() {
					@Override
					protected SSLSocketFactory createDefaultSecureSocketFactory() {
						return sslSocketFactory;
					}
				};

				sardine.setCredentials(getConnection().getUsername(), getConnection().getPassword());
			}
			else {
				sardine = SardineFactory.begin(getConnection().getUsername(), getConnection().getPassword());
			}
		}
	}

	@Override
	public void disconnect() {
		sardine = null;
	}

	@Override
	public void init(boolean createIfRequired) throws StorageException {
		connect();

		try {
			if (!repoExists() && createIfRequired) {
				logger.log(Level.INFO, "WebDAV: Init called; creating repo directories ... ");
				
				sardine.createDirectory(repoPath);
				sardine.createDirectory(multichunkPath);
				sardine.createDirectory(databasePath);
			}
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Cannot initialize WebDAV folder.", e);
			throw new StorageException(e);
		}
	}

	@Override
	public void download(RemoteFile remoteFile, File localFile) throws StorageException {
		connect();
		String remoteURL = getRemoteFileUrl(remoteFile);

		try {
			logger.log(Level.INFO, "WebDAV: Downloading " + remoteURL + " to temp file " + localFile + " ...");
			
			InputStream webdavFileInputStream = sardine.get(remoteURL);
			FileOutputStream localFileOutputStream = new FileOutputStream(localFile);

			FileUtil.appendToOutputStream(webdavFileInputStream, localFileOutputStream);

			localFileOutputStream.close();
			webdavFileInputStream.close();
		}
		catch (IOException ex) {
			logger.log(Level.SEVERE, "Error while downloading file from WebDAV: " + remoteURL, ex);
			throw new StorageException(ex);
		}
	}

	@Override
	public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
		connect();
		String remoteURL = getRemoteFileUrl(remoteFile);

		try {
			logger.log(Level.INFO, "WebDAV: Uploading local file " + localFile + " to " + remoteURL + " ...");
			InputStream localFileInputStream = new FileInputStream(localFile);

			sardine.put(remoteURL, localFileInputStream, APPLICATION_CONTENT_TYPE);
			localFileInputStream.close();
		}
		catch (Exception ex) {
			logger.log(Level.SEVERE, "Error while uploading file to WebDAV: " + remoteURL, ex);
			throw new StorageException(ex);
		}
	}

	@Override
	public <T extends RemoteFile> Map<String, T> list(Class<T> remoteFileClass) throws StorageException {
		connect();

		try {
			// List folder
			String remoteFileUrl = getRemoteFilePath(remoteFileClass);			
			logger.log(Level.INFO, "WebDAV: Listing objects in " + remoteFileUrl + " ...");
			
			List<DavResource> resources = sardine.list(remoteFileUrl);

			// Create RemoteFile objects
			String rootPath = repoPath.substring(0, repoPath.length() - new URI(repoPath).getRawPath().length());
			Map<String, T> remoteFiles = new HashMap<String, T>();

			for (DavResource res : resources) {
				// WebDAV returns the parent resource itself; ignore it
				String fullResourceUrl = rootPath + res.getPath().replaceAll("/$", "") + "/";				
				boolean isParentResource = remoteFileUrl.equals(fullResourceUrl.toString());

				if (!isParentResource) {
					try {
						T remoteFile = RemoteFile.createRemoteFile(res.getName(), remoteFileClass);
						remoteFiles.put(res.getName(), remoteFile);

						logger.log(Level.FINE, "WebDAV: Matching WebDAV resource: " + res);
					}
					catch (Exception e) {
						logger.log(Level.FINEST, "Cannot create instance of " + remoteFileClass.getSimpleName() + " for object " + res.getName()
								+ "; maybe invalid file name pattern. Ignoring file.");
					}
				}
			}

			return remoteFiles;
		}
		catch (Exception ex) {
			logger.log(Level.SEVERE, "WebDAV: Unable to list WebDAV directory.", ex);
			throw new StorageException(ex);
		}
	}

	@Override
	public boolean delete(RemoteFile remoteFile) throws StorageException {
		connect();
		String remoteURL = getRemoteFileUrl(remoteFile);

		try {
			logger.log(Level.FINE, "WebDAV: Deleting " + remoteURL);
			sardine.delete(remoteURL);
			
			return true;
		}
		catch (SardineException e) {
			if (e.getStatusCode() == HTTP_NOT_FOUND) {
				return true;
			}
			else {
				return false;
			}
		}
		catch (IOException ex) {
			logger.log(Level.SEVERE, "Error while deleting file from WebDAV: " + remoteURL, ex);
			throw new StorageException(ex);
		}
	}

	private String getRemoteFileUrl(RemoteFile remoteFile) {
		return getRemoteFilePath(remoteFile.getClass()) + remoteFile.getName();
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
		// TODO not tested
		try {
			sardine.createDirectory(repoPath);
			sardine.delete(repoPath);
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}

	/**
	 * Checks if the repo exists at the repo URL.
	 * 
	 * <p><b>Note:</b> This uses list() instead of exists() because Sardine implements
	 * the exists() method with a HTTP HEAD only. Some WebDAV servers respond with "Forbidden" 
	 * if for directories.
	 */
	@Override
	public boolean repoExists() throws StorageException {
		try {
			sardine.list(repoPath);
			return true;
		}
		catch (SardineException e) {
			return false;
		}
		catch (IOException e) {
			throw new StorageException(e);
		}
	}

	@Override
	public boolean repoIsValid() throws StorageException {
		// TODO not tested
		try {
			return sardine.list(repoPath).size() != 0;
		}
		catch (IOException e) {
			throw new StorageException(e);
		}
	}
}
