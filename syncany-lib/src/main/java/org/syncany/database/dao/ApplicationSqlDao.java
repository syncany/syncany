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
package org.syncany.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.database.VectorClock;
import org.syncany.plugins.transfer.files.DatabaseRemoteFile;

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
		if (remoteDatabases.size() == 0) {
			// Nothing to write
			return;
		}
		PreparedStatement preparedStatement = getStatement("application.insert.all.persistNewKnownRemoteDatabases.sql");

		for (DatabaseRemoteFile databaseRemoteFile : remoteDatabases) {
			preparedStatement.setString(1, databaseRemoteFile.getClientName());
			preparedStatement.setInt(2, (int) databaseRemoteFile.getClientVersion());
			
			preparedStatement.addBatch();
		}

		preparedStatement.executeBatch();
	}

	public VectorClock getHighestKnownDatabaseFilenameNumbers() {
		VectorClock highestKnownDatabaseFilenameNumbers = new VectorClock();

		try (PreparedStatement preparedStatement = getStatement("application.select.all.getHighestKnownDatabaseFilenameNumbers.sql")) {
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					String clientName = resultSet.getString("client");
					int fileNumber = resultSet.getInt("filenumber");

					highestKnownDatabaseFilenameNumbers.put(clientName, (long) fileNumber);
				}

				return highestKnownDatabaseFilenameNumbers;
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
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
					String clientName = resultSet.getString("client");
					int fileNumber = resultSet.getInt("filenumber");
					
					knownDatabases.add(new DatabaseRemoteFile(clientName, fileNumber));
				}

				return knownDatabases;
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void removeKnownDatabases() {
		try (PreparedStatement preparedStatement = getStatement("application.delete.all.removeKnownDatabases.sql")) {
			preparedStatement.execute();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Deletes all metadata, including known databases.
	 */
	public void deleteAll() {
		try {
			runScript("script.delete.all.sql");
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
			logger.log(Level.FINE, "Could not shutdown the connection", e);
		}
		finally {
			try {
				connection.close();
			}
			catch (SQLException e) {
				logger.log(Level.FINE, "Could not close the connection", e);
			}
		}
	}

	public Long getCleanupNumber() {
		return readSettingAsLong("cleanupNumber");
	}

	public Long getCleanupTime() {
		return readSettingAsLong("cleanupTime");
	}

	public void writeCleanupNumber(long cleanupNumber) {
		writeSetting("cleanupNumber", "" + cleanupNumber);
	}

	public void writeCleanupTime(long cleanupTime) {
		writeSetting("cleanupTime", "" + cleanupTime);
	}

	public Long readSettingAsLong(String key) {
		try {
			String strValue = readSetting(key);

			if (strValue != null) {
				return Long.parseLong(strValue);
			}
			else {
				return null;
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String readSetting(String key) {
		try (PreparedStatement preparedStatement = getStatement("application.select.all.readGeneralSettings.sql")) {
			preparedStatement.setString(1, key);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					String value = resultSet.getString("value");
					logger.log(Level.INFO, "SQL (general_settings): Read " + key + "  = " + value);

					return value;
				}
				else {
					logger.log(Level.INFO, "SQL (general_settings): Read " + key + "  = (not set)");
					return null;
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void writeSetting(String key, String value) {
		logger.log(Level.INFO, "SQL (general_settings): Writing " + key + "  = " + value);

		try (PreparedStatement preparedStatement = getStatement("application.insert.all.writeGeneralSettings.sql")) {
			preparedStatement.setString(1, key);
			preparedStatement.setString(2, value);

			preparedStatement.execute();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
