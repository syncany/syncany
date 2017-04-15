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
import static org.junit.contrib.java.lang.system.TextFromStandardInputStream.emptyStandardInputStream;

import java.io.File;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.TextFromStandardInputStream;
import org.syncany.cli.CommandLineClient;
import org.syncany.tests.unit.util.TestCliUtil;
import org.syncany.tests.unit.util.TestFileUtil;
import org.syncany.tests.util.TestConfigUtil;

public class GenlinkCommandTest {
	@Rule
	public final TextFromStandardInputStream systemInMock = emptyStandardInputStream();

	// TODO [low] TextFromStandardInputStream is not thread-safe. This leads to failures from time to time.

	@Test
	public void testGenlinkCommandShortNotEncrypted() throws Exception {
		// Setup
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);
		File clientLocalDirB = TestFileUtil.createTempDirectoryInSystemTemp();

		String[] cliOutA = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
				"--localdir", clientA.get("localdir"),
				"genlink",
				"--machine-readable"
		}));

		assertEquals("Different number of output lines expected.", 1, cliOutA.length);
		String createdLink = cliOutA[0];

		String[] cliOutB = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
				"--localdir", clientLocalDirB.getAbsolutePath(),
				"connect",
				createdLink
		}));

		assertEquals("Different number of output lines expected.", 3, cliOutB.length);
		assertEquals("Repository connected, and local folder initialized.", cliOutB[1]);

		TestCliUtil.deleteTestLocalConfigAndData(clientA);
		TestFileUtil.deleteDirectory(clientLocalDirB);
	}

	@Test
	public void testGenlinkCommandShortEncrypted() throws Exception {
		// Setup		
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnv("A", connectionSettings);
		File clientLocalDirB = TestFileUtil.createTempDirectoryInSystemTemp();

		//systemInMock.provideText("somelongpassword\nsomelongpassword\nsomelongpassword\n");

		// Run Init
		String[] initArgs = new String[] {
				"--localdir", clientA.get("localdir"),
				"init",
				"--plugin", "local",
				"--plugin-option", "path=" + clientA.get("repopath"),
				"--no-compression",
				"--password", "somelongpassword"
		};

		TestCliUtil.runAndCaptureOutput(new CommandLineClient(initArgs));

		// Run Genlink (on A)		
		String[] cliOutA = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
				"--localdir", clientA.get("localdir"),
				"genlink",
				"--machine-readable"
		}));

		assertEquals("Different number of output lines expected.", 1, cliOutA.length);
		String createdLink = cliOutA[0];

		String[] cliOutB = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
				"--localdir", clientLocalDirB.getAbsolutePath(),
				"connect",
				"--password", "somelongpassword",
				createdLink
		}));

		assertEquals("Different number of output lines expected.", 5, cliOutB.length);
		assertEquals("Repository connected, and local folder initialized.", cliOutB[3]);

		TestCliUtil.deleteTestLocalConfigAndData(clientA);
		TestFileUtil.deleteDirectory(clientLocalDirB);
	}
}
