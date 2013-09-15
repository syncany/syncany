package org.syncany.tests.scenarios;

import static org.syncany.tests.util.TestAssertUtil.assertDatabaseFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

public class FileTreeMoveToSubfolderScenarioTest {
	@Test
	public void testCreateFileTreeAndMoveSubfolder() throws Exception {		
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
				
		// Run 
		clientA.createNewFolder("A-folder1");
		clientA.createNewFolder("A-folder1/A-subfolder1");
		clientA.createNewFolder("A-folder1/A-subfolder2");
		clientA.createNewFolder("A-folder1/A-subfolder2/A-subsubfolder1");
		clientA.createNewFolder("A-folder1/A-subfolder2/A-subsubfolder2");
		TestFileUtil.createRandomFilesInDirectory(clientA.getLocalFile("A-folder1/A-subfolder1"), 500*1024, 15);
		clientA.up();		
		
		clientB.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		
		clientB.moveFile("A-folder1/A-subfolder1", "A-folder1/A-subfolder2/B-subsubfolder1");
		clientB.up();
		
		clientA.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
}
