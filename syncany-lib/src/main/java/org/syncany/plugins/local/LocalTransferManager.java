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
package org.syncany.plugins.local;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.syncany.config.Config;
import org.syncany.plugins.transfer.AbstractTransferManager;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.StorageFileNotFoundException;
import org.syncany.plugins.transfer.StorageMoveException;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.files.ActionRemoteFile;
import org.syncany.plugins.transfer.files.CleanupRemoteFile;
import org.syncany.plugins.transfer.files.DatabaseRemoteFile;
import org.syncany.plugins.transfer.files.MultichunkRemoteFile;
import org.syncany.plugins.transfer.files.RemoteFile;
import org.syncany.plugins.transfer.files.SyncanyRemoteFile;
import org.syncany.plugins.transfer.files.TempRemoteFile;
import org.syncany.plugins.transfer.files.TransactionRemoteFile;

import com.google.common.collect.Maps;

/**
 * Implements a {@link TransferManager} based on a local storage backend for the
 * {@link LocalTransferPlugin}.
 *
 * <p>Using a {@link LocalTransferSettings}, the transfer manager is configured and uses
 * any local folder to store the Syncany repository data. While repo and
 * master file are stored in the given folder, databases and multichunks are stored
 * in special sub-folders:
 *
 * <ul>
 * <li>The <tt>databases</tt> folder keeps all the {@link DatabaseRemoteFile}s</li>
 * <li>The <tt>multichunks</tt> folder keeps the actual data within the {@link MultichunkRemoteFile}s</li>
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

	private Path repoPath;
	private Path multichunksPath;
	private Path databasesPath;
	private Path actionsPath;
	private Path transactionsPath;
	private Path temporaryPath;

	public LocalTransferManager(LocalTransferSettings connection, Config config) {
		super(connection, config);

		this.repoPath = Paths.get(connection.getPath().toURI()); // absolute file to get abs. path!
		this.multichunksPath = repoPath.resolve("multichunks");
		this.databasesPath = repoPath.resolve("databases");
		this.actionsPath = repoPath.resolve("actions");
		this.transactionsPath = repoPath.resolve("transactions");
		this.temporaryPath = repoPath.resolve("temporary");
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

		try {
			if (!testTargetExists() && createIfRequired) {
				if (!Files.exists(Files.createDirectory(repoPath))) {
					throw new StorageException("Cannot create repository directory: " + repoPath);
				}
			}

			if (!Files.exists(Files.createDirectory(multichunksPath))) {
				throw new StorageException("Cannot create multichunk directory: " + multichunksPath);
			}

			if (!Files.exists(Files.createDirectory(databasesPath))) {
				throw new StorageException("Cannot create databases directory: " + databasesPath);
			}

			if (!Files.exists(Files.createDirectory(actionsPath))) {
				throw new StorageException("Cannot create actions directory: " + actionsPath);
			}

			if (!Files.exists(Files.createDirectory(transactionsPath))) {
				throw new StorageException("Cannot create transactions directory: " + transactionsPath);
			}

			if (!Files.exists(Files.createDirectory(temporaryPath))) {
				throw new StorageException("Cannot create temporary directory: " + temporaryPath);
			}
		}
		catch (IOException e) {
			throw new StorageException("Unable to create directories", e);
		}
	}

	@Override
	public void download(RemoteFile remoteFile, File localFile) throws StorageException {
		connect();

		File repoFile = getRemoteFile(remoteFile);

		if (!repoFile.exists()) {
			throw new StorageFileNotFoundException("No such file in local repository: " + repoFile);
		}

		try {
			File tempLocalFile = createTempFile("local-tm-download");
			tempLocalFile.deleteOnExit();

			FileUtils.copyFile(repoFile, tempLocalFile);

			localFile.delete();
			FileUtils.moveFile(tempLocalFile, localFile);
			tempLocalFile.delete();
		}
		catch (IOException ex) {
			throw new StorageException("Unable to copy file " + repoFile + " from local repository to " + localFile, ex);
		}
	}

	@Override
	public void move(RemoteFile sourceFile, RemoteFile targetFile) throws StorageException {
		connect();

		File sourceRemoteFile = getRemoteFile(sourceFile);
		File targetRemoteFile = getRemoteFile(targetFile);

		if (!sourceRemoteFile.exists()) {
			throw new StorageMoveException("Unable to move file " + sourceFile + " because it does not exist.");
		}

		try {
			FileUtils.moveFile(sourceRemoteFile, targetRemoteFile);
		}
		catch (IOException ex) {
			throw new StorageException("Unable to move file " + sourceRemoteFile + " to destination " + targetRemoteFile, ex);
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
			FileUtils.copyFile(localFile, tempRepoFile);
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

		return !repoFile.exists() || repoFile.delete();

	}

	@Override
	public <T extends RemoteFile> Map<String, T> list(Class<T> remoteFileClass) throws StorageException {
		connect();

		Path folder = Paths.get(getRemoteFilePath(remoteFileClass));
		Map<String, T> files = Maps.newHashMap();

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folder)) {
			for (Path path : directoryStream) {
				try {
					T remoteFile = RemoteFile.createRemoteFile(path.getFileName().toString(), remoteFileClass);
					files.put(path.getFileName().toString(), remoteFile);
				}
				catch (StorageException e) {
					logger.log(Level.INFO, "Cannot create instance of " + remoteFileClass.getSimpleName() + " for file " + path
									+ "; maybe invalid file name pattern. Ignoring file.");
				}
			}
		}
		catch (IOException e) {
			logger.log(Level.SEVERE, "Unable to list directory", e);
		}

		return files;
	}

	@Override
	public String getRemoteFilePath(Class<? extends RemoteFile> remoteFile) {
		if (remoteFile.equals(MultichunkRemoteFile.class)) {
			return multichunksPath.toString();
		}
		else if (remoteFile.equals(DatabaseRemoteFile.class) || remoteFile.equals(CleanupRemoteFile.class)) {
			return databasesPath.toString();
		}
		else if (remoteFile.equals(ActionRemoteFile.class)) {
			return actionsPath.toString();
		}
		else if (remoteFile.equals(TransactionRemoteFile.class)) {
			return transactionsPath.toString();
		}
		else if (remoteFile.equals(TempRemoteFile.class)) {
			return temporaryPath.toString();
		}
		else {
			return repoPath.toString();
		}
	}

	private File getRemoteFile(RemoteFile remoteFile) {
		String rootPath = getRemoteFilePath(remoteFile.getClass());
		return Paths.get(rootPath, remoteFile.getName()).toFile();
	}

	public String getAbsoluteParentDirectory(File file) {
		return file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(File.separator));
	}

	@Override
	public boolean testTargetCanWrite() {
		try {
			if (Files.isDirectory(repoPath)) {
				Path tempFile = Files.createTempFile(repoPath, "syncany-write-test", "tmp");
				Files.delete(tempFile);

				logger.log(Level.INFO, "testTargetCanWrite: Can write, test file created/deleted successfully.");
				return true;
			}
			else {
				logger.log(Level.INFO, "testTargetCanWrite: Can NOT write, target does not exist.");
				return false;
			}
		}
		catch (Exception e) {
			logger.log(Level.INFO, "testTargetCanWrite: Can NOT write to target.", e);
			return false;
		}
	}

	@Override
	public boolean testTargetExists() {
		if (Files.exists(repoPath)) {
			logger.log(Level.INFO, "testTargetExists: Target exists.");
			return true;
		}
		else {
			logger.log(Level.INFO, "testTargetExists: Target does NOT exist.");
			return false;
		}
	}

	@Override
	public boolean testRepoFileExists() {
		try {
			File repoFile = getRemoteFile(new SyncanyRemoteFile());

			if (repoFile.exists()) {
				logger.log(Level.INFO, "testRepoFileExists: Repo file exists, list(syncany) returned one result.");
				return true;
			}
			else {
				logger.log(Level.INFO, "testRepoFileExists: Repo file DOES NOT exist.");
				return false;
			}
		}
		catch (Exception e) {
			logger.log(Level.INFO, "testRepoFileExists: Repo file DOES NOT exist. Exception occurred.", e);
			return false;
		}
	}

	@Override
	public boolean testTargetCanCreate() {
		if (Files.isWritable(repoPath.getParent())) {
			logger.log(Level.INFO, "testTargetCanCreate: Can create target.");
			return true;
		}
		else {
			logger.log(Level.INFO, "testTargetCanCreate: Can NOT create target.");
			return false;
		}
	}
}
