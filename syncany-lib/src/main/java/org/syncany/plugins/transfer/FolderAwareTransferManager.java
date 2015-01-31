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
package org.syncany.plugins.transfer;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;
import org.syncany.plugins.transfer.files.RemoteFile;
import org.syncany.util.StringUtil;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public class FolderAwareTransferManager implements TransferManager {
	private static final Logger logger = Logger.getLogger(FolderAwareTransferManager.class.getSimpleName());
	private static final char FOLDER_SEPERATOR = '/';

	private final FolderizableTransferManager underlyingTransferManager;

	public FolderAwareTransferManager(FolderizableTransferManager underlyingTransferManager) {
		this.underlyingTransferManager = underlyingTransferManager;
	}

	@Override
	public void connect() throws StorageException {
		underlyingTransferManager.connect();
	}

	@Override
	public void disconnect() throws StorageException {
		underlyingTransferManager.disconnect();
	}

	@Override
	public void init(final boolean createIfRequired) throws StorageException {
		underlyingTransferManager.init(createIfRequired);
	}

	@Override
	public void download(final RemoteFile remoteFile, final File localFile) throws StorageException {
		underlyingTransferManager.download(createPathAwareRemoteFile(remoteFile), localFile);
	}

	@Override
	public void move(final RemoteFile sourceFile, final RemoteFile targetFile) throws StorageException {
		final RemoteFile pathAwareTargetFile = createPathAwareRemoteFile(targetFile);

		if (!underlyingTransferManager.createPathIfRequired(pathAwareTargetFile)) {
			throw new StorageException("Unable to create path for " + pathAwareTargetFile);
		}

		underlyingTransferManager.move(createPathAwareRemoteFile(sourceFile), pathAwareTargetFile);
	}

	@Override
	public void upload(final File localFile, final RemoteFile remoteFile) throws StorageException {
		final RemoteFile pathAwareRemoteFile = createPathAwareRemoteFile(remoteFile);

		if (!underlyingTransferManager.createPathIfRequired(pathAwareRemoteFile)) {
			throw new StorageException("Unable to create path for " + pathAwareRemoteFile);
		}

		underlyingTransferManager.upload(localFile, pathAwareRemoteFile);
	}

	@Override
	public boolean delete(final RemoteFile remoteFile) throws StorageException {
		return underlyingTransferManager.delete(createPathAwareRemoteFile(remoteFile));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends RemoteFile> Map<String, T> list(final Class<T> remoteFileClass) throws StorageException {
		return underlyingTransferManager.list(remoteFileClass);
	}

	@Override
	public StorageTestResult test(boolean testCreateTarget) {
		return underlyingTransferManager.test(testCreateTarget);
	}

	@Override
	public boolean testTargetExists() throws StorageException {
		return underlyingTransferManager.testTargetExists();
	}

	@Override
	public boolean testTargetCanWrite() throws StorageException {
		return underlyingTransferManager.testTargetCanWrite();
	}

	@Override
	public boolean testTargetCanCreate() throws StorageException {
		return underlyingTransferManager.testTargetCanCreate();
	}

	@Override
	public boolean testRepoFileExists() throws StorageException {
		return underlyingTransferManager.testRepoFileExists();
	}

	private boolean isFolderizable(Class<? extends RemoteFile> remoteFileClass) {
		return underlyingTransferManager.getFolderizableFiles().contains(remoteFileClass);
	}

	private RemoteFile createPathAwareRemoteFile(RemoteFile remoteFile) throws StorageException {
		if (!isFolderizable(remoteFile.getClass())) {
			return remoteFile;
		}

		// we need to use the hash value of a file's name because some files aren't folderizable by default
		String fileId = StringUtil.toHex(DigestUtils.sha256(remoteFile.getSimpleName()));
		StringBuilder path = new StringBuilder();

		for (int i = 0; i < underlyingTransferManager.getSubfolderDepth(); i++) {
			path.append(fileId.substring(i * underlyingTransferManager.getBytesPerFolder(), (i + 1) * underlyingTransferManager.getBytesPerFolder()));
			path.append(FOLDER_SEPERATOR);
		}

		return RemoteFile.createRemoteFileWithPath(remoteFile.getSimpleName(), path.toString(), remoteFile.getClass());
	}

}
