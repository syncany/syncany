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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.syncany.cli.CommandLineClient;
import org.syncany.tests.unit.util.TestCliUtil;
import org.syncany.tests.unit.util.TestFileUtil;
import org.syncany.tests.util.TestConfigUtil;

/**
 * Testing the restore command
 *
 * @author Pim Otte
 */
public class RestoreCommandTest {
	// TODO [low] Write more restore tests: (1) change file, restore and compare file contents; (2) go back x versions, (3) restore folder 
	
	@Test
	public void testCliSimpleRestore() throws Exception {
		// Setup
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);

		// Run
		TestFileUtil.createRandomFile(new File(clientA.get("localdir"),"file1"), 50L);
		new CommandLineClient(new String[] { 
			 "--localdir", clientA.get("localdir"),
			 "up" 
		}).start();
		
		Thread.sleep(1000);
		
		TestFileUtil.deleteFile(new File(clientA.get("localdir"),"file1"));
		new CommandLineClient(new String[] { 
			 "--localdir", clientA.get("localdir"),
			 "up" 
		}).start();
				
		Thread.sleep(1000);
		
		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] { 
			 "--localdir", clientA.get("localdir"),
			 "ls",
			 "--date=2s"
		}));
		
		assertTrue(cliOut.length >= 1);		
		String fileHistoryId = cliOut[0].split("\\s+")[8];
		
		System.out.println("filehistory id is " + fileHistoryId);
		
		new CommandLineClient(new String[] { 
			 "--localdir", clientA.get("localdir"),
			 "restore",
			 "--revision=1",
			 "--target=restoredfile",
			 fileHistoryId
		}).start();
		
		assertTrue(new File(clientA.get("localdir"),"restoredfile").exists());
		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
	}	
}
