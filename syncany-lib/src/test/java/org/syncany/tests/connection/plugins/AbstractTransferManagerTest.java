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
package org.syncany.tests.connection.plugins;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.connection.plugins.MasterRemoteFile;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.RepoRemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.util.StringUtil;

public abstract class AbstractTransferManagerTest {
	private File tempLocalSourceDir;
	private File localRootDir;

	public abstract Map<String, String> createPluginSettings();
	public abstract String getPluginId();
	
	@Before
	public void setUp() throws Exception {	
		tempLocalSourceDir = new File(localRootDir + "/local");
		tempLocalSourceDir.mkdir();
	}
	
	@After
	public void tearDown() {
		TestFileUtil.deleteDirectory(tempLocalSourceDir);
		TestFileUtil.deleteDirectory(localRootDir);
	}

	@Test
	public void testLoadPluginAndCreateTransferManager() throws StorageException {
		loadPluginAndCreateTransferManager();
	}

	@Test
	public void testLocalPluginInfo() {
		String pluginId = getPluginId();
		Plugin plugin = Plugins.get(pluginId);
		
		assertNotNull("PluginInfo should not be null.", plugin);
		assertEquals("Plugin ID should different.", pluginId, plugin.getId());
		assertNotNull("Plugin version should not be null.", plugin.getVersion());
		assertNotNull("Plugin name should not be null.", plugin.getName());
	}

	@Test(expected = StorageException.class)
	public void testConnectWithInvalidSettings() throws StorageException {
		Plugin plugin = Plugins.get(getPluginId());

		Map<String, String> invalidEmptyPluginSettings = new HashMap<String, String>();

		Connection connection = plugin.createConnection();
		connection.init(invalidEmptyPluginSettings);

		TransferManager transferManager = connection.createTransferManager();

		// This should cause a Storage exception, because the path does not exist
		transferManager.connect();
	}

	@Test
	public void testUploadListDownloadAndDelete() throws Exception {
		// Setup
		File tempFromDir = TestFileUtil.createTempDirectoryInSystemTemp();
		File tempToDir = TestFileUtil.createTempDirectoryInSystemTemp();
		
		// Create connection, upload, list, download
		TransferManager transferManager = loadPluginAndCreateTransferManager();
		
		transferManager.init(true);		
		transferManager.connect();

		// Clear up previous test (if test location is reused)
		cleanTestLocation(transferManager);

		// Run!
		uploadDownloadListDelete(transferManager, tempFromDir, tempToDir, RepoRemoteFile.class, new RepoRemoteFile[] { 
			new RepoRemoteFile()
		});
		
		uploadDownloadListDelete(transferManager, tempFromDir, tempToDir, MasterRemoteFile.class, new MasterRemoteFile[] {  
			new MasterRemoteFile()
		});
		
		uploadDownloadListDelete(transferManager, tempFromDir, tempToDir, DatabaseRemoteFile.class, new DatabaseRemoteFile[] { 
			new DatabaseRemoteFile("db-A-0001"), 
			new DatabaseRemoteFile("db-B-0002") 
		});

		uploadDownloadListDelete(transferManager, tempFromDir, tempToDir, MultiChunkRemoteFile.class, new MultiChunkRemoteFile[] { 
			new MultiChunkRemoteFile("multichunk-84f7e2b31440aaef9b73de3cadcf4e449aeb55a1"), 
			new MultiChunkRemoteFile("multichunk-beefbeefbeefbeefbeefbeefbeefbeefbeefbeef"), 
			new MultiChunkRemoteFile("multichunk-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa") 
		});

		// Clear up previous test (if test location is reused)
		cleanTestLocation(transferManager);

		// Clean local location
		TestFileUtil.deleteDirectory(tempFromDir);
		TestFileUtil.deleteDirectory(tempToDir);
	}

	private <T extends RemoteFile> void uploadDownloadListDelete(TransferManager transferManager, File tempFromDir, File tempToDir, Class<T> remoteFileClass, T[] remoteFiles) throws Exception {
		for (RemoteFile remoteFile : remoteFiles) {
			File originalLocalFile = new File(tempFromDir, remoteFile.getName());
			File downloadedLocalFile = new File(tempToDir, remoteFile.getName());

			TestFileUtil.createNonRandomFile(originalLocalFile, 5*1024);
			
			transferManager.upload(originalLocalFile, remoteFile);
			transferManager.download(remoteFile, downloadedLocalFile);
			
			String checksumOriginalFile = StringUtil.toHex(TestFileUtil.createChecksum(originalLocalFile));
			String checksumDownloadedFile = StringUtil.toHex(TestFileUtil.createChecksum(downloadedLocalFile));

			assertEquals("Uploaded file differs from original file, for file " + originalLocalFile, checksumOriginalFile, checksumDownloadedFile);
		}

		Map<String, T> listLocalFilesAfterUpload = transferManager.list(remoteFileClass);
		assertEquals(remoteFiles.length, listLocalFilesAfterUpload.size());
		
		for (RemoteFile remoteFile : remoteFiles) {
			transferManager.delete(remoteFile);
		}
		
		Map<String, T> listLocalFileAfterDelete = transferManager.list(remoteFileClass);
		assertEquals(0, listLocalFileAfterDelete.size());		
	}
	
	private void cleanTestLocation(TransferManager transferManager) throws StorageException {
		Map<String, RemoteFile> normalFiles = transferManager.list(RemoteFile.class);
		Map<String, DatabaseRemoteFile> databaseFiles = transferManager.list(DatabaseRemoteFile.class);
		Map<String, MultiChunkRemoteFile> multiChunkFiles = transferManager.list(MultiChunkRemoteFile.class);
		
		for (RemoteFile remoteFile : normalFiles.values()) {
			transferManager.delete(remoteFile);
		}
		
		for (RemoteFile remoteFile : databaseFiles.values()) {
			transferManager.delete(remoteFile);
		}
		
		for (RemoteFile remoteFile : multiChunkFiles.values()) {
			transferManager.delete(remoteFile);
		}
	}
	
	@Test
	public void testDeleteNonExistentFile() throws StorageException {
		TransferManager transferManager = loadPluginAndCreateTransferManager();
		transferManager.connect();	
		
		boolean deleteSuccess = transferManager.delete(new MultiChunkRemoteFile("multichunk-dddddddddddddddddddddddddddddddddddddddd")); // does not exist
		assertTrue(deleteSuccess);
	}	

	private TransferManager loadPluginAndCreateTransferManager() throws StorageException {
		Plugin pluginInfo = Plugins.get(getPluginId());

		Connection connection = pluginInfo.createConnection();
		connection.init(createPluginSettings());

		return connection.createTransferManager();
	}
}
