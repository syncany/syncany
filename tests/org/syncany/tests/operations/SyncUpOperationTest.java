package org.syncany.tests.operations;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.syncany.tests.util.TestUtil;

public class SyncUpOperationTest {
	private File tempLocalSourceDir;
	private File tempLocalRepoDir;
	
	@Before
	public void setUp() throws Exception {
		File rootDir = TestUtil.createTempDirectoryInSystemTemp();
		
		tempLocalSourceDir = new File(rootDir+"/local");
		tempLocalSourceDir.mkdir();
		
		tempLocalRepoDir = new File(rootDir+"/repo");		
		tempLocalRepoDir.mkdir();						
	}
	
	@After
	public void tearDown() {
		TestUtil.deleteDirectory(tempLocalSourceDir);
	}
	
	@Test
	@Ignore
	public void testSyncUpWithOneFile() {
		
	}

}
