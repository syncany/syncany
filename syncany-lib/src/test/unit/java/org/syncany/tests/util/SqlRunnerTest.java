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
package org.syncany.tests.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.tests.unit.util.TestFileUtil;
import org.syncany.util.SqlRunner;

public class SqlRunnerTest {
	@BeforeClass
	public static void loadDriver() throws ClassNotFoundException {
		Class.forName("org.hsqldb.jdbcDriver");
	}

	@Test
	public void testRunScript() throws Exception {
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		File tempDatabaseFile = new File(tempDir, "db");

		InputStream inputStream = SqlRunnerTest.class.getResourceAsStream("/org/syncany/database/sql/script.create.all.sql");

		String connectionString = "jdbc:hsqldb:file:" + tempDatabaseFile.getAbsolutePath()
				+ ";user=sa;password=;create=true;write_delay=false;hsqldb.write_delay=false;shutdown=true";
		Connection connection = DriverManager.getConnection(connectionString);

		SqlRunner.runScript(connection, inputStream);

		connection.createStatement().execute("INSERT INTO DATABASEVERSION VALUES(1337,'MASTER',TIMESTAMP_WITH_ZONE(1388589969),'A','(A1)')");
		connection.commit();

		// Test a few selects
		assertEquals("1337", TestSqlUtil.runSqlSelect("select id from databaseversion", connection));
		assertEquals(
				"DATABASEVERSION\nCHUNK\nDATABASEVERSION_VECTORCLOCK\nFILECONTENT\nFILECONTENT_CHUNK\nFILEHISTORY\nFILEVERSION\nMULTICHUNK\nMULTICHUNK_CHUNK\nMULTICHUNK_MUDDY\nKNOWN_DATABASES\nGENERAL_SETTINGS\nDATABASEVERSION_MASTER\nFILEVERSION_MASTER\nFILEVERSION_MASTER_MAXVERSION\nFILEVERSION_MASTER_LAST\nFILEHISTORY_FULL\nFILEVERSION_FULL",
				TestSqlUtil.runSqlSelect("select table_name from information_schema.tables where table_schema='PUBLIC'", connection));

		// Test the function (--> different delimiter!)
		assertEquals("3", TestSqlUtil.runSqlSelect("select distinct substr_count('/a/b/c', '/') from information_schema.system_tables", connection));

		connection.createStatement().execute("shutdown");

		TestFileUtil.deleteDirectory(tempDir);
	}
}
