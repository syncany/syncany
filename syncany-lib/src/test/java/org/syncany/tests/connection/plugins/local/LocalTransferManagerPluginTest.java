/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.connection.plugins.local;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.connection.plugins.TransferManager.StorageTestResult;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.connection.plugins.local.LocalPlugin;
import org.syncany.connection.plugins.local.LocalTransferManager;
import org.syncany.tests.util.TestFileUtil;

/**
 * 
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class LocalTransferManagerPluginTest {
	private File tempLocalSourceDir;
	private File tempLocalRepoDir;
	private Map<String, String> localPluginSettings;
	private Map<String, String> localPluginSettingsLongFolder;
	
	@Before
	public void setUp() throws Exception {
		File rootDir = TestFileUtil.createTempDirectoryInSystemTemp();
		
		tempLocalSourceDir = new File(rootDir+"/local");
		tempLocalSourceDir.mkdir();
		
		tempLocalRepoDir = new File(rootDir+"/repo");		
		tempLocalRepoDir.mkdir();
				
		localPluginSettings = new HashMap<String, String>();
		localPluginSettings.put("path", tempLocalRepoDir.getAbsolutePath());
		
		localPluginSettingsLongFolder = new HashMap<String, String>();
		localPluginSettingsLongFolder.put("path", tempLocalRepoDir.getAbsolutePath() + "/f1/f2/f3");
	}
	
	@Test
	public void testTestFunctionEmptyRepoPermissionOk() throws StorageException{
		TransferManager tm = loadPluginAndCreateTransferManager();
		Assert.assertEquals(StorageTestResult.NO_REPO_LOCATION_EMPTY_PERMISSIONS_OK, tm.test());
	}
	
	@Test
	public void testTestFunctionRepoNotEmpty() throws StorageException{
		TransferManager tm = loadPluginAndCreateTransferManager();
		File subFolder = new File(tempLocalRepoDir, "temp");
		subFolder.mkdir();
		Assert.assertEquals(StorageTestResult.NO_REPO_LOCATION_NOT_EMPTY, tm.test());
	}
	
	@Test
	public void testTestFunctionExistingRepo() throws StorageException, IOException{
		TransferManager tm = loadPluginAndCreateTransferManager();
		File subFolder = new File(tempLocalRepoDir, "syncany");
		subFolder.createNewFile();
		Assert.assertEquals(StorageTestResult.REPO_ALREADY_EXISTS, tm.test());
	}
	
	@Test
	public void testTestFunction() throws StorageException, IOException{
		Plugin pluginInfo = Plugins.get("local");	
		Connection connection = pluginInfo.createConnection();				
		connection.init(localPluginSettingsLongFolder);
		TransferManager tm = connection.createTransferManager();
		Assert.assertEquals(StorageTestResult.NO_REPO_PERMISSIONS_OK, tm.test());
	}
	
	@After
	public void tearDown() {
		TestFileUtil.deleteDirectory(tempLocalSourceDir);
		TestFileUtil.deleteDirectory(tempLocalRepoDir);
	}
	
	private TransferManager loadPluginAndCreateTransferManager() throws StorageException {
		Plugin pluginInfo = Plugins.get("local");	
		
		Connection connection = pluginInfo.createConnection();				
		connection.init(localPluginSettings);
		
		TransferManager transferManager = connection.createTransferManager();

		assertEquals("LocalPluginInfo expected.", LocalPlugin.class, pluginInfo.getClass());
		assertEquals("LocalConnection expected.", LocalConnection.class, connection.getClass());
		assertEquals("LocalTransferManager expected.", LocalTransferManager.class, transferManager.getClass());
		
		return transferManager;
	}
}
