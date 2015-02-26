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
import static org.junit.contrib.java.lang.system.TextFromStandardInputStream.emptyStandardInputStream;

import java.io.File;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.TextFromStandardInputStream;
import org.syncany.cli.CommandLineClient;
import org.syncany.cli.util.InitConsole;
import org.syncany.tests.unit.util.TestCliUtil;
import org.syncany.tests.unit.util.TestFileUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.util.StringUtil;

public class InitAndConnectCommandNoEncryptionTest {
	private File originalWorkingDirectory;

	@Rule
	public final TextFromStandardInputStream systemInMock = emptyStandardInputStream();

	@Before
	public void before() {
		originalWorkingDirectory = new File(System.getProperty("user.dir"));
	}

	@After
	public void after() {
		TestCliUtil.setCurrentDirectory(originalWorkingDirectory);
	}

	@Test
	public void testCliInitCommandUninitializedLocalDir() throws Exception {
		// Setup
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		TestCliUtil.setCurrentDirectory(tempDir);

		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnv("A", connectionSettings);

		// Run
		String[] initArgs = new String[] {
				"init",
				"--plugin", "local",
				"--plugin-option", "path=" + clientA.get("repopath"),
				"--no-encryption",
				"--no-compression"
		};

		new CommandLineClient(initArgs).start();

		assertTrue(tempDir.exists());
		assertTrue(new File(tempDir + "/.syncany").exists());
		assertTrue(new File(tempDir + "/.syncany/syncany").exists());
		assertTrue(new File(tempDir + "/.syncany/config.xml").exists());

		// Tear down
		TestCliUtil.setCurrentDirectory(originalWorkingDirectory);

		TestCliUtil.deleteTestLocalConfigAndData(clientA);
		TestFileUtil.deleteDirectory(tempDir);
	}

	@Test
	public void testCliInitCommandInteractive() throws Exception {
		// Setup
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		TestCliUtil.setCurrentDirectory(tempDir);

		// Ensuring no console is set
		InitConsole.setInstance(null);

		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnv("A", connectionSettings);

		// Run
		String[] initArgs = new String[] {
				"init",
				"--no-encryption",
				"--no-compression"
		};

		systemInMock.provideText(StringUtil.join(new String[] {
				"local",
				clientA.get("repopath")
		}, "\n") + "\n");

		new CommandLineClient(initArgs).start();

		assertTrue(tempDir.exists());
		assertTrue(new File(tempDir + "/.syncany").exists());
		assertTrue(new File(tempDir + "/.syncany/syncany").exists());
		assertTrue(new File(tempDir + "/.syncany/config.xml").exists());

		// Tear down
		TestCliUtil.setCurrentDirectory(originalWorkingDirectory);

		TestCliUtil.deleteTestLocalConfigAndData(clientA);
		TestFileUtil.deleteDirectory(tempDir);
	}
}
