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
package org.syncany.connection.plugins.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.syncany.connection.plugins.AbstractTransferManager;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.RepoRemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;

/**
 * Implements a {@link TransferManager} based on a local storage backend for the
 * {@link LocalPlugin}. 
 * 
 * <p>Using a {@link LocalConnection}, the transfer manager is configured and uses 
 * any local folder to store the Syncany repository data. While repo and
 * master file are stored in the given folder, databases and multichunks are stored
 * in special sub-folders:
 * 
 * <ul>
 *   <li>The <tt>databases</tt> folder keeps all the {@link DatabaseRemoteFile}s</li>
 *   <li>The <tt>multichunks</tt> folder keeps the actual data within the {@link MultiChunkRemoteFile}s</li>
 * </ul>
 * 
 * <p>This plugin can be used for testing or to point to a repository
 * on a mounted remote device or network storage such as an NFS or a 
 * Samba/NetBIOS share.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class LocalTransferManager extends AbstractTransferManager {
	private static final Logger logger = Logger.getLogger(LocalTransferManager.class.getSimpleName());

	private File repoPath;
	private File multichunksPath;
	private File databasePath;

	public LocalTransferManager(LocalConnection connection) {
		super(connection);

		this.repoPath = connection.getRepositoryPath().getAbsoluteFile(); // absolute file to get abs. path!
		this.multichunksPath = new File(connection.getRepositoryPath().getAbsolutePath(), "multichunks");
		this.databasePath = new File(connection.getRepositoryPath().getAbsolutePath(), "databases");
	}

	@Override
	public void connect() throws StorageException {
		if (repoPath == null) {
			throw new StorageException("Repository folder '" + repoPath + "' does not exist or is not writable.");
		}
	}

	@Override
	public void disconnect() throws StorageException {
		// Nothing.
	}

	@Override
	public void init(boolean createIfRequired) throws StorageException {
		connect();

		if (!repoExists() && createIfRequired) {
			if (!repoPath.mkdir()) {
				throw new StorageException("Cannot create repository directory: " + repoPath);
			}
		}

		if (!multichunksPath.mkdir()) {
			throw new StorageException("Cannot create multichunk directory: " + multichunksPath);
		}

		if (!databasePath.mkdir()) {
			throw new StorageException("Cannot create database directory: " + databasePath);
		}
	}

	@Override
	public void download(RemoteFile remoteFile, File localFile) throws StorageException {
		connect();

		File repoFile = getRemoteFile(remoteFile);

		if (!repoFile.exists()) {
			throw new StorageException("No such file in local repository: " + repoFile);
		}

		try {
			File tempLocalFile = createTempFile("local-tm-download");
			tempLocalFile.deleteOnExit();

			copyLocalFile(repoFile, tempLocalFile);

			localFile.delete();
			FileUtils.moveFile(tempLocalFile, localFile);
			tempLocalFile.delete();
		}
		catch (IOException ex) {
			throw new StorageException("Unable to copy file " + repoFile + " from local repository to " + localFile, ex);
		}
	}

	@Override
	public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
		connect();

		File repoFile = getRemoteFile(remoteFile);
		File tempRepoFile = new File(getAbsoluteParentDirectory(repoFile) + File.separator + ".temp-" + repoFile.getName());

		// Do not overwrite files with same size!
		if (repoFile.exists() && repoFile.length() == localFile.length()) {
			return;
		}

		// No such local file
		if (!localFile.exists()) {
			throw new StorageException("No such file on local disk: " + localFile);
		}

		try {
			copyLocalFile(localFile, tempRepoFile);
			FileUtils.moveFile(tempRepoFile, repoFile);
		}
		catch (IOException ex) {
			throw new StorageException("Unable to copy file " + localFile + " to local repository " + repoFile, ex);
		}
	}

	@Override
	public boolean delete(RemoteFile remoteFile) throws StorageException {
		connect();

		File repoFile = getRemoteFile(remoteFile);

		if (!repoFile.exists()) {
			return true;
		}

		return repoFile.delete();
	}

	@Override
	public <T extends RemoteFile> Map<String, T> list(Class<T> remoteFileClass) throws StorageException {
		connect();

		// List folder
		File remoteFilePath = getRemoteFilePath(remoteFileClass);
		File[] files = remoteFilePath.listFiles();

		if (files == null) {
			throw new StorageException("Unable to read local respository " + repoPath);
		}

		// Create RemoteFile objects
		Map<String, T> remoteFiles = new HashMap<String, T>();

		for (File file : files) {
			try {
				T remoteFile = RemoteFile.createRemoteFile(file.getName(), remoteFileClass);
				remoteFiles.put(file.getName(), remoteFile);
			}
			catch (Exception e) {
				logger.log(Level.INFO, "Cannot create instance of " + remoteFileClass.getSimpleName() + " for file " + file
						+ "; maybe invalid file name pattern. Ignoring file.");
			}
		}

		return remoteFiles;
	}

	private File getRemoteFile(RemoteFile remoteFile) {
		return new File(getRemoteFilePath(remoteFile.getClass()) + File.separator + remoteFile.getName());
	}

	private File getRemoteFilePath(Class<? extends RemoteFile> remoteFile) {
		if (remoteFile.equals(MultiChunkRemoteFile.class)) {
			return multichunksPath;
		}
		else if (remoteFile.equals(DatabaseRemoteFile.class)) {
			return databasePath;
		}
		else {
			return repoPath;
		}
	}

	public void copyLocalFile(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		byte[] buf = new byte[4096];

		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}

		in.close();
		out.close();
	}

	public String getAbsoluteParentDirectory(File file) {
		return file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(File.separator));
	}

	@Override
	public boolean repoHasWriteAccess() {		
		return repoPath.getParentFile().canWrite();
	}

	@Override
	public boolean repoExists() throws StorageException {
		return repoPath.exists();
	}

	@Override
	public boolean repoIsValid() throws StorageException {
		final RepoRemoteFile repoRemoteFile = new RepoRemoteFile();
		
		String[] listRepoFile = repoPath.list(new FilenameFilter() {			
			@Override
			public boolean accept(File dir, String name) {
				return name.equals(repoRemoteFile.getName());
			}
		});
				
		if (listRepoFile != null) {
			return listRepoFile.length == 1; // If exactly one file!
		}
		else {
			return false;
		}
	}
}
