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
package org.syncany.tests.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.syncany.database.DatabaseConnectionFactory;
import org.syncany.util.SqlRunner;

public class TestSqlUtil {
	public static void runSqlFromResource(Connection connection, String resourceSqlScript) throws SQLException, IOException {
		String fullPathResource = String.format(DatabaseConnectionFactory.DATABASE_RESOURCE_PATTERN, resourceSqlScript);
		InputStream inputStream = DatabaseConnectionFactory.class.getResourceAsStream(fullPathResource);

		if (inputStream == null) {
			throw new RuntimeException("Unable to find script: " + fullPathResource);
		}

		SqlRunner.runScript(connection, inputStream);
		connection.commit();
	}

	public static String runSqlSelect(String sqlQuery, Connection databaseConnection) throws SQLException {
		StringBuilder queryResult = new StringBuilder();

		try (PreparedStatement preparedStatement = databaseConnection.prepareStatement(sqlQuery)) {
			try (ResultSet actualResultSet = preparedStatement.executeQuery()) {
				ResultSetMetaData metaData = actualResultSet.getMetaData();

				boolean isFirstRow = true;
				int columnsCount = metaData.getColumnCount();

				while (actualResultSet.next()) {
					if (!isFirstRow) {
						queryResult.append('\n');
					}
					else {
						isFirstRow = false;
					}

					for (int i = 1; i <= columnsCount; i++) {
						queryResult.append(actualResultSet.getString(i));

						if (i != columnsCount) {
							queryResult.append(',');
						}
					}
				}
			}
		}

		return queryResult.toString();
	}
	
	public static boolean runSql(String sqlQuery, Connection databaseConnection) throws SQLException {
		try (PreparedStatement preparedStatement = databaseConnection.prepareStatement(sqlQuery)) {
			return preparedStatement.execute();	
		}
	}
}
