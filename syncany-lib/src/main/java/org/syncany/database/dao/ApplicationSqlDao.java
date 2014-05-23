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
 * The application data access object (DAO) writes and queries the SQL database for 
 * general information about the application. 
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class ApplicationSqlDao extends AbstractSqlDao {
	protected static final Logger logger = Logger.getLogger(ApplicationSqlDao.class.getSimpleName());	
	
	public ApplicationSqlDao(Connection connection) {
		super(connection);
	}	
	
	/**
	 * Writes a list of {@link DatabaseRemoteFile}s to the database using the given connection.
	 * <p><b>Note:</b> This method executes, but does not commit the query.
	 * 
	 * @param remoteDatabases List of remote databases to write to the database
	 * @throws SQLException If the SQL statement fails
	 */
	public void writeKnownRemoteDatabases(List<DatabaseRemoteFile> remoteDatabases) throws SQLException {
		PreparedStatement preparedStatement = getStatement("application.insert.all.persistNewKnownRemoteDatabases.sql");

		for (DatabaseRemoteFile databaseRemoteFile : remoteDatabases) {
			preparedStatement.setString(1, databaseRemoteFile.getName());
			preparedStatement.addBatch();
		}
		
		preparedStatement.executeBatch();
		connection.commit();
		preparedStatement.close();
	}
	
	/**
	 * Queries the database for already known {@link DatabaseRemoteFile}s and returns a
	 * list of all of them. 
	 * 
	 * @return Returns a list of all known/processed remote databases
	 */
	public List<DatabaseRemoteFile> getKnownDatabases() {
		List<DatabaseRemoteFile> knownDatabases = new ArrayList<DatabaseRemoteFile>();
				
		try (PreparedStatement preparedStatement = getStatement("application.select.all.getKnownDatabases.sql")) {
			try (ResultSet resultSet = preparedStatement.executeQuery()) {		
				while (resultSet.next()) {
					knownDatabases.add(new DatabaseRemoteFile(resultSet.getString("database_name")));
				}
				
				return knownDatabases;
			}  
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Shuts down the HSQL database, i.e. persists all data, closes all connections
	 * and unlocks the database for other processes. 
	 * 
	 * <p>The command sends the <b><tt>SHUTDOWN</tt></b> SQL command.
	 */
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
