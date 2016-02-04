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
package org.syncany.tests.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.syncany.Client;
import org.syncany.config.Config;
import org.syncany.crypto.CipherException;
import org.syncany.operations.cleanup.CleanupOperation;
import org.syncany.operations.cleanup.CleanupOperationOptions;
import org.syncany.operations.cleanup.CleanupOperationResult;
import org.syncany.operations.down.DownOperation;
import org.syncany.operations.down.DownOperationOptions;
import org.syncany.operations.down.DownOperationResult;
import org.syncany.operations.init.ConnectOperation;
import org.syncany.operations.init.ConnectOperationOptions;
import org.syncany.operations.init.ConnectOperationResult;
import org.syncany.operations.init.GenlinkOperation;
import org.syncany.operations.init.GenlinkOperationOptions;
import org.syncany.operations.init.GenlinkOperationResult;
import org.syncany.operations.init.InitOperation;
import org.syncany.operations.init.InitOperationOptions;
import org.syncany.operations.init.InitOperationResult;
import org.syncany.operations.log.LogOperation;
import org.syncany.operations.log.LogOperationOptions;
import org.syncany.operations.log.LogOperationResult;
import org.syncany.operations.ls.LsOperation;
import org.syncany.operations.ls.LsOperationOptions;
import org.syncany.operations.ls.LsOperationResult;
import org.syncany.operations.ls_remote.LsRemoteOperation;
import org.syncany.operations.ls_remote.LsRemoteOperationResult;
import org.syncany.operations.plugin.PluginOperation;
import org.syncany.operations.plugin.PluginOperationOptions;
import org.syncany.operations.plugin.PluginOperationResult;
import org.syncany.operations.restore.RestoreOperation;
import org.syncany.operations.restore.RestoreOperationOptions;
import org.syncany.operations.restore.RestoreOperationResult;
import org.syncany.operations.status.StatusOperation;
import org.syncany.operations.status.StatusOperationOptions;
import org.syncany.operations.status.StatusOperationResult;
import org.syncany.operations.up.UpOperation;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.operations.up.UpOperationResult;
import org.syncany.operations.update.UpdateOperation;
import org.syncany.operations.update.UpdateOperationOptions;
import org.syncany.operations.update.UpdateOperationResult;
import org.syncany.operations.watch.WatchOperation;
import org.syncany.operations.watch.WatchOperationOptions;
import org.syncany.plugins.UserInteractionListener;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.unit.util.TestFileUtil;

public class TestClient extends Client {
	private Config config;

	public TestClient(String machineName, TransferSettings connection) throws Exception {
		Config testConfig = TestConfigUtil.createTestLocalConfig(machineName, connection);

		testConfig.setMachineName(machineName);
		testConfig.setDisplayName(machineName);

		config = testConfig;
	}

	public Config getConfig() {
		return config;
	}

	public UpOperationResult up() throws Exception {
		return up(new UpOperationOptions());
	}

	public UpOperationResult up(UpOperationOptions options) throws Exception {
		return new UpOperation(config, options).execute();
	}

	public DownOperationResult down() throws Exception {
		return down(new DownOperationOptions());
	}

	public DownOperationResult down(DownOperationOptions options) throws Exception {
		return new DownOperation(config, options).execute();
	}

	public StatusOperationResult status() throws Exception {
		return status(new StatusOperationOptions());
	}

	public StatusOperationResult status(StatusOperationOptions options) throws Exception {
		return new StatusOperation(config, options).execute();
	}

	public LsRemoteOperationResult lsRemote() throws Exception {
		return new LsRemoteOperation(config).execute();
	}

	public RestoreOperationResult restore(RestoreOperationOptions options) throws Exception {
		return new RestoreOperation(config, options).execute();
	}

	public LsOperationResult ls(LsOperationOptions options) throws Exception {
		return new LsOperation(config, options).execute();
	}

	public LogOperationResult log(LogOperationOptions options) throws Exception {
		return new LogOperation(config, options).execute();
	}

	public void watch(WatchOperationOptions options) throws Exception {
		new WatchOperation(config, options).execute();
	}

	public GenlinkOperationResult genlink(GenlinkOperationOptions options) throws Exception {
		return new GenlinkOperation(config, options).execute();
	}

	public InitOperationResult init(InitOperationOptions options) throws Exception {
		return init(options, null);
	}

	public InitOperationResult init(InitOperationOptions options, UserInteractionListener listener) throws Exception {
		return new InitOperation(options, listener).execute();
	}

	public ConnectOperationResult connect(ConnectOperationOptions options) throws Exception {
		return connect(options, null);
	}

	public ConnectOperationResult connect(ConnectOperationOptions options, UserInteractionListener listener) throws Exception,
			CipherException {

		return new ConnectOperation(options, listener).execute();
	}

	public CleanupOperationResult cleanup() throws Exception {
		return new CleanupOperation(config, new CleanupOperationOptions()).execute();
	}

	public CleanupOperationResult cleanup(CleanupOperationOptions options) throws Exception {
		return new CleanupOperation(config, options).execute();
	}

	public PluginOperationResult plugin(PluginOperationOptions options) throws Exception {
		return new PluginOperation(config, options).execute();
	}

	public UpdateOperationResult update(UpdateOperationOptions options) throws Exception {
		return new UpdateOperation(config, options).execute();
	}

	public UpOperationResult upWithForceChecksum() throws Exception {
		StatusOperationOptions statusOptions = new StatusOperationOptions();
		statusOptions.setForceChecksum(true);

		UpOperationOptions upOptions = new UpOperationOptions();
		upOptions.setStatusOptions(statusOptions);

		return up(upOptions);
	}

	public void sync() throws Exception {
		up();
		down();
	}

	public Thread watchAsThread(final int interval) {
		return new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					WatchOperationOptions watchOperationOptions = new WatchOperationOptions();

					watchOperationOptions.setAnnouncements(false);
					watchOperationOptions.setWatcher(false);
					watchOperationOptions.setInterval(interval);

					watch(watchOperationOptions);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}, "watchOperationsThread");
	}

	public void createNewFiles() throws IOException {
		TestFileUtil.createRandomFilesInDirectory(config.getLocalDir(), 25 * 1024, 20);
	}

	public void createNewFiles(String inFolder) throws IOException {
		TestFileUtil.createRandomFilesInDirectory(getLocalFile(inFolder), 25 * 1024, 20);
	}

	public File createNewFile(String name) throws IOException {
		return createNewFile(name, 50 * 1024);
	}

	public File createNewFileInFolder(String name, String rootFolder) throws IOException {
		return createNewFileInFolder(name, rootFolder, 50 * 1024);
	}

	public File createNewFile(String name, long size) throws IOException {
		File localFile = getLocalFile(name);
		TestFileUtil.createNonRandomFile(localFile, size);

		return localFile;
	}

	public File createNewFileInFolder(String name, String rootFolder, long size) throws IOException {
		File localFile = getLocalFileInFolder(name, rootFolder);
		TestFileUtil.createNonRandomFile(localFile, size);

		return localFile;
	}

	public File createFileWithContent(String name, String content) throws IOException {
		File localFile = getLocalFile(name);
		TestFileUtil.createFileWithContent(localFile, content);
		return localFile;
	}

	public void createNewFolder(String name) {
		getLocalFile(name).mkdirs();
	}

	public void moveFile(String fileFrom, String fileTo) throws Exception {
		File fromLocalFile = getLocalFile(fileFrom);
		File toLocalFile = getLocalFile(fileTo);

		try {
			if (fromLocalFile.isDirectory()) {
				FileUtils.moveDirectory(fromLocalFile, toLocalFile);
			}
			else {
				FileUtils.moveFile(fromLocalFile, toLocalFile);
			}
		}
		catch (Exception e) {
			throw new Exception("Move failed: " + fileFrom + " --> " + fileTo, e);
		}
	}

	public void copyFile(String fileFrom, String fileTo) throws IOException {
		FileUtils.copyFile(getLocalFile(fileFrom), getLocalFile(fileTo));
	}

	public void changeFile(String name) throws IOException {
		TestFileUtil.changeRandomPartOfBinaryFile(getLocalFile(name));
	}

	public boolean deleteFile(String name) {
		return FileUtils.deleteQuietly(getLocalFile(name));
	}

	public void deleteTestData() {
		TestConfigUtil.deleteTestLocalConfigAndData(config);
	}

	public File getLocalFile(String name) {
		return new File(config.getLocalDir() + "/" + name);
	}

	public File getLocalFileInFolder(String name, String rootFolder) {
		return new File(config.getLocalDir() + "/" + rootFolder + "/" + name);
	}

	public Map<String, File> getLocalFiles() throws FileNotFoundException {
		return TestFileUtil.getLocalFiles(config.getLocalDir());
	}

	public Map<String, File> getLocalFilesExcludeLockedAndNoRead() throws FileNotFoundException {
		return TestFileUtil.getLocalFilesExcludeLockedAndNoRead(config.getLocalDir());
	}

	public File getDatabaseFile() {
		return config.getDatabaseFile();
	}

	public TestSqlDatabase loadLocalDatabase() throws IOException {
		return new TestSqlDatabase(config);
	}
}
