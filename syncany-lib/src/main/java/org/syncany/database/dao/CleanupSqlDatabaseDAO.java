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
import java.sql.SQLException;

/**
 * @author pheckel
 *
 */
public class CleanupSqlDatabaseDAO extends SqlDatabaseDAO {
	public CleanupSqlDatabaseDAO(Connection connection) {
		super(connection);
	}

	public void removeDirtyDatabaseVersions() {
		try {
			// The order is important, because of the database foreign key consistencies
			
			removeDirtyChunks();
			removeDirtyMultiChunks();
			removeDirtyFileVersions();
			removeDirtyFileContents();
			removeDirtyFileHistories();
			removeDirtyVectorClocks();
			removeDirtyDatabaseVersionsInt();
	
			connection.commit();
		}
		catch (SQLException e) {
			throw new RuntimeException("Unable to remove dirty database versions.", e);
		}
	}

	private void removeDirtyVectorClocks() throws SQLException {
		PreparedStatement preparedStatement = getStatement("/sql/delete.removeDirtyVectorClocks.sql");
		preparedStatement.executeUpdate();		
	}
	
	private void removeDirtyDatabaseVersionsInt() throws SQLException {
		PreparedStatement preparedStatement = getStatement("/sql/delete.removeDirtyDatabaseVersionsInt.sql");
		preparedStatement.executeUpdate();		
	}

	private void removeDirtyFileHistories() throws SQLException {
		PreparedStatement preparedStatement = getStatement("/sql/delete.removeDirtyFileHistories.sql");
		preparedStatement.executeUpdate();
	}

	private void removeDirtyFileVersions() throws SQLException {
		PreparedStatement preparedStatement = getStatement("/sql/delete.removeDirtyFileVersions.sql");
		preparedStatement.executeUpdate();		
	}

	private void removeDirtyFileContents() {
		// TODO Auto-generated method stub

	}

	private void removeDirtyChunks() throws SQLException {
		PreparedStatement preparedStatement = getStatement("/sql/delete.removeDirtyUnreferencedChunks.sql");
		preparedStatement.executeUpdate();
	}

	private void removeDirtyMultiChunks() throws SQLException {
		//PreparedStatement preparedStatement = getStatement("/sql/delete.removeDirtyMultiChunks.sql");
		//preparedStatement.executeUpdate();
	}

}
