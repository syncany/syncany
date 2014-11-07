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
package org.syncany.tests.plugins;

import java.io.File;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.plugins.Plugin;
import org.syncany.plugins.Plugins;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransactionAwareTransferManager;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.plugins.transfer.files.DatabaseRemoteFile;
import org.syncany.plugins.transfer.files.MasterRemoteFile;
import org.syncany.plugins.transfer.files.MultichunkRemoteFile;
import org.syncany.plugins.transfer.files.RemoteFile;
import org.syncany.plugins.transfer.files.SyncanyRemoteFile;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.util.StringUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
		TransferPlugin plugin = Plugins.get(getPluginId(), TransferPlugin.class);

		TransferSettings connection = plugin.createEmptySettings();

		// This should cause a Storage exception, because the path does not exist
		TransferManager transferManager = plugin.createTransferManager(connection, null);

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
		uploadDownloadListDelete(transferManager, tempFromDir, tempToDir, SyncanyRemoteFile.class,
				new SyncanyRemoteFile[] { new SyncanyRemoteFile() });

		uploadDownloadListDelete(transferManager, tempFromDir, tempToDir, MasterRemoteFile.class, new MasterRemoteFile[] { new MasterRemoteFile() });

		uploadDownloadListDelete(transferManager, tempFromDir, tempToDir, DatabaseRemoteFile.class, new DatabaseRemoteFile[] {
				new DatabaseRemoteFile("database-A-0001"), new DatabaseRemoteFile("database-B-0002") });

		uploadDownloadListDelete(transferManager, tempFromDir, tempToDir, MultichunkRemoteFile.class, new MultichunkRemoteFile[] {
				new MultichunkRemoteFile("multichunk-84f7e2b31440aaef9b73de3cadcf4e449aeb55a1"),
				new MultichunkRemoteFile("multichunk-beefbeefbeefbeefbeefbeefbeefbeefbeefbeef"),
				new MultichunkRemoteFile("multichunk-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa") });

		// Clear up previous test (if test location is reused)
		cleanTestLocation(transferManager);

		// Clean local location
		TestFileUtil.deleteDirectory(tempFromDir);
		TestFileUtil.deleteDirectory(tempToDir);
	}

	private <T extends RemoteFile> void uploadDownloadListDelete(TransferManager transferManager, File tempFromDir, File tempToDir,
			Class<T> remoteFileClass, T[] remoteFiles) throws Exception {
		for (RemoteFile remoteFile : remoteFiles) {
			File originalLocalFile = new File(tempFromDir, remoteFile.getName());
			File downloadedLocalFile = new File(tempToDir, remoteFile.getName());

			TestFileUtil.createNonRandomFile(originalLocalFile, 5 * 1024);

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
		Map<String, MultichunkRemoteFile> multiChunkFiles = transferManager.list(MultichunkRemoteFile.class);

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

		boolean deleteSuccess = transferManager.delete(new MultichunkRemoteFile("multichunk-dddddddddddddddddddddddddddddddddddddddd")); // does not
																																			// exist
		assertTrue(deleteSuccess);
	}

	private TransferManager loadPluginAndCreateTransferManager() throws StorageException {
		TransferPlugin pluginInfo = Plugins.get(getPluginId(), TransferPlugin.class);

		TransferSettings connection = pluginInfo.createEmptySettings();
		for (Map.Entry<String, String> pair : createPluginSettings().entrySet()) {
			connection.setField(pair.getKey(), pair.getValue());
		}

		return new TransactionAwareTransferManager(pluginInfo.createTransferManager(connection, null), null);
	}
}
