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
package org.syncany.util;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class to execute SQL scripts on a given connection. The script honors SQL comments and 
 * separately executes commands one after another.
 * 
 * @see Originally taken from <a href="http://allstarnix.blogspot.de/2013/03/how-to-execute-sql-script-file-using.html">allstarnix.blogspot.de</a>
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
// TODO [low] Needs polishing/stripping of useless stuff.
public class SqlRunner {
	private static final Logger logger = Logger.getLogger(SqlRunner.class.getSimpleName());

	public static final String DELIMITER_LINE_REGEX = "(?i)DELIMITER.+";
	public static final String DELIMITER_LINE_SPLIT_REGEX = "(?i)DELIMITER";
	public static final String DELIMITER = ";";
	
	private final Connection connection;
	private String delimiter = SqlRunner.DELIMITER;

	public SqlRunner(Connection connection) {
		this.connection = connection;
	}

	public void runScript(Reader reader) throws SQLException {
		StringBuffer command = null;
		
		try {
			LineNumberReader lineReader = new LineNumberReader(reader);
			String line = null;
			
			while ((line = lineReader.readLine()) != null) {
				if (command == null) {
					command = new StringBuffer();
				}
				
				String trimmedLine = line.trim();

				// a) Comment line
				if (trimmedLine.startsWith("--") || trimmedLine.startsWith("//") || trimmedLine.startsWith("#")) {
					logger.log(Level.INFO, "SQL (comment): " + trimmedLine);
				}
				
				// b) End of statement (ends with ";")
				else if (trimmedLine.endsWith(DELIMITER)) {
					// Support new delimiter
					final Pattern pattern = Pattern.compile(SqlRunner.DELIMITER_LINE_REGEX);
					final Matcher matcher = pattern.matcher(trimmedLine);
					if (matcher.matches()) {
						delimiter = trimmedLine.split(SqlRunner.DELIMITER_LINE_SPLIT_REGEX)[1].trim();

						// New delimiter is processed, continue on next
						// statement
						line = lineReader.readLine();
						if (line == null) {
							break;
						}
						trimmedLine = line.trim();
					}

					// Append
					command.append(line.substring(0, line.lastIndexOf(this.delimiter)));
					command.append(" ");

					Statement stmt = null;
					ResultSet rs = null;
					try {
						stmt = connection.createStatement();
						logger.log(Level.INFO, "SQL: " + command);

						stmt.execute(command.toString());

						rs = stmt.getResultSet();

						command = null;
					}
					finally {
						if (rs != null)
							try {
								rs.close();
							}
							catch (final Exception e) {
								logger.log(Level.SEVERE, "SQL ERROR: " + e.getMessage(), e);
							}
						if (stmt != null)
							try {
								stmt.close();
							}
							catch (final Exception e) {
								logger.log(Level.SEVERE, "SQL ERROR: " + e.getMessage(), e);
							}
					}
				}
				
				// c) Not the end of a statement
				else {
					final Pattern pattern = Pattern.compile(SqlRunner.DELIMITER_LINE_REGEX);
					final Matcher matcher = pattern.matcher(trimmedLine);
					if (matcher.matches()) {
						delimiter = trimmedLine.split(SqlRunner.DELIMITER_LINE_SPLIT_REGEX)[1].trim();
						line = lineReader.readLine();
						if (line == null) {
							break;
						}
						trimmedLine = line.trim();
					}
					command.append(line);
					command.append(" ");
				}
			}
		}
		catch (final IOException e) {
			logger.log(Level.SEVERE, "SQL ERROR: " + e.getMessage(), e);
		}
	}
}
