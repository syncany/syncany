/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.tests.cli;

import static org.syncany.tests.util.TestAssertUtil.assertFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEqualsExcludeLockedAndNoRead;

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
		assertFileListEqualsExcludeLockedAndNoRead(new File(clientA.get("localdir")), new File(clientB.get("localdir")));

		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
		TestCliUtil.deleteTestLocalConfigAndData(clientB);
	}			
}
