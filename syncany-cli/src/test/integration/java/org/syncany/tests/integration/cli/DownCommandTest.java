/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.integration.cli;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.syncany.cli.CommandLineClient;
import org.syncany.tests.unit.util.TestCliUtil;
import org.syncany.tests.unit.util.TestFileUtil;
import org.syncany.tests.util.TestConfigUtil;

public class DownCommandTest {	
	@Test
	public void testDownCommandNoArgs() throws Exception {
		// Setup
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);
		Map<String, String> clientB = TestCliUtil.createLocalTestEnvAndConnect("B", connectionSettings);
		
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/file1"), 20*1024);
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/file2"), 20*1024);
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/file3"), 20*1024);
				
		// Round 1: No changes
		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"--localdir", clientB.get("localdir"),
			"down"
		}));
		
		assertEquals("Different number of output lines expected.", 3, cliOut.length);
		
		// Round 2: Only added files
		new CommandLineClient(new String[] { 
			 "--localdir", clientA.get("localdir"),
			 "up",
			 "--force-checksum"
		}).start();
		
		cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"--localdir", clientB.get("localdir"),
			"down"
		}));
		
		assertEquals("Different number of output lines expected.", 10, cliOut.length);
		assertEquals("A file1", cliOut[6]);
		assertEquals("A file2", cliOut[7]);
		assertEquals("A file3", cliOut[8]);		
		
		// Round 3: Modified and deleted files
		TestFileUtil.changeRandomPartOfBinaryFile(new File(clientA.get("localdir")+"/file2"));
		new File(clientA.get("localdir")+"/file3").delete();
		
		new CommandLineClient(new String[] { 
			 "--localdir", clientA.get("localdir"),
			 "up",
			 "--force-checksum"
		}).start();
		
		cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"--localdir", clientB.get("localdir"),
			"down"
		}));
		
		assertEquals("Different number of output lines expected.", 9, cliOut.length);
		assertEquals("M file2", cliOut[6]);
		assertEquals("D file3", cliOut[7]);
		
		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
		TestCliUtil.deleteTestLocalConfigAndData(clientB);
	}			
}
