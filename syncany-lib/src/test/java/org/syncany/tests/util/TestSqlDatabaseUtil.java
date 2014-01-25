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
package org.syncany.tests.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;

import org.syncany.database.DatabaseConnectionFactory;
import org.syncany.util.SqlRunner;

/**
 * @author pheckel
 *
 */
public class TestSqlDatabaseUtil {
	public static void main(String[] args) throws SQLException {
		if (args.length != 2 || "".equals(args[0]) || "".equals(args[1])) {
			System.err.println();
			System.err.println("-------------------------------------------------------------------------");
			System.err.println("Syntax: gradle runSql -Pdb=DBPATH -Psql=SCRIPTRESOURCE");
			System.err.println("   e.g. gradle runSql -Pdb=/tmp/a/test.db -Psql=/sql/test.insert.set1.sql");
			System.err.println("-------------------------------------------------------------------------");
			System.exit(0);
		}

		File databaseFile = new File(args[0]);
		String sqlScriptResource = args[1];

		System.out.println("Running on '" + databaseFile + " from SQL script '" + sqlScriptResource + "'");

		if (!databaseFile.getParentFile().exists()) {
			databaseFile.getParentFile().mkdirs();
		}

		Connection connection = DatabaseConnectionFactory.createConnection(databaseFile);
		runSqlFromResource(connection, sqlScriptResource);
	}

	public static void runSqlFromResource(Connection connection, String resourceSqlScript) throws SQLException {
		InputStream inputStream = DatabaseConnectionFactory.class.getResourceAsStream(resourceSqlScript);

		if (inputStream == null) {
			throw new RuntimeException("Unable to find script: " + resourceSqlScript);
		}

		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		new SqlRunner(connection).runScript(reader);
		connection.commit();
	}
}
