/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.plugin.ssh;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.file.nativefs.NativeFileSystemFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthNone;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.connection.plugins.sftp.SftpConnection;
import org.syncany.connection.plugins.sftp.SftpPlugin;
import org.syncany.connection.plugins.sftp.SftpTransferManager;
import org.syncany.tests.util.TestFileUtil;

public class SftpConnectionPluginTest {
	private static File tempLocalSourceDir;
	private static File tempRepoSourceDir;
	
	private Map<String, String> sshPluginSettings;
	private String HOST = "127.0.0.1";
	private static int PORT = 2338;
	private static SshServer sshd;
	
	@BeforeClass
	public static void beforeTestSetup() {
		sshd = SshServer.setUpDefaultServer();
		sshd.setPort(PORT);
		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
		sshd.setFileSystemFactory(new NativeFileSystemFactory());
		
		List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>();
		userAuthFactories.add(new UserAuthNone.Factory());
		sshd.setUserAuthFactories(userAuthFactories);
		//sshd.setPublickeyAuthenticator(new PublickeyAuthenticator());

		sshd.setCommandFactory(new ScpCommandFactory());

		List<NamedFactory<Command>> namedFactoryList = new ArrayList<NamedFactory<Command>>();
		namedFactoryList.add(new SftpSubsystem.Factory());
		sshd.setSubsystemFactories(namedFactoryList);

		try {
			sshd.start();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Before
	public void setUp() throws Exception {
		File rootDir = TestFileUtil.createTempDirectoryInSystemTemp();

		tempLocalSourceDir = new File(rootDir+"/local");
		tempRepoSourceDir = new File(rootDir+"/repo");
		tempLocalSourceDir.mkdir();
		tempRepoSourceDir.mkdir();
		
		sshPluginSettings = new HashMap<String, String>();
		sshPluginSettings.put("hostname", HOST);
		sshPluginSettings.put("username", "user");
		sshPluginSettings.put("password", "pass");
		sshPluginSettings.put("port", "" + PORT);
		sshPluginSettings.put("path", tempRepoSourceDir.getAbsolutePath());
	}
	
	@AfterClass
	public static void tearDown() {
		TestFileUtil.deleteDirectory(tempLocalSourceDir);
		TestFileUtil.deleteDirectory(tempRepoSourceDir);
		try {
			sshd.stop(true);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testLoadPluginAndCreateTransferManager() throws StorageException {
		loadPluginAndCreateTransferManager();
	}
	
	@Test
	public void testLocalPluginInfo() {
		Plugin pluginInfo = Plugins.get("sftp");
		
		assertNotNull("PluginInfo should not be null.", pluginInfo);
		assertEquals("Plugin ID should be 'ssh'.", "sftp", pluginInfo.getId());
		assertNotNull("Plugin version should not be null.", pluginInfo.getVersion());
		assertNotNull("Plugin name should not be null.", pluginInfo.getName());
	}
	
	@Test(expected=StorageException.class)
	public void testConnectToNonExistantFolder() throws StorageException {
		Plugin pluginInfo = Plugins.get("sftp");
		
		Map<String, String> invalidPluginSettings = new HashMap<String, String>();
		invalidPluginSettings.put("hostname", HOST);
		invalidPluginSettings.put("username", "user");
		invalidPluginSettings.put("password", "pass");
		invalidPluginSettings.put("port", "" + PORT);
		invalidPluginSettings.put("path", "/path/does/not/exist");
		
		Connection connection = pluginInfo.createConnection();
		connection.init(invalidPluginSettings);
		
		TransferManager transferManager = connection.createTransferManager();
		
		// This should cause a Storage exception, because the path does not exist
		transferManager.connect();	
		transferManager.init();
	}
	
	@Test(expected=StorageException.class)
	public void testConnectWithInvalidSettings() throws StorageException {
		Plugin pluginInfo = Plugins.get("sftp");
		
		Map<String, String> invalidPluginSettings = new HashMap<String, String>();
		
		Connection connection = pluginInfo.createConnection();
		connection.init(invalidPluginSettings);
		
		TransferManager transferManager = connection.createTransferManager();
		
		// This should cause a Storage exception, because the path does not exist
		transferManager.connect();		
	}

	@Test
	public void testSftpUploadListDownloadAndDelete() throws Exception {				
		// Generate test files
		Map<String, File> inputFiles = generateTestInputFile();

		// Create connection, upload, list, download
		TransferManager transferManager = loadPluginAndCreateTransferManager();		
		transferManager.connect();	

		Map<File, RemoteFile> uploadedFiles = uploadChunkFiles(transferManager, inputFiles.values());
		Map<String, RemoteFile> remoteFiles = transferManager.list(RemoteFile.class);
		Map<RemoteFile, File> downloadedLocalFiles = downloadRemoteFiles(transferManager, remoteFiles.values());

		// Compare
		assertEquals("Number of uploaded files should be the same as the input files.", uploadedFiles.size(), remoteFiles.size());
		assertEquals("Number of remote files should be the same as the downloaded files.", remoteFiles.size(), downloadedLocalFiles.size());
		
		for (Map.Entry<String, File> inputFileEntry : inputFiles.entrySet()) {		
			File inputFile = inputFileEntry.getValue();
			
			RemoteFile uploadedFile = uploadedFiles.get(inputFile);
			File downloadedLocalFile = downloadedLocalFiles.get(uploadedFile);
			
			assertNotNull("Cannot be null.", uploadedFile);
			assertNotNull("Cannot be null.", downloadedLocalFile);
			
			byte[] checksumOriginalFile = TestFileUtil.createChecksum(inputFile);
			byte[] checksumDownloadedFile = TestFileUtil.createChecksum(downloadedLocalFile);
			
			assertArrayEquals("Uploaded file differs from original file.", checksumOriginalFile, checksumDownloadedFile);			
		}
		
		// Delete
		for (RemoteFile remoteFileToDelete : uploadedFiles.values()) {			
			transferManager.delete(remoteFileToDelete);
		}
		
		Map<String, RemoteFile> remoteFiles2 = transferManager.list(RemoteFile.class);
		Map<RemoteFile, File> downloadedLocalFiles2 = downloadRemoteFiles(transferManager, remoteFiles2.values());
		
		for (RemoteFile remoteFileToBeDeleted : downloadedLocalFiles2.keySet()) {			
			assertFalse("Could not delete remote file.",downloadedLocalFiles2.containsKey(remoteFileToBeDeleted));
		}
	}	
	
	@Test(expected=StorageException.class)
	public void testDeleteNonExistantFile() throws StorageException {
		TransferManager transferManager = loadPluginAndCreateTransferManager();		
		transferManager.connect();	
		transferManager.delete(new RemoteFile("non-existant-file"));
	}
	
	private Map<String, File> generateTestInputFile() throws IOException {
		Map<String, File> inputFilesMap = new HashMap<String, File>();
		List<File> inputFiles = TestFileUtil.createRandomFilesInDirectory(tempLocalSourceDir, 50*1024, 10);
		
		for (File file : inputFiles) {
			inputFilesMap.put(file.getName(), file);
		}
		
		return inputFilesMap;
	}
	
	private Map<File, RemoteFile> uploadChunkFiles(TransferManager transferManager, Collection<File> inputFiles) throws StorageException {
		Map<File, RemoteFile> inputFileOutputFile = new HashMap<File, RemoteFile>();
		
		for (File inputFile : inputFiles) {
			RemoteFile remoteOutputFile = new RemoteFile(inputFile.getName());
			transferManager.upload(inputFile, remoteOutputFile);
			
			inputFileOutputFile.put(inputFile, remoteOutputFile);			
		}
		
		return inputFileOutputFile;
	}
	
	private Map<RemoteFile, File> downloadRemoteFiles(TransferManager transferManager, Collection<RemoteFile> remoteFiles) throws StorageException {
		Map<RemoteFile, File> downloadedLocalFiles = new HashMap<RemoteFile, File>();
		
		for (RemoteFile remoteFile : remoteFiles) {
			File downloadedLocalFile = new File(tempLocalSourceDir+"/downloaded-"+remoteFile.getName());
			transferManager.download(remoteFile, downloadedLocalFile);
			
			downloadedLocalFiles.put(remoteFile, downloadedLocalFile);
			
			assertTrue("Downloaded file does not exist.", downloadedLocalFile.exists());
		}
		
		return downloadedLocalFiles;
	}	
	
	private TransferManager loadPluginAndCreateTransferManager() throws StorageException {
		Plugin pluginInfo = Plugins.get("sftp");	
		
		Connection connection = pluginInfo.createConnection();				
		connection.init(sshPluginSettings);
		
		TransferManager transferManager = connection.createTransferManager();

		assertEquals("LocalPluginInfo expected.", SftpPlugin.class, pluginInfo.getClass());
		assertEquals("LocalConnection expected.", SftpConnection.class, connection.getClass());
		assertEquals("LocalTransferManager expected.", SftpTransferManager.class, transferManager.getClass());
		
		return transferManager;
	}
}
