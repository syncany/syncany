package org.syncany.tests.scenarios;

import static org.syncany.tests.util.TestAssertUtil.assertDatabaseFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

public class Scenario4Test {
	@Test
	public void testEmptyFolderCreateAndSync() throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		// Run 
		clientA.createNewFolder("A-folder1");
		clientA.createNewFolder("A-folder2");
		clientA.createNewFolder("A-folder3");
		clientA.up();		
		
		clientB.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile());
		
		clientB.createNewFolder("B-folder4");
		clientB.createNewFolder("B-folder5");
		clientB.up();
		
		File beforeUpDatabaseFile = TestFileUtil.copyIntoDirectory(clientB.getLocalDatabaseFile(), clientB.getConfig().getAppCacheDir()); 
		clientB.up(); // double-up, has caused problems
		assertFileEquals("Nothing changed. Local database file should not change.", beforeUpDatabaseFile, clientB.getLocalDatabaseFile());
		
		clientA.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile());
		
		Map<String, File> beforeSyncDownFileList = clientB.getLocalFiles();
		clientA.down(); // double-down, has caused problems		
		assertFileListEquals("No change in file lists expected. Nothing changed", beforeSyncDownFileList, clientA.getLocalFiles()); 				
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
}
