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
package org.syncany.tests.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.operations.plugin.ExtendedPluginInfo;
import org.syncany.operations.plugin.PluginOperationOptions;
import org.syncany.operations.plugin.PluginOperationOptions.PluginAction;
import org.syncany.operations.plugin.PluginOperationOptions.PluginListMode;
import org.syncany.operations.plugin.PluginOperationResult;
import org.syncany.operations.plugin.PluginOperationResult.PluginResultCode;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class PluginOperationTest {
	@Test
	public void testPluginListLocalOnly() throws Exception {
		// Setup
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();
		TestClient client = new TestClient("A", testConnection);

		PluginOperationOptions pluginOptions = new PluginOperationOptions();
		pluginOptions.setAction(PluginAction.LIST);
		pluginOptions.setListMode(PluginListMode.LOCAL);
		
		// Run
		PluginOperationResult pluginResult = client.plugin(pluginOptions);
		List<Plugin> pluginList = Plugins.list(); // for comparison only!
		
		// Test
		assertNotNull(pluginResult);
		assertEquals(PluginResultCode.OK, pluginResult.getResultCode());
		assertEquals(pluginList.size(), pluginResult.getPluginList().size());
		assertEquals(2, pluginResult.getPluginList().size()); // local and unreliable_local
		
		for (ExtendedPluginInfo pluginInfo : pluginResult.getPluginList()) {
			assertNull(pluginInfo.getRemotePluginInfo());

			assertNotNull(pluginInfo.getLocalPluginInfo());
			assertNull(pluginInfo.getLocalPluginInfo().getDownloadUrl());
			assertNull(pluginInfo.getLocalPluginInfo().getSha256sum());

			assertNotNull(pluginInfo.getLocalPluginInfo().getPluginId());
			assertNotNull(pluginInfo.getLocalPluginInfo().getPluginVersion()); 
			// The rest is not important for processing ...
			
			assertNotNull(Plugins.get(pluginInfo.getLocalPluginInfo().getPluginId()));
		}
		
		// Tear down
		client.deleteTestData();	
	}
	
	@Test
	public void testPluginListRemoteOnlyReleasesOnly() throws Exception {
		// Tests which plugin releases are available. This is difficult because 
		// that will change. So we can only test the bare minimum.
		
		// Setup
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();
		TestClient client = new TestClient("A", testConnection);

		PluginOperationOptions pluginOptions = new PluginOperationOptions();
		pluginOptions.setAction(PluginAction.LIST);
		pluginOptions.setListMode(PluginListMode.REMOTE);
		pluginOptions.setSnapshots(false);
		
		PluginOperationResult pluginResult = client.plugin(pluginOptions);		
		
		assertNotNull(pluginResult);
		assertEquals(PluginResultCode.OK, pluginResult.getResultCode());
		
		// Tear down
		client.deleteTestData();	
	}
	
	@Test
	public void testPluginListRemoteOnlyIncludingSnapshots() throws Exception {
		// Tests which plugin snapshots are available. This is difficult because 
		// that will change. So we can only test the bare minimum.
		
		// Setup
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();
		TestClient client = new TestClient("A", testConnection);

		PluginOperationOptions pluginOptions = new PluginOperationOptions();
		pluginOptions.setAction(PluginAction.LIST);
		pluginOptions.setListMode(PluginListMode.REMOTE);
		pluginOptions.setSnapshots(true);
		
		PluginOperationResult pluginResult = client.plugin(pluginOptions);		
		
		assertNotNull(pluginResult);
		assertEquals(PluginResultCode.OK, pluginResult.getResultCode());
		
		// Tear down
		client.deleteTestData();	
	}
	
}
