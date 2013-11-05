package org.syncany.tests.cli;

import static org.syncany.tests.util.TestAssertUtil.assertFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.syncany.cli.CommandLineClient;
import org.syncany.tests.util.TestCliUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

public class SyncCommandTest {	
	@Test
	public void testDownCommandNoArgs() throws Exception {
		// Setup
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);
		Map<String, String> clientB = TestCliUtil.createLocalTestEnvAndConnect("B", connectionSettings);
		
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/file1"), 20*1024);
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/file2"), 20*1024);
		
		TestFileUtil.createRandomFile(new File(clientB.get("localdir")+"/file3"), 20*1024);
		TestFileUtil.createRandomFile(new File(clientB.get("localdir")+"/file4"), 20*1024);
				
		// Run
		new CommandLineClient(new String[] {
			"--localdir", clientA.get("localdir"),
			"sync"
		}).start();
		
		new CommandLineClient(new String[] {
			"--localdir", clientB.get("localdir"),
			"sync"
		}).start();
		
		new CommandLineClient(new String[] {
			"--localdir", clientA.get("localdir"),
			"sync"
		}).start();		
		
		assertFileEquals(new File(clientB.get("localdir")+"/file1"), new File(clientA.get("localdir")+"/file1"));
		assertFileEquals(new File(clientB.get("localdir")+"/file2"), new File(clientA.get("localdir")+"/file2"));
		assertFileEquals(new File(clientB.get("localdir")+"/file3"), new File(clientA.get("localdir")+"/file3"));
		assertFileEquals(new File(clientB.get("localdir")+"/file4"), new File(clientA.get("localdir")+"/file4"));
		assertFileListEquals(new File(clientA.get("localdir")), new File(clientB.get("localdir")));

		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
		TestCliUtil.deleteTestLocalConfigAndData(clientB);
	}			
}
