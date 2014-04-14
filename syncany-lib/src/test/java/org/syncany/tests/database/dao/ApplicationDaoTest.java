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
package org.syncany.tests.database.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.database.dao.ApplicationSqlDao;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestSqlDatabaseUtil;
import org.syncany.util.CollectionUtil;

public class ApplicationDaoTest {
	@Test
	public void testGetKnownDatabases() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlDatabaseUtil.runSqlFromResource(databaseConnection, "/sql/test.insert.set3.sql");
		
		ApplicationSqlDao applicationDao = new ApplicationSqlDao(databaseConnection);
		List<DatabaseRemoteFile> actualKnownDatabases = applicationDao.getKnownDatabases();
		
		// Test
		assertTrue(CollectionUtil.containsExactly(Arrays.asList(new DatabaseRemoteFile[] { 
			new DatabaseRemoteFile("db-B-0000000001"),
			new DatabaseRemoteFile("db-B-0000000002"),
			new DatabaseRemoteFile("db-B-0000000003")
		}), actualKnownDatabases));

		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testPersistAndGetKnownDatabases() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		ApplicationSqlDao applicationDao = new ApplicationSqlDao(databaseConnection);
		
		List<DatabaseRemoteFile> expectedKnownDatabases = Arrays.asList(new DatabaseRemoteFile[] { 
			new DatabaseRemoteFile("db-A-0000000001"),
			new DatabaseRemoteFile("db-V-0000000001"),
			new DatabaseRemoteFile("db-B-0000000001"),
			new DatabaseRemoteFile("db-A-0000000002")
		});
		
		applicationDao.writeKnownRemoteDatabases(expectedKnownDatabases);
		List<DatabaseRemoteFile> actualKnownDatabases = applicationDao.getKnownDatabases();
		
		// Test
		assertTrue(CollectionUtil.containsExactly(expectedKnownDatabases, actualKnownDatabases));

		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testShutdown() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Prepare
		ApplicationSqlDao applicationDao = new ApplicationSqlDao(databaseConnection);
		
		List<DatabaseRemoteFile> expectedKnownDatabases = Arrays.asList(new DatabaseRemoteFile[] { 
			new DatabaseRemoteFile("db-A-0000000001")
		});
		
		applicationDao.writeKnownRemoteDatabases(expectedKnownDatabases);

		// Run & Test
		assertTrue(new File(testConfig.getDatabaseFile()+".lck").exists());
		applicationDao.shutdown();
		assertFalse(new File(testConfig.getDatabaseFile()+".lck").exists());		

		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
}
