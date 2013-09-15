package org.syncany.tests.operations;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.database.Database;
import org.syncany.database.DatabaseDAO;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseXmlDAO;
import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.operations.SyncUpOperation;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.util.FileUtil;

public class SyncUpOperationTest {
	private Config testConfig;	

	@Before
	public void setUp() throws Exception {
		testConfig = TestConfigUtil.createTestLocalConfig();
	}
	
	@After
	public void tearDown() throws Exception {
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}

	@Test
	public void testUploadLocalDatabase() throws Exception {
		int fileSize = 1230 * 1024;
		int fileAmount = 3;

		List<File> originalFiles = TestFileUtil.createRandomFilesInDirectory(testConfig.getLocalDir(), fileSize,
				fileAmount);
		
		// Run!
		SyncUpOperation op = new SyncUpOperation(testConfig);		
		op.execute();

		//Compare dbs
		LocalConnection localConnection = (LocalConnection) testConfig.getConnection();
		
		File localDatabaseFile = new File(testConfig.getDatabaseDir() + "/local.db");
		File remoteDatabaseFile = new File(localConnection.getRepositoryPath() + "/db-" + testConfig.getMachineName()+"-1");
		
		assertTrue(localDatabaseFile.exists());
		assertTrue(remoteDatabaseFile.exists());
		assertArrayEquals(FileUtil.createChecksum(localDatabaseFile), FileUtil.createChecksum(remoteDatabaseFile));
		
		DatabaseDAO dDAO = new DatabaseXmlDAO(testConfig.getTransformer());
		Database localDatabase = new Database();
		Database remoteDatabase = new Database();
		dDAO.load(localDatabase, localDatabaseFile);
		dDAO.load(remoteDatabase, remoteDatabaseFile);
		
		DatabaseVersion localDatabaseVersion = localDatabase.getLastDatabaseVersion();
		DatabaseVersion remoteDatabaseVersion = remoteDatabase.getLastDatabaseVersion();
		
		assertEquals(localDatabaseVersion.getHeader(), remoteDatabaseVersion.getHeader());

		assertEquals(localDatabaseVersion.getFileHistories().size(),fileAmount);
		assertEquals(localDatabaseVersion.getFileHistories().size(),remoteDatabaseVersion.getFileHistories().size());
		
		Collection<PartialFileHistory> localFileHistories = localDatabaseVersion.getFileHistories();
		Collection<PartialFileHistory> remoteFileHistories = remoteDatabaseVersion.getFileHistories();
	
		List<FileVersion> remoteFileVersions = new ArrayList<FileVersion>(); 
		List<FileVersion> localFileVersions = new ArrayList<FileVersion>();
		
		for (PartialFileHistory partialFileHistory : remoteFileHistories) {
			remoteFileVersions.add(partialFileHistory.getLastVersion());
			assertTrue(localFileHistories.contains(partialFileHistory));
		}
		
		for (PartialFileHistory partialFileHistory : localFileHistories) {
			localFileVersions.add(partialFileHistory.getLastVersion());
		}
		
		assertEquals(localFileVersions,remoteFileVersions);
		
		compareFileVersionsAgainstOriginalFiles(originalFiles, localFileVersions);
		compareFileVersionsAgainstOriginalFiles(originalFiles, remoteFileVersions);
	}

	private void compareFileVersionsAgainstOriginalFiles(List<File> originalFiles, List<FileVersion> localFileVersions) throws Exception {
		int toFind = originalFiles.size();
		for (File originalFile : originalFiles) {
			String originalFileName = originalFile.getName();
			
			for (FileVersion fileVersion : localFileVersions) {
				String fileVersionFileName = fileVersion.getName();
				
				if(fileVersionFileName.equals(originalFileName)) {
					toFind--;
				}
			}
		}
		assertEquals(0, toFind);
	}

}
