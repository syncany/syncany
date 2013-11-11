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
package org.syncany.tests.connection.plugins.unreliable_local;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.operations.WatchOperation.WatchOperationOptions;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class UnreliableLocalPluginTest {
	@Test
	public void testUnreliablePlugin() throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestUnreliableLocalConnection(
			Arrays.asList(new String[] { 
				// List of failing operations (regex)
				// Format: abs=<count> rel=<count> op=<connect|init|upload|...> <operation description>
					
				"upload.+db-A-2"
			}
		));
		
		TestClient clientA = new TestClient("A", testConnection);
		//TestClient clientB = new TestClient("B", testConnection);
		
		WatchOperationOptions watchOperationOptions = new WatchOperationOptions();
		watchOperationOptions.setInterval(1000);
		
		fail("Implement this"); // TODO [medium] Implement failing storage tests
		
		// A new/up
		new Thread(new Runnable() {
			@Override
			public void run() {
				
			}			
		}).start();
		
		clientA.createNewFile("A-original");
		//clientA.up();
		clientA.watch(watchOperationOptions);

		clientA.createNewFile("A-original2");
		clientA.up();
		
		// Tear down
		clientA.cleanup();
		//clientB.cleanup();
	}				
}
