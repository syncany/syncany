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

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.syncany.database.DatabaseConnectionFactory;
import org.syncany.util.SqlRunner;

/**
 * Small helper class to implement common tasks for the inheriting 
 * SQL data access objects.
 * 
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
public abstract class AbstractSqlDao {
	protected Connection connection;
	
	public AbstractSqlDao(Connection connection) {
		this.connection = connection;
	}
	
	public Connection getConnection() {
		return connection;
	}
	
	protected PreparedStatement getStatement(String resourceId) throws SQLException {
		return getStatement(connection, resourceId);
	}
	
	protected PreparedStatement getStatement(Connection overrideConnection, String resourceId) throws SQLException {
		return overrideConnection.prepareStatement(DatabaseConnectionFactory.getStatement(resourceId));
	}
	
	protected void runScript(String resourceId) throws SQLException, IOException {
		SqlRunner.runScript(connection, DatabaseConnectionFactory.getStatementInputStream(resourceId));
	}
}
