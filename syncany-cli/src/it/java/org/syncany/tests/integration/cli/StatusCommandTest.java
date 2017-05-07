/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.syncany.cli.CommandLineClient;
import org.syncany.config.Config;
import org.syncany.tests.util.TestCliUtil;
import org.syncany.tests.util.TestConfigUtil;

public class StatusCommandTest {		
	@Test
	public void testStatusCommandWithLogLevelOff() throws Exception {
		// Setup
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);

		// Run!
		new File(clientA.get("localdir")+"/somefolder1").mkdir();
		new File(clientA.get("localdir")+"/somefolder2").mkdir();
				
		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] { 
			"--loglevel", "OFF", 
			"--localdir", clientA.get("localdir"),
			"status" 
		}));
		
		// Test
		assertEquals("Different number of output lines expected.", 5, cliOut.length);
		assertEquals("? .syignore", cliOut[2]);
		assertEquals("? somefolder1", cliOut[3]);
		assertEquals("? somefolder2", cliOut[4]);
		// TODO [medium] This test case does NOT test the loglevel option
		
		// Cleanup
		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
	}	
	
	@Test
	public void testStatusCommandWithLogFile() throws Exception {
		// Setup
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);

		File tempLogFile = new File(clientA.get("localdir")+"/"+Config.DIR_APPLICATION
				+"/"+Config.DIR_LOG+"/templogfile");
		
		// Run!
		new File(clientA.get("localdir")+"/somefolder1").mkdir();
		new File(clientA.get("localdir")+"/somefolder2").mkdir();
				
		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] { 
			"--log", tempLogFile.getAbsolutePath(), 
			"--localdir", clientA.get("localdir"),
			"status"
		}));
		
		// Test		
		assertTrue("Log file should exist.", new File(tempLogFile.getAbsolutePath() + ".0").exists());
		assertEquals(5, cliOut.length);
		assertEquals("? .syignore", cliOut[2]);
		assertEquals("? somefolder1", cliOut[3]);
		assertEquals("? somefolder2", cliOut[4]);
		
		// Cleanup
		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
	}
	

	@Test
	public void testStatusCommandWithNoDelete() throws Exception {
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);
		
		new CommandLineClient(new String[] {
				"--localdir", clientA.get("localdir"),
				"up"
		}).start();

		for (int i = 1; i <= 20; i++) {
			new File(clientA.get("localdir") + "/somefolder" + i).mkdir();

			new CommandLineClient(new String[] {
					"--localdir", clientA.get("localdir"),
					"up"
			}).start();
		}

		// Delete file
		new File(clientA.get("localdir") + "/somefolder1").delete();

		// Test status without no-delete parameter
		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
				"--localdir", clientA.get("localdir"),
				"status"
		}));

		assertEquals("Number of output lines", 3, cliOut.length);
		assertTrue(cliOut[2].contains("D somefolder1"));
		
		// Test status with no-delete parameter
		cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
				"--localdir", clientA.get("localdir"),
				"status", "--no-delete"
		}));

		assertEquals("Number of output lines", 3, cliOut.length);
		assertTrue(cliOut[2].contains("No local changes."));
		TestCliUtil.deleteTestLocalConfigAndData(clientA);
	}
}
