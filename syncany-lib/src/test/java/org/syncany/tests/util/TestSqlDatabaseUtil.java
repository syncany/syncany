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
	public static void runSqlFromResource(Connection connection, String resourceSqlScript) throws SQLException {
		String fullPathResource = String.format(DatabaseConnectionFactory.DATABASE_RESOURCE_PATTERN, resourceSqlScript);
		InputStream inputStream = DatabaseConnectionFactory.class.getResourceAsStream(fullPathResource);

		if (inputStream == null) {
			throw new RuntimeException("Unable to find script: " + fullPathResource);
		}

		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		new SqlRunner(connection).runScript(reader);
		connection.commit();
	}
}
