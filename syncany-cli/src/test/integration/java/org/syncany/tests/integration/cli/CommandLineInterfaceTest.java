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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.syncany.cli.CommandLineClient;
import org.syncany.config.Config;
import org.syncany.tests.unit.util.TestCliUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.util.StringUtil;

public class CommandLineInterfaceTest {
	private static final Logger logger = Logger.getLogger(CommandLineInterfaceTest.class.getSimpleName());

	@Test
	public void testCliInitAndConnect() throws Exception {
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnv("A", connectionSettings);
		Map<String, String> clientB = TestCliUtil.createLocalTestEnv("B", connectionSettings);

		// Init
		String[] initArgs = new String[] {
				"--localdir", clientA.get("localdir"),
				"init",
				"--plugin", "local",
				"--plugin-option", "path=" + clientA.get("repopath"),
				"--no-encryption",
				"--no-compression"
		};

		logger.log(Level.INFO, "Running syncany with argument: " + StringUtil.join(initArgs, " "));
		new CommandLineClient(initArgs).start();

		assertTrue("Repo file in repository should exist.", new File(clientA.get("repopath") + "/syncany").exists());
		assertTrue("Repo file in local client should exist.", new File(clientA.get("localdir") + "/" + Config.DIR_APPLICATION + "/"
				+ Config.FILE_REPO).exists());
		assertTrue("Config file in local client should exist.", new File(clientA.get("localdir") + "/" + Config.DIR_APPLICATION + "/"
				+ Config.FILE_CONFIG).exists());

		// Connect
		String[] connectArgs = new String[] {
				"connect",
				"--localdir", clientB.get("localdir"),
				"--plugin", "local",
				"--plugin-option", "path=" + clientB.get("repopath")
		};

		logger.log(Level.INFO, "Running syncany with argument: " + StringUtil.join(connectArgs, " "));
		new CommandLineClient(connectArgs).start();

		assertTrue("Repo file in local client should exist.", new File(clientB.get("localdir") + "/" + Config.DIR_APPLICATION + "/"
				+ Config.FILE_REPO).exists());
		assertTrue("Config file in local client should exist.", new File(clientB.get("localdir") + "/" + Config.DIR_APPLICATION + "/"
				+ Config.FILE_CONFIG).exists());

		TestCliUtil.deleteTestLocalConfigAndData(clientA);
		TestCliUtil.deleteTestLocalConfigAndData(clientB);
	}

	@Test
	public void testAppFoldersExist() throws Exception {
		// Setup
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);

		// Run!
		new File(clientA.get("localdir") + "/somefolder").mkdir();

		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
				"--localdir", clientA.get("localdir"),
				"up"
		}));

		// Test folder existence
		File appFolder = new File(clientA.get("localdir") + "/" + Config.DIR_APPLICATION);
		File logFolder = new File(appFolder + "/" + Config.DIR_LOG);
		File dbFolder = new File(appFolder + "/" + Config.DIR_DATABASE);
		File cacheFolder = new File(appFolder + "/" + Config.DIR_CACHE);

		assertTrue("App folder should exist", appFolder.exists());
		assertTrue("Logs folder should exist", logFolder.exists());
		assertTrue("Log file should exist", logFolder.list().length > 0);
		assertTrue("Database folder should exist", dbFolder.exists());
		assertTrue("Cache folder should exist", cacheFolder.exists());

		// Test output
		assertEquals("Different number of output lines expected.", 15, cliOut.length);
		assertEquals("A somefolder", cliOut[13]);

		// Cleanup
		TestCliUtil.deleteTestLocalConfigAndData(clientA);
	}
}
