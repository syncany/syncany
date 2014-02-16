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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.syncany.cli.CommandLineClient;
import org.syncany.tests.util.TestCliUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

public class LogCommandTest {	
	@Test
	public void testLogCommandNoArgs() throws Exception {
		// Setup
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);
		
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/file1"), 20*1024);
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/file2"), 20*1024);
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/file3"), 20*1024);
						
		// Prepare (create some database entries)
		new CommandLineClient(new String[] { 
			 "--localdir", clientA.get("localdir"),
			 "up",
			 "--force-checksum"
		}).start();
		
		TestFileUtil.changeRandomPartOfBinaryFile(new File(clientA.get("localdir")+"/file1"));

		new CommandLineClient(new String[] { 
			 "--localdir", clientA.get("localdir"),
			 "up",
			 "--force-checksum"
		}).start();		
		
		// Run 
		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"--localdir", clientA.get("localdir"),
			"log"
		}));
		
		assertEquals("Different number of output lines expected.", 6, cliOut.length);
		// TODO [low] How to test the log command any further? Non-deterministic output!
		
		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
	}			
}
