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
package org.syncany.plugins.dummy;

import java.io.File;
import java.util.Map;

import org.syncany.config.Config;
import org.syncany.plugins.transfer.AbstractTransferManager;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.files.RemoteFile;

/**
 * @author Christian Roth (christian.roth@port17.de)
 */

public class DummyTransferManager extends AbstractTransferManager {

	public DummyTransferManager(DummyTransferSettings settings, Config config) {
		super(settings, config);
	}

	@Override
	public void connect() throws StorageException {

	}

	@Override
	public void disconnect() throws StorageException {

	}

	@Override
	public void init(boolean createIfRequired) throws StorageException {

	}

	@Override
	public void download(RemoteFile remoteFile, File localFile) throws StorageException {

	}

	@Override
	public void upload(File localFile, RemoteFile remoteFile) throws StorageException {

	}

	@Override
	public void move(RemoteFile sourceFile, RemoteFile targetFile) throws StorageException {

	}

	@Override
	public boolean delete(RemoteFile remoteFile) throws StorageException {
		return false;
	}

	@Override
	public <T extends RemoteFile> Map<String, T> list(Class<T> remoteFileClass) throws StorageException {
		return null;
	}

	@Override
	public boolean testTargetExists() throws StorageException {
		return false;
	}

	@Override
	public boolean testTargetCanWrite() throws StorageException {
		return false;
	}

	@Override
	public boolean testTargetCanCreate() throws StorageException {
		return false;
	}

	@Override
	public boolean testRepoFileExists() throws StorageException {
		return false;
	}

	@Override
	public String getRemoteFilePath(Class<? extends RemoteFile> remoteFileClass) {
		return "";
	}
}
