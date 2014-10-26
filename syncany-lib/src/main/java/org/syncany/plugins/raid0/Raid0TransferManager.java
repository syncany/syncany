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
package org.syncany.plugins.raid0;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.syncany.config.Config;
import org.syncany.plugins.Plugins;
import org.syncany.plugins.transfer.AbstractTransferManager;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.plugins.transfer.files.MultichunkRemoteFile;
import org.syncany.plugins.transfer.files.RemoteFile;
import org.syncany.plugins.transfer.files.TempRemoteFile;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */
public class Raid0TransferManager extends AbstractTransferManager {
	private TransferPlugin transferPlugin1;
	private TransferPlugin transferPlugin2;
	private TransferSettings transferSettings1;
	private TransferSettings transferSettings2;
	private TransferManager transferManager1;
	private TransferManager transferManager2;
	
	public Raid0TransferManager(Raid0TransferSettings settings, Config config) {
		super(settings, config);
		
		try {
			this.transferSettings1 = settings.getTransferSettings1();
			this.transferSettings2 = settings.getTransferSettings2();
			this.transferPlugin1 = (TransferPlugin) Plugins.get(transferSettings1.getType());
			this.transferPlugin2 = (TransferPlugin) Plugins.get(transferSettings2.getType());
			this.transferManager1 = transferPlugin1.createTransferManager(transferSettings1, config);
			this.transferManager2 = transferPlugin2.createTransferManager(transferSettings2, config);
		}
		catch (StorageException e) {
			throw new RuntimeException("Cannot create RAID0 transfer manager.", e);
		} 
	}

	@Override
	public void connect() throws StorageException {
		transferManager1.connect();
		transferManager2.connect();
	}

	@Override
	public void disconnect() throws StorageException {
		transferManager1.disconnect();
		transferManager2.disconnect();
	}

	@Override
	public void init(boolean createIfRequired) throws StorageException {
		transferManager1.init(createIfRequired);
		transferManager2.init(createIfRequired);
	}

	@Override
	public void download(RemoteFile remoteFile, File localFile) throws StorageException {
		mapTransferManager(remoteFile).download(remoteFile, localFile);
	}

	@Override
	public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
		mapTransferManager(remoteFile).upload(localFile, remoteFile);
	}

	@Override
	public void move(RemoteFile sourceFile, RemoteFile targetFile) throws StorageException {
		mapTransferManager(sourceFile).move(sourceFile, targetFile);
	}

	@Override
	public boolean delete(RemoteFile remoteFile) throws StorageException {
		return mapTransferManager(remoteFile).delete(remoteFile);
	}

	@Override
	public <T extends RemoteFile> Map<String, T> list(Class<T> remoteFileClass) throws StorageException {
		if (remoteFileClass == MultichunkRemoteFile.class) {
			Map<String, T> remoteFileList1 = transferManager1.list(remoteFileClass);
			Map<String, T> remoteFileList2 = transferManager2.list(remoteFileClass);
			
			Map<String, T> fullRemoteFileList = new HashMap<>();
			
			fullRemoteFileList.putAll(remoteFileList1);
			fullRemoteFileList.putAll(remoteFileList2);
			
			return fullRemoteFileList;
		}
		else {
			return transferManager1.list(remoteFileClass);
		}		
	}

	@Override
	public boolean testTargetExists() throws StorageException {
		return transferManager1.testTargetExists() && transferManager2.testTargetExists();
	}

	@Override
	public boolean testTargetCanWrite() throws StorageException {
		return transferManager1.testTargetCanWrite() && transferManager2.testTargetCanWrite();
	}

	@Override
	public boolean testTargetCanCreate() throws StorageException {
		return transferManager1.testTargetCanCreate() && transferManager2.testTargetCanCreate();
	}

	@Override
	public boolean testRepoFileExists() throws StorageException {
		return transferManager1.testRepoFileExists() && transferManager2.testRepoFileExists();
	}	
	
	private TransferManager mapTransferManager(RemoteFile remoteFile) {
		// THIS IS A PROOF OF CONCEPT; IT DOES NOT YET WORK PROPERLY
		// The temp file needs to reference the target file to be able to determine the target TM1/2
		
		if (remoteFile.getClass().equals(TempRemoteFile.class)) {
			TempRemoteFile tempRemoteFile = (TempRemoteFile) remoteFile;
			return (tempRemoteFile.getName().hashCode() % 2 == 0) ? transferManager1 : transferManager2;
		}
		else if (remoteFile.getClass().equals(MultichunkRemoteFile.class)) {
			MultichunkRemoteFile multiChunkRemoteFile = (MultichunkRemoteFile) remoteFile;
			byte[] multiChunkId = multiChunkRemoteFile.getMultiChunkId();
			
			System.out.println(multiChunkId[multiChunkId.length-1] % 2);
			return (multiChunkId[multiChunkId.length-1] % 2 == 0) ? transferManager1 : transferManager2;
		}
		else {
			return transferManager1;
		}
	}
}
