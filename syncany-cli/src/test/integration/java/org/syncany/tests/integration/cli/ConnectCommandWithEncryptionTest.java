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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.contrib.java.lang.system.TextFromStandardInputStream.emptyStandardInputStream;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.TextFromStandardInputStream;
import org.syncany.cli.CommandLineClient;
import org.syncany.cli.util.InitConsole;
import org.syncany.database.FileVersionComparator.FileChange;
import org.syncany.tests.unit.util.TestCliUtil;
import org.syncany.tests.unit.util.TestFileUtil;
import org.syncany.tests.util.TestAssertUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.util.StringUtil;

/*
 * Originally the before/after methods in this class were supposed to be @BeforeClass and @AfterClass
 * methods, and the initialized* variables were supposed to be set only once. So basically: 
 * 
 *   1 x init
 *   N x connect tests
 *   
 * Reason behind doing that: Masker key generation takes ages! 
 * 
 * However, the TextFromStandardInputStream is somewhat incompatible with this logic. At least I have not
 * been able to do the above. If you stumble over some weird code, this is probably the reason. 
 */
public class ConnectCommandWithEncryptionTest {
	private File originalWorkingDirectory;

	private File initializedRepoFolder;
	private File initializedRepoFolderCopy;
	private Map<String, String> initializedRepoConnectionSettings;
	private String initializedRepoPassword;
	private String initializedRepoConnectLink;

	@Rule
	public final TextFromStandardInputStream systemInMock = emptyStandardInputStream();

	@Before
	public void before() throws Exception {
		originalWorkingDirectory = new File(System.getProperty("user.dir"));
		initInteractiveWithEncryptionWithParams(); // sets the initialized* variables
	}

	@After
	public void after() {
		TestCliUtil.setCurrentDirectory(originalWorkingDirectory);
		TestFileUtil.deleteDirectory(initializedRepoFolder);
		TestFileUtil.deleteDirectory(initializedRepoFolderCopy);
	}

	@Test
	public void testCliConnectInteractiveWithParamsEnc() throws Exception {
		// Setup
		resetRepo();
		Map<String, String> clientB = TestCliUtil.createLocalTestEnv("B", initializedRepoConnectionSettings);

		// Run
		File localDirB = TestFileUtil.createTempDirectoryInSystemTemp();
		TestCliUtil.setCurrentDirectory(localDirB);

		String[] connectArgs = new String[] {
				"connect"
		};

		System.out.println("repopath = " + clientB.get("repopath"));
		systemInMock.provideText(StringUtil.join(new String[] {
				"local",
				clientB.get("repopath"),
				"somesuperlongpassword"
		}, "\n") + "\n");

		String[] cliOutputA = TestCliUtil.runAndCaptureOutput(new CommandLineClient(connectArgs));
		String cliOutputSingleLineA = StringUtil.join(cliOutputA, " ");

		assertTrue(localDirB.exists());
		assertTrue(new File(localDirB + "/.syncany").exists());
		assertTrue(new File(localDirB + "/.syncany/syncany").exists());
		assertTrue(new File(localDirB + "/.syncany/master").exists());
		assertTrue(new File(localDirB + "/.syncany/config.xml").exists());
		assertTrue(cliOutputSingleLineA.contains("Repository connected"));

		TestAssertUtil.assertFileEquals(new File(localDirB, ".syncany/syncany"), new File(initializedRepoConnectionSettings.get("path"), "syncany"),
				FileChange.CHANGED_ATTRIBUTES, FileChange.CHANGED_LAST_MOD_DATE);
		TestAssertUtil.assertFileEquals(new File(localDirB, ".syncany/master"), new File(initializedRepoConnectionSettings.get("path"), "master"),
				FileChange.CHANGED_ATTRIBUTES, FileChange.CHANGED_LAST_MOD_DATE);

		// Tear down
		TestFileUtil.deleteDirectory(localDirB);
	}

	@Test
	public void testCliConnectInteractiveWithLinkEnc() throws Exception {
		// Setup
		resetRepo();

		// 2. Connect
		File localDirB = TestFileUtil.createTempDirectoryInSystemTemp();
		TestCliUtil.setCurrentDirectory(localDirB);

		String[] connectArgs = new String[] {
				"connect",
				initializedRepoConnectLink
		};

		systemInMock.provideText(StringUtil.join(new String[] {
				// No path or params, not needed because link provided
				initializedRepoPassword
		}, "\n") + "\n");

		String[] cliOutputA = TestCliUtil.runAndCaptureOutput(new CommandLineClient(connectArgs));
		String cliOutputSingleLineA = StringUtil.join(cliOutputA, " ");

		assertTrue(localDirB.exists());
		assertTrue(new File(localDirB + "/.syncany").exists());
		assertTrue(new File(localDirB + "/.syncany/syncany").exists());
		assertTrue(new File(localDirB + "/.syncany/master").exists());
		assertTrue(new File(localDirB + "/.syncany/config.xml").exists());
		assertTrue(cliOutputSingleLineA.contains("Repository connected"));

		TestAssertUtil.assertFileEquals(new File(localDirB, ".syncany/syncany"), new File(initializedRepoConnectionSettings.get("path"), "syncany"),
				FileChange.CHANGED_ATTRIBUTES, FileChange.CHANGED_LAST_MOD_DATE);
		TestAssertUtil.assertFileEquals(new File(localDirB, ".syncany/master"), new File(initializedRepoConnectionSettings.get("path"), "master"),
				FileChange.CHANGED_ATTRIBUTES, FileChange.CHANGED_LAST_MOD_DATE);

		// Tear down
		TestFileUtil.deleteDirectory(localDirB);
	}

	@Test
	@Ignore
	public void testCliConnectInteractiveWithParamsInvalidPassEnc() throws Exception {
		// TODO
	}

	@Test
	@Ignore
	public void testCliConnectInteractiveWithLinkInvalidPassEnc() throws Exception {
		// TODO
	}

	private void initInteractiveWithEncryptionWithParams() throws Exception {
		// Ensuring no console is set
		InitConsole.setInstance(null);

		// Create test repo and temp. client
		initializedRepoPassword = "somesuperlongpassword";
		initializedRepoConnectionSettings = TestConfigUtil.createTestLocalConnectionSettings();

		Map<String, String> clientA = TestCliUtil.createLocalTestEnv("A", initializedRepoConnectionSettings);
		File tempLocalDirA = new File(clientA.get("localdir"));

		initializedRepoFolder = new File(clientA.get("repopath"));

		TestCliUtil.setCurrentDirectory(tempLocalDirA);

		String[] initArgs = new String[] {
				"init"
		};

		systemInMock.provideText(StringUtil.join(new String[] {
				"local",
				initializedRepoFolder.getAbsolutePath(),
				initializedRepoPassword,
				initializedRepoPassword
		}, "\n") + "\n");

		String[] cliOutputA = TestCliUtil.runAndCaptureOutput(new CommandLineClient(initArgs));
		String cliOutputSingleLineA = StringUtil.join(cliOutputA, " ");

		assertTrue(new File(initializedRepoFolder + "/syncany").exists());
		assertTrue(new File(initializedRepoFolder + "/master").exists());
		assertTrue(new File(initializedRepoFolder + "/databases").exists());
		assertTrue(new File(initializedRepoFolder + "/multichunks").exists());

		Pattern linkPattern = Pattern.compile("(syncany://[^\\s]+)");
		Matcher linkMatcher = linkPattern.matcher(cliOutputSingleLineA);

		if (!linkMatcher.find()) {
			fail("Cannot find syncany:// link");
		}

		initializedRepoConnectLink = linkMatcher.group(1);
		initializedRepoFolderCopy = TestFileUtil.createTempDirectoryInSystemTemp();

		// Make copy of the repo
		TestFileUtil.deleteDirectory(initializedRepoFolderCopy);
		FileUtils.copyDirectory(initializedRepoFolder, initializedRepoFolderCopy);

		TestFileUtil.deleteDirectory(tempLocalDirA);
	}

	private void resetRepo() throws IOException {
		TestFileUtil.deleteDirectory(initializedRepoFolder);
		assertFalse(initializedRepoFolder.exists());

		FileUtils.copyDirectory(initializedRepoFolderCopy, initializedRepoFolder);
		assertTrue(initializedRepoFolder.exists());
	}
}
