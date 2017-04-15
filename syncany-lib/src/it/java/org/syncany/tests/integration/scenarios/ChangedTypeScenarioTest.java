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
package org.syncany.tests.integration.scenarios;

import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

import org.junit.Test;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.integration.scenarios.framework.AbstractClientAction;
import org.syncany.tests.integration.scenarios.framework.ChangeTypeFileToFolder;
import org.syncany.tests.integration.scenarios.framework.ClientActions;
import org.syncany.tests.integration.scenarios.framework.CreateFileTree;
import org.syncany.tests.integration.scenarios.framework.Executable;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class ChangedTypeScenarioTest {
	@Test
	public void testSingleFileChangeTypeToFolder() throws Exception {
		final TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();
		final TestClient clientA = new TestClient("A", testConnection);
		final TestClient clientB = new TestClient("B", testConnection);

		// 1. Client A creates a new file
		clientA.createNewFile("test-file");
		clientA.up();

		// 2. Client A removes file and creates directory with the same name
		clientA.deleteFile("test-file");
		clientA.createNewFolder("test-file");
		clientA.up();

		// 3. Client B downloads repository state
		clientB.down();

		// Verify that client A and client B have identical repositories
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());

		clientA.deleteTestData();
		clientB.deleteTestData();
	}

	@Test
	public void testChangeTypeToFolder() throws Exception {		
		final TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		final TestClient clientA = new TestClient("A", testConnection);
		final TestClient clientB = new TestClient("B", testConnection);
		
		ClientActions.run(clientA, null,
			new AbstractClientAction[] {
				new CreateFileTree(),
				new ChangeTypeFileToFolder(),
			},
			new Executable() {
				@Override
				public void execute() throws Exception {
					clientA.upWithForceChecksum();		
					
					clientB.down();
					assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
					assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());					
				}			
			}
		);
		
		clientA.deleteTestData();
		clientB.deleteTestData();
	}
}
