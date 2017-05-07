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
package org.syncany.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.RepoTO;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.util.EnvironmentUtil;
import org.syncany.util.StringUtil;

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
	public void testConfigHelperLoadConfig() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		
		// Run
		Config loadedConfig = ConfigHelper.loadConfig(testConfig.getLocalDir());
		
		// Test
		assertNotNull(loadedConfig);
		assertEquals(testConfig.getAppDir(), loadedConfig.getAppDir());
		assertEquals(testConfig.getCacheDir(), loadedConfig.getCacheDir());
		assertNotNull(loadedConfig.getChunker());
		assertEquals(testConfig.getChunker().toString(), loadedConfig.getChunker().toString());
		assertNotNull(loadedConfig.getCache());
		assertNotNull(loadedConfig.getConnection());
		assertEquals(testConfig.getDatabaseDir(), loadedConfig.getDatabaseDir());		
		assertEquals(testConfig.getDatabaseFile(), loadedConfig.getDatabaseFile());
		assertEquals(testConfig.getDisplayName(), loadedConfig.getDisplayName());
		assertEquals(testConfig.getLocalDir(), loadedConfig.getLocalDir());
		assertEquals(testConfig.getLogDir(), loadedConfig.getLogDir());
		assertEquals(testConfig.getMachineName(), loadedConfig.getMachineName());
		assertEquals(testConfig.getMasterKey(), loadedConfig.getMasterKey());
		assertNotNull(loadedConfig.getMultiChunker());
		assertNotNull(loadedConfig.getRepoId());
		assertNotNull(loadedConfig.getTransformer());

		// Tear down
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);		
	}
	
	@Test
	public void testConfigHelperLoadConfigTO() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		
		// Run
		ConfigTO loadedConfigTO = ConfigHelper.loadConfigTO(testConfig.getLocalDir());
		
		// Test
		assertNotNull(loadedConfigTO);
		assertEquals(testConfig.getDisplayName(), loadedConfigTO.getDisplayName());
		assertEquals(testConfig.getMachineName(), loadedConfigTO.getMachineName());
		assertEquals(testConfig.getMasterKey(), loadedConfigTO.getMasterKey());
		
		// Tear down
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);		
	}
	
	@Test
	public void testConfigHelperLoadRepoTO() throws Exception {		
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		
		// Run
		ConfigTO loadedConfigTO = ConfigHelper.loadConfigTO(testConfig.getLocalDir());
		RepoTO repoConfigTO = ConfigHelper.loadRepoTO(testConfig.getLocalDir(), loadedConfigTO);
		
		// Test
		assertNotNull(repoConfigTO);
		assertNotNull(repoConfigTO.getChunkerTO());
		assertNotNull(repoConfigTO.getMultiChunker());
		assertNotNull(repoConfigTO.getRepoId());
		
		if (TestConfigUtil.getCrypto()) {
			assertNotNull(repoConfigTO.getTransformers());
			assertEquals(2, repoConfigTO.getTransformers().size());
			assertEquals("gzip", repoConfigTO.getTransformers().get(0).getType());
			assertEquals("cipher", repoConfigTO.getTransformers().get(1).getType());
		}
		else {
			assertNull(repoConfigTO.getTransformers());
		}
		
		assertEquals("fixed", repoConfigTO.getChunkerTO().getType());
		assertEquals(1, repoConfigTO.getChunkerTO().getSettings().size());
		assertEquals("zip", repoConfigTO.getMultiChunker().getType());
		assertEquals(1, repoConfigTO.getMultiChunker().getSettings().size());
		assertEquals("010203", StringUtil.toHex(repoConfigTO.getRepoId()));
		
		// Tear down
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);			
	}
}
