package org.syncany.tests.connection.local;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.CookieHandler;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.PluginInfo;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.connection.plugins.local.LocalPluginInfo;
import org.syncany.connection.plugins.local.LocalTransferManager;
import org.syncany.tests.TestUtil;

public class LocalConnectionPluginTest {
	private File tempLocalSourceDir;
	private File tempLocalRepoDir;
	private Map<String, String> localPluginSettings;
	
	@Before
	public void setUp() throws Exception {
		File rootDir = TestUtil.createTempDirectoryInSystemTemp();
		
		tempLocalSourceDir = new File(rootDir+"/local");
		tempLocalSourceDir.mkdir();
		
		tempLocalRepoDir = new File(rootDir+"/repo");		
		tempLocalRepoDir.mkdir();
				
		localPluginSettings = new HashMap<String, String>();
		localPluginSettings.put("path", tempLocalRepoDir.getAbsolutePath());
	}
	
	@After
	public void tearDown() {
		TestUtil.deleteDirectory(tempLocalSourceDir);
	}
	
	@Test
	public void testLoadPluginAndCreateTransferManager() throws StorageException {
		loadPluginAndCreateTransferManager();
	}
	
	@Test
	public void testLocalPluginInfo() {
		PluginInfo pluginInfo = Plugins.get("local");
		
		assertNotNull("PluginInfo should not be null.", pluginInfo);
		assertEquals("Plugin ID should be 'local'.", "local", pluginInfo.getId());
		assertNotNull("Plugin version should not be null.", pluginInfo.getVersion());
		assertNotNull("Plugin name should not be null.", pluginInfo.getName());
	}
	
	@Test(expected=StorageException.class)
	public void testConnectToNonExistantFolder() throws StorageException {
		PluginInfo pluginInfo = Plugins.get("local");
		
		Map<String, String> invalidPluginSettings = new HashMap<String, String>();
		invalidPluginSettings.put("path", "/path/does/not/exist");
		
		Connection connection = pluginInfo.createConnection();
		connection.init(invalidPluginSettings);
		
		TransferManager transferManager = connection.createTransferManager();
		
		// This should cause a Storage exception, because the path does not exist
		transferManager.connect();		
	}
	
	@Test(expected=StorageException.class)
	public void testConnectWithInvalidSettings() throws StorageException {
		PluginInfo pluginInfo = Plugins.get("local");
		
		Map<String, String> invalidPluginSettings = new HashMap<String, String>();
		// do NOT add 'path'
		
		Connection connection = pluginInfo.createConnection();
		connection.init(invalidPluginSettings);
		
		TransferManager transferManager = connection.createTransferManager();
		
		// This should cause a Storage exception, because the path does not exist
		transferManager.connect();		
	}

	@Test
	public void testLocalUploadListDownloadAndDelete() throws Exception {				
		// Generate test files
		Map<String, File> inputFiles = generateTestInputFile();

		// Create connection, upload, list, download
		TransferManager transferManager = loadPluginAndCreateTransferManager();		
		transferManager.connect();	

		Map<File, RemoteFile> uploadedFiles = uploadFiles(transferManager, inputFiles.values());
		Map<String, RemoteFile> remoteFiles = transferManager.list();
		Map<RemoteFile, File> downloadedLocalFiles = downloadRemoteFiles(transferManager, remoteFiles.values());

		// Compare
		assertEquals("Number of uploaded files should be the same as the input files.", uploadedFiles.size(), remoteFiles.size());
		assertEquals("Number of remote files should be the same as the downloaded files.", remoteFiles.size(), downloadedLocalFiles.size());
		
		for (Map.Entry<String, File> inputFileEntry : inputFiles.entrySet()) {		
			File inputFile = inputFileEntry.getValue();
			
			RemoteFile uploadedFile = uploadedFiles.get(inputFile);
			File uploadedFileOnRemoteStorage = new File(tempLocalRepoDir+"/"+uploadedFile.getName());
			File downloadedLocalFile = downloadedLocalFiles.get(uploadedFile);
			
			assertNotNull("Cannot be null.", uploadedFile);
			assertNotNull("Cannot be null.", downloadedLocalFile);
			
			byte[] checksumOriginalFile = TestUtil.createChecksum(inputFile);
			byte[] checksumUploadedFile = TestUtil.createChecksum(uploadedFileOnRemoteStorage);
			byte[] checksumDownloadedFile = TestUtil.createChecksum(downloadedLocalFile);
			
			assertArrayEquals("Uploaded file differs from original file.", checksumOriginalFile, checksumUploadedFile);
			assertArrayEquals("Uploaded file differs from original file.", checksumOriginalFile, checksumDownloadedFile);			
		}
		
		// Delete
		for (RemoteFile remoteFileToDelete : uploadedFiles.values()) {			
			transferManager.delete(remoteFileToDelete);
			
			File uploadedFileOnRemoteStorage = new File(tempLocalRepoDir+"/"+remoteFileToDelete.getName());
			assertFalse("Could not delete remote file.", uploadedFileOnRemoteStorage.exists());
		}
		
		assertArrayEquals("Not all files were successfully deleted from remote storage.", new String[] { }, tempLocalRepoDir.list());
	}	
	
	@Test
	public void testDeleteNonExistantFile() throws StorageException {
		TransferManager transferManager = loadPluginAndCreateTransferManager();		
		transferManager.connect();	
		
		boolean fileDeletedSuccessfully = transferManager.delete(new RemoteFile("non-existant-file"));
		assertFalse("File deletion expected to fail.", fileDeletedSuccessfully);
	}
	
	@Test
	public void testListWithPrefix() throws StorageException, IOException {
		// Create remote files directly in the 'remote' directory
		String correctPrefix = "prefix-";
		String incorrectPrefix = "otherprefix-";
		
		Collection<File> remoteFiles = Arrays.asList(new File[] {
			new File(tempLocalRepoDir+"/"+correctPrefix+"1"),
			new File(tempLocalRepoDir+"/"+correctPrefix+"2"),
			new File(tempLocalRepoDir+"/"+correctPrefix+"3"),
			new File(tempLocalRepoDir+"/"+incorrectPrefix+"1")	
		});
		
		for (File remoteFileToCreate : remoteFiles) {
			TestUtil.generateRandomBinaryFile(remoteFileToCreate, 10*1024);
		}
		
		// Connect and list
		TransferManager transferManager = loadPluginAndCreateTransferManager();		
		transferManager.connect();	
		
		Map<String, RemoteFile> listedRemoteFiles = transferManager.list(correctPrefix);
		assertEquals("Expected 3 files with prefix "+correctPrefix, 3, listedRemoteFiles.size());
	}

	private Map<String, File> generateTestInputFile() throws IOException {
		Map<String, File> inputFilesMap = new HashMap<String, File>();
		List<File> inputFiles = TestUtil.generateRandomBinaryFilesInDirectory(tempLocalSourceDir, 50*1024, 10);
		
		for (File file : inputFiles) {
			inputFilesMap.put(file.getName(), file);
		}
		
		return inputFilesMap;
	}
	
	private Map<File, RemoteFile> uploadFiles(TransferManager transferManager, Collection<File> inputFiles) throws StorageException {
		Map<File, RemoteFile> inputFileOutputFile = new HashMap<File, RemoteFile>();
		
		for (File inputFile : inputFiles) {
			RemoteFile remoteOutputFile = new RemoteFile(inputFile.getName());
			transferManager.upload(inputFile, remoteOutputFile);
			
			inputFileOutputFile.put(inputFile, remoteOutputFile);			
			
			assertTrue("Uploaded file does not exist.", new File(tempLocalRepoDir+"/"+remoteOutputFile.getName()).exists());			
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
		PluginInfo pluginInfo = Plugins.get("local");	
		
		Connection connection = pluginInfo.createConnection();				
		connection.init(localPluginSettings);
		
		TransferManager transferManager = connection.createTransferManager();

		assertEquals("LocalPluginInfo expected.", LocalPluginInfo.class, pluginInfo.getClass());
		assertEquals("LocalConnection expected.", LocalConnection.class, connection.getClass());
		assertEquals("LocalTransferManager expected.", LocalTransferManager.class, transferManager.getClass());
		
		return transferManager;
	}
				
}
