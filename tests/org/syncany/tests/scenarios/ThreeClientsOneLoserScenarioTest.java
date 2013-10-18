package org.syncany.tests.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.operations.DownOperation.DownOperationResult;
import org.syncany.operations.DownOperation.DownOperationResult.DownResultCode;
import org.syncany.operations.UpOperation.UpOperationOptions;
import org.syncany.operations.UpOperation.UpOperationResult;
import org.syncany.operations.UpOperation.UpOperationResult.UpResultCode;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class ThreeClientsOneLoserScenarioTest {
	@Test
	public void testChangeAttributes() throws Exception {		
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);

		UpOperationOptions cUpOptionsWithForce = new UpOperationOptions();
		cUpOptionsWithForce.setForceUploadEnabled(true);
		
		// Run 
		clientA.createNewFile("file1.jpg");
		clientA.upWithForceChecksum();
		
		clientB.down();		
		clientB.createNewFile("file2.jpg");
		clientB.upWithForceChecksum();
		
		clientC.createNewFile("file3.jpg"); // Client C: No down!
		clientC.up(cUpOptionsWithForce);		
		
		// A tries to upload, this fails due to C's unknown database
		clientA.createNewFile("file4.jpg");
		UpOperationResult aUpResult = clientA.upWithForceChecksum(); // 
		assertEquals("Expected to fail, because db-C-1 has not been looked at", UpResultCode.NOK_UNKNOWN_DATABASES, aUpResult.getResultCode());
		
		// A downloads C's changes, no file changes are expected
		DownOperationResult aDownResult = clientA.down(); 
		assertEquals("Expected to succeed with remote changes (a new database file, but no file changes!).", DownResultCode.OK_WITH_REMOTE_CHANGES, aDownResult.getResultCode());
		// TODO [low] Add assert: "no file changes are expected"
		
		// A uploads again, this time it should succeed, because C's file is in knowndbs.list
		aUpResult = clientA.upWithForceChecksum(); 
		assertEquals("Expected to succeed, because db-C-1 has already been looked at", UpResultCode.OK_APPLIED_CHANGES, aUpResult.getResultCode());
		
		// C calls down and up, to sync its changes
		clientC.down();
		clientC.upWithForceChecksum(); // FIXME TODO [high] Should C upload db-C-1 or db-C-2? Using db-C-2 might cause problems in DatabaseReconciliator?! Using db-C-1 leads other clients ignoring this file from the repo (because they think they have seen it before)
		
		clientA.down();
		assertFileListEquals(clientA.getLocalFiles(), clientC.getLocalFiles());
		
		fail("xx");
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
		clientC.cleanup();
	}	
	
}
