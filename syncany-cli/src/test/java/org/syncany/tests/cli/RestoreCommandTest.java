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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.syncany.cli.CommandLineClient;
import org.syncany.tests.util.TestCliUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

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
		
		new CommandLineClient(new String[] { 
			 "--localdir", clientA.get("localdir"),
			 "restore",
			 "--date=2s",
			 "file1"
		}).start();
		
		assertTrue(new File(clientA.get("localdir"),"file1").exists());
		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
	}	
}
