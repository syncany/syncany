package org.syncany.tests.operations;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.syncany.tests.util.TestFileUtil;

public class SyncDownOperationTest {
	private File tempLocalSourceDir;
	private File tempLocalRepoDir;
	
	@Before
	public void setUp() throws Exception {
		tempLocalSourceDir = TestFileUtil.createTempDirectoryInSystemTemp();
			
		
	}
	
	@After
	public void tearDown() {
		TestFileUtil.deleteDirectory(tempLocalSourceDir);
	}
	
	@Test
	@Ignore
	public void testSyncUpWithOneFile() {
		
	}

}
