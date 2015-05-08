/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
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
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class SqlRunner {
	private static final Logger logger = Logger.getLogger(SqlRunner.class.getSimpleName());

	private static final String DEFAULT_DELIMITER = ";";
	private static final Pattern NEW_DELIMITER_PATTERN = Pattern.compile("(?:--|\\/\\/|\\#)?!DELIMITER=(.+)");
	private static final Pattern COMMENT_PATTERN = Pattern.compile("^(?:--|\\/\\/|\\#).+");

	public static void runScript(Connection connection, InputStream scriptInputStream) throws SQLException, IOException {
		try (BufferedReader scriptReader = new BufferedReader(new InputStreamReader(scriptInputStream))) {
			StringBuffer command = null;
			String delimiter = DEFAULT_DELIMITER;
			String line = null;

			while ((line = scriptReader.readLine()) != null) {
				if (command == null) {
					command = new StringBuffer();
				}

				String trimmedLine = line.trim();

				Matcher delimiterMatcher = NEW_DELIMITER_PATTERN.matcher(trimmedLine);
				Matcher commentMatcher = COMMENT_PATTERN.matcher(trimmedLine);

				// a) Delimiter change
				if (delimiterMatcher.find()) {
					delimiter = delimiterMatcher.group(1);
					logger.log(Level.INFO, "SQL (new delimiter): " + delimiter);
				}

				// b) Comment
				else if (commentMatcher.find()) {
					logger.log(Level.INFO, "SQL (comment): " + trimmedLine);
				}

				// c) Statement
				else {
					command.append(trimmedLine);
					command.append(' ');

					// End of statement
					if (trimmedLine.endsWith(delimiter)) {
						logger.log(Level.INFO, "SQL: " + command);

						Statement statement = connection.createStatement();

						statement.execute(command.toString());
						statement.close();

						command = null;
					}
				}
			}
		}
	}
}
