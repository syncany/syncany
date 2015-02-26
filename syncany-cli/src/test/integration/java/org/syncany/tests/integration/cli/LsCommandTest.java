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

import static org.junit.Assert.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.junit.Test;
import org.syncany.cli.CommandLineClient;
import org.syncany.tests.unit.util.TestCliUtil;
import org.syncany.tests.unit.util.TestFileUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.util.StringUtil;

public class LsCommandTest {	
	@Test
	public void testLsCommand() throws Exception {
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);

		// No lines expected
		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"--localdir", clientA.get("localdir"),
			"ls"
		}));

		assertEquals("Different number of output lines expected.", 1, cliOut.length);
		assertEquals("", StringUtil.join(cliOut, ""));

		// Create some files
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/file1"), 20*1024);
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/file2"), 20*1024);
		
		new File(clientA.get("localdir")+"/folder/subfolder").mkdirs();
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/folder/fileinfolder"), 20*1024);
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/folder/subfolder/fileinsubfolder"), 20*1024);
		
		new CommandLineClient(new String[] { 
			 "--localdir", clientA.get("localdir"),
			 "up", "--force-checksum"
		}).start();
	
		TestFileUtil.changeRandomPartOfBinaryFile(new File(clientA.get("localdir")+"/folder/subfolder/fileinsubfolder"));
		TestFileUtil.changeRandomPartOfBinaryFile(new File(clientA.get("localdir")+"/file2"));
		
		Thread.sleep(1500);
		Date beforeSecondUpTime = new Date();
		
		new CommandLineClient(new String[] { 
			 "--localdir", clientA.get("localdir"),
			 "up", "--force-checksum"
		}).start();
		
		// Check 'ls' output
		cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"--localdir", clientA.get("localdir"),
			"ls"
		}));
		
		assertEquals("Different number of output lines expected.", 3, cliOut.length);
		assertTrue(cliOut[0].contains("1 file1"));
		assertTrue(cliOut[1].contains("2 file2"));
		assertTrue(cliOut[2].contains("1 folder"));
		
		// Check 'ls --recursive' output
		cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"--localdir", clientA.get("localdir"),
			"ls", "--recursive"
		}));
		
		assertEquals("Different number of output lines expected.", 6, cliOut.length);
		assertTrue(cliOut[0].contains("1 file1"));
		assertTrue(cliOut[1].contains("2 file2"));
		assertTrue(cliOut[2].contains("1 folder"));
		assertTrue(cliOut[3].contains("1 folder/fileinfolder"));
		assertTrue(cliOut[4].contains("1 folder/subfolder"));
		assertTrue(cliOut[5].contains("2 folder/subfolder/fileinsubfolder"));
		
		// Check 'ls --versions --recursive' output
		cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"--localdir", clientA.get("localdir"),
			"ls", "--versions", "--recursive"
		}));
		
		assertEquals("Different number of output lines expected.", 8, cliOut.length);
		assertTrue(cliOut[0].contains("1 file1"));
		assertTrue(cliOut[1].contains("1 file2"));
		assertTrue(cliOut[2].contains("2 file2"));
		assertTrue(cliOut[3].contains("1 folder"));
		assertTrue(cliOut[4].contains("1 folder/fileinfolder"));
		assertTrue(cliOut[5].contains("1 folder/subfolder"));
		assertTrue(cliOut[6].contains("1 folder/subfolder/fileinsubfolder"));
		assertTrue(cliOut[7].contains("2 folder/subfolder/fileinsubfolder"));
		
		// Check 'ls --versions --group' output
		cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"--localdir", clientA.get("localdir"),
			"ls", "--versions", "--group"
		}));
		
		assertEquals("Different number of output lines expected.", 9, cliOut.length); 
		
		assertTrue(cliOut[0].contains(", file1"));
		assertTrue(cliOut[1].contains("1 file1"));
		assertEquals("", cliOut[2].trim());
		assertTrue(cliOut[3].contains(", file2"));
		assertTrue(cliOut[4].contains("1 file2"));
		assertTrue(cliOut[5].contains("2 file2"));
		assertEquals("", cliOut[6].trim());
		assertTrue(cliOut[7].contains(", folder"));
		assertTrue(cliOut[8].contains("1 folder"));
		
		// Check 'ls --types=d' output
		cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"--localdir", clientA.get("localdir"),
			"ls", "--types=d"
		}));
		
		assertEquals("Different number of output lines expected.", 1, cliOut.length); 		
		assertTrue(cliOut[0].contains("folder"));
		
		// Check 'ls --date=..' output
		cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"--localdir", clientA.get("localdir"),
			"ls", "--date", new SimpleDateFormat("yy-MM-dd HH:mm:ss").format(beforeSecondUpTime)
		}));
		
		assertEquals("Different number of output lines expected.", 3, cliOut.length); 		
		assertTrue(cliOut[0].contains("1 file1"));
		assertTrue(cliOut[1].contains("1 file2"));
		assertTrue(cliOut[2].contains("folder"));
		
		// Check 'ls folder/' output
		cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"--localdir", clientA.get("localdir"),
			"ls", "folder/"
		}));
		
		assertEquals("Different number of output lines expected.", 2, cliOut.length); 		
		assertTrue(cliOut[0].contains("1 folder/fileinfolder"));
		assertTrue(cliOut[1].contains("1 folder/subfolder"));

		TestCliUtil.deleteTestLocalConfigAndData(clientA);
	}	
}
