package org.syncany.tests.operations;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.syncany.tests.util.TestUtil;

public class SyncDownOperationTest {
	private File tempLocalSourceDir;
	private File tempLocalRepoDir;
	
	@Before
	public void setUp() throws Exception {
		tempLocalSourceDir = TestUtil.createTempDirectoryInSystemTemp();
			
		
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
