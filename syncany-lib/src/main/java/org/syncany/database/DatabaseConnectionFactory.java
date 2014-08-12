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
package org.syncany.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.util.FileUtil;
import org.syncany.util.SqlRunner;

/**
 * This class is a helper class that provides the connection to the embedded 
 * HSQLDB database. It is mainly used by the data access objects.
 * 
 * <p>The class provides methods to create {@link Connection} objects, retrieve
 * SQL statements from the resources, and create the initial tables when the 
 * application is first started.   
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class DatabaseConnectionFactory {
	private static final Logger logger = Logger.getLogger(DatabaseConnectionFactory.class.getSimpleName());
	
	public static final String DATABASE_DRIVER = "org.hsqldb.jdbcDriver";
	public static final String DATABASE_CONNECTION_FILE_STRING = "jdbc:hsqldb:file:%DATABASEFILE%;user=sa;password=;create=true;write_delay=false;hsqldb.write_delay=false;shutdown=true";
	public static final String DATABASE_RESOURCE_PATTERN = "/org/syncany/database/sql/%s";
	public static final String DATABASE_RESOURCE_CREATE_ALL = "create.all.sql";	
	
	public static final Map<String, String> DATABASE_STATEMENTS = new HashMap<String, String>(); 
	
	static {
		try {
			logger.log(Level.INFO, "Loading database driver "+DATABASE_DRIVER+" ...");
			Class.forName(DATABASE_DRIVER);
		}
		catch (Exception e) {
			throw new RuntimeException("Cannot load database driver: "+DATABASE_DRIVER, e);
		}
	}

	/**
	 * Creates a database connection using the given database file. If the database exists and the
	 * application tables are present, a valid connection is returned. If not, the database is created
	 * and the application tables are created.
	 * 
	 * @param databaseFile File at which to create/load the database
	 * @return Returns a valid database connection 
	 */
	public static Connection createConnection(File databaseFile) {
		String databaseFilePath = FileUtil.getDatabasePath(databaseFile.toString());
		String connectionString = DATABASE_CONNECTION_FILE_STRING.replaceAll("%DATABASEFILE%", databaseFilePath);
		
		if (logger.isLoggable(Level.FINEST)) {
			connectionString += ";hsqldb.sqllog=3";
		}
		
		return createConnection(connectionString);
	}
	
	/**
	 * Retrieves a SQL statement template from a resource using the given resource identifier. From
	 * this template, a {@link PreparedStatement} can be created.
	 * 
	 * <p>The statement is either loaded from the resource (if it is first encountered),
	 * or loaded from the cache if it has been seen before.
	 * 
	 * @param resourceIdentifier Path to the resource, e.g. "create.all.sql"
	 * @return Returns the SQL statement read from the resource
	 */
	public synchronized static String getStatement(String resourceIdentifier) {
		String fullResourcePath = String.format(DATABASE_RESOURCE_PATTERN, resourceIdentifier);
		String preparedStatement = DATABASE_STATEMENTS.get(fullResourcePath);
		
		if (preparedStatement != null) {
			return preparedStatement;
		}
		else {
			InputStream statementInputStream = DatabaseConnectionFactory.class.getResourceAsStream(fullResourcePath);
			
			if (statementInputStream == null) {
				throw new RuntimeException("Unable to load SQL statement '" + fullResourcePath + "'.");
			}
			
			preparedStatement = readDatabaseStatement(statementInputStream);			
			DATABASE_STATEMENTS.put(fullResourcePath, preparedStatement);			
			
			return preparedStatement;
		}		
	}
	
	private static Connection createConnection(String connectionString) {
		try {
			Connection connection = DriverManager.getConnection(connectionString);			
			connection.setAutoCommit(false);
			
			// Test and create tables
			if (!tablesExist(connection)) {
				createTables(connection);
			}
			
			return connection;
		}
		catch (Exception e) {
			if (e.getCause() != null) {
				if (e.getCause().getMessage().contains("Database lock acquisition failure")) {
					throw new RuntimeException("Another process is using the database.", e);
				}
			}
			throw new RuntimeException("Cannot create new connection; database down?", e);
		}
	} 

	private static boolean tablesExist(Connection connection) {
		try {
			ResultSet resultSet = connection.prepareStatement("select count(*) from chunk").executeQuery();
			
			if (resultSet.next()) {
				return true;
			}
			else {
				return false;
			}
		}
		catch (SQLException e) {
			return false;
		}
	}
	
	private static void createTables(Connection connection) throws SQLException, IOException {
		logger.log(Level.INFO, "Database has no tables. Creating tables from "+DATABASE_RESOURCE_CREATE_ALL);
		
		String fullResourcePath = String.format(DATABASE_RESOURCE_PATTERN, DATABASE_RESOURCE_CREATE_ALL);
		InputStream inputStream = DatabaseConnectionFactory.class.getResourceAsStream(fullResourcePath);
		
		connection.setAutoCommit(true);
		
		SqlRunner.runScript(connection, inputStream);
		 
		connection.setAutoCommit(false);
	}
	
	// TODO [low] Shouldn't the SqlRunner be used here? If so, the SqlRunner also needs refactoring.
	private static String readDatabaseStatement(InputStream inputStream) {
		try {
			StringBuilder preparedStatementStr = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

			String line = null;
			
			while (null != (line = reader.readLine())) {
				String trimmedLine = line.trim();
				
				if (!trimmedLine.startsWith("--")) {
					preparedStatementStr.append(" ");
					preparedStatementStr.append(trimmedLine);
				}
			}
			
			reader.close();
			inputStream.close();
			
			return preparedStatementStr.toString();
		}
		catch (IOException e) {
			throw new RuntimeException("Unable to read SQL statement from resource.", e);
		}		
	}
}
