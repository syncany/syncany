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
package org.syncany.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.syncany.connection.plugins.DatabaseRemoteFile;

/**
 * @author pheckel
 *
 */
public class ApplicationSqlDao extends AbstractSqlDao {
	protected static final Logger logger = Logger.getLogger(ApplicationSqlDao.class.getSimpleName());	
	
	public ApplicationSqlDao(Connection connection) {
		super(connection);
	}	
	
	public void persistNewKnownRemoteDatabases(List<DatabaseRemoteFile> remoteDatabases) throws SQLException {
		PreparedStatement preparedStatement = getStatement("/sql/insert.persistNewKnownRemoteDatabases.sql");

		for (DatabaseRemoteFile databaseRemoteFile : remoteDatabases) {
			preparedStatement.setString(1, databaseRemoteFile.getName());
			preparedStatement.addBatch();
		}
		
		preparedStatement.executeBatch();
		connection.commit();
	}
	
	public List<String> getKnownDatabases() {
		List<String> knownDatabases = new ArrayList<String>();
		
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getKnownDatabases.sql");					
			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				knownDatabases.add(resultSet.getString("database_name"));
			}

			return knownDatabases;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void shutdown() {
		try {
			connection.prepareStatement("shutdown").execute();
		}
		catch (SQLException e) {
			// Don't care
		}
		finally {
			try {
				connection.close();
			}
			catch (SQLException e) {
				// Don't care
			}
		}
	}
}
