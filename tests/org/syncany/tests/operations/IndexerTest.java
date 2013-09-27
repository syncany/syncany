package org.syncany.tests.operations;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.syncany.tests.util.TestFileUtil;

public class IndexerTest {
	private File tempLocalSourceDir;
	private File tempLocalCacheDir;
	
	@Before
	public void setUp() throws Exception {
		File rootDir = TestFileUtil.createTempDirectoryInSystemTemp();
		
		tempLocalSourceDir = new File(rootDir+"/local");
		tempLocalSourceDir.mkdir();
		
		tempLocalCacheDir = new File(rootDir+"/cache");		
		tempLocalCacheDir.mkdir();
	}
	
	@After
	public void tearDown() {
		TestFileUtil.deleteDirectory(tempLocalSourceDir);
	}
	
	@Test
	@Ignore
	public void testDeduperWithDatabase() throws IOException {
		// TODO [high] write indexer test
	}

}
