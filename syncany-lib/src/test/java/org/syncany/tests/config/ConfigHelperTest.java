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
package org.syncany.tests.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.config.ConfigHelper;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.util.EnvironmentUtil;

public class ConfigHelperTest {
	@Test
	public void testConfigHelperFindLocalDirInPath() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		File startingPath = testConfig.getLocalDir();
		
		// Run
		File actualLocalDir = ConfigHelper.findLocalDirInPath(startingPath);
		
		// Test
		assertNotNull(actualLocalDir);
		assertEquals(testConfig.getLocalDir(), actualLocalDir.getCanonicalFile());
		
		// Tear down
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testConfigHelperFindLocalDirInPathNotExistent() throws IOException {
		File startingPath = EnvironmentUtil.isWindows() ? new File("C:\\does\\not\\exist") : new File("/does/not/exist");
		
		// Run
		File actualLocalDir = ConfigHelper.findLocalDirInPath(startingPath);
		
		// Test
		assertNull(actualLocalDir);
	}
	
	@Test
	public void testConfigHelperFindLocalDirInPathTwoLevelsDown() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		new File(testConfig.getAppDir()+"/some/folder").mkdirs();
		
		File startingPath = new File(testConfig.getAppDir()+"/some/folder");
		
		// Run
		File actualLocalDir = ConfigHelper.findLocalDirInPath(startingPath);
		
		// Test
		assertNotNull(actualLocalDir);
		assertEquals(testConfig.getLocalDir(), actualLocalDir.getCanonicalFile());
		
		// Tear down
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);		
	}
	
	@Test
	@Ignore
	public void testConfigHelperLoadConfig() throws Exception {
		// Config actualConfig = ConfigHelper.loadConfig(expectedConfig.getLocalDir());		
	}
	
	@Test
	@Ignore
	public void testConfigHelperLoadConfigTO() throws Exception {
		// ConfigHelper.loadConfigTO(localDir);	
	}
	
	@Test
	@Ignore
	public void testConfigHelperLoadRepoTO() throws Exception {		
		// ConfigHelper.loadRepoTO(localDir, configTO);		
	}
}
