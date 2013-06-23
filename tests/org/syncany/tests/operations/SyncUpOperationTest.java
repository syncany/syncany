package org.syncany.tests.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.database.Database;
import org.syncany.database.DatabaseDAO;
import org.syncany.operations.LocalDatabaseFile;
import org.syncany.operations.RemoteDatabaseFile;
import org.syncany.operations.SyncUpOperation;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

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

		List<File> originalFiles = TestFileUtil.generateRandomBinaryFilesInDirectory(testConfig.getLocalDir(), fileSize,
				fileAmount);
		
		// Run!
		SyncUpOperation op = new SyncUpOperation(testConfig);		
		op.execute();

		//Compare dbs
		LocalConnection localConnection = (LocalConnection) testConfig.getConnection();
		
		File localDatabaseFile = new File(testConfig.getAppDatabaseDir() + "/local.db");
		File remoteDatabaseFile = new File(localConnection.getRepositoryPath() + "/db-" + testConfig.getMachineName()+"-1");
		
		assertTrue(localDatabaseFile.exists());
		assertTrue(remoteDatabaseFile.exists());
		assertEquals(TestFileUtil.getMD5Checksum(localDatabaseFile), TestFileUtil.getMD5Checksum(remoteDatabaseFile));
		
		DatabaseDAO dDAO = new DatabaseDAO();
		Database localDatabase = new Database();
		Database remoteDatabase = new Database();
		dDAO.load(localDatabase, new LocalDatabaseFile(localDatabaseFile));
		dDAO.load(remoteDatabase, new RemoteDatabaseFile(remoteDatabaseFile));
		
		//assertEquals(localDatabase, remoteDatabase);
		// TODO
		//compare files listed in db remote & local 
	}

	

	

}
