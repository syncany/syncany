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

public class LsRemoteCommandTest {
	@Test
	public void testLsRemoteCommand() throws Exception {
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);
		Map<String, String> clientB = TestCliUtil.createLocalTestEnvAndConnect("B", connectionSettings);

		// Round 1: No changes / remote databases expected
		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
				"--localdir", clientA.get("localdir"),
				"ls-remote"
		}));

		assertEquals("Different number of output lines expected.", 3, cliOut.length);

		// Round 2: One new database expected
		TestFileUtil.createRandomFile(new File(clientB.get("localdir") + "/file1"), 20 * 1024);
		TestFileUtil.createRandomFile(new File(clientB.get("localdir") + "/file2"), 20 * 1024);

		new CommandLineClient(new String[] {
				"--localdir", clientB.get("localdir"),
				"up",
		}).start();

		cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
				"--localdir", clientA.get("localdir"),
				"ls-remote"
		}));

		assertEquals("Different number of output lines expected.", 3, cliOut.length);
		assertEquals("? database-B-0000000001", cliOut[2]);

		TestCliUtil.deleteTestLocalConfigAndData(clientA);
		TestCliUtil.deleteTestLocalConfigAndData(clientB);
	}
}
