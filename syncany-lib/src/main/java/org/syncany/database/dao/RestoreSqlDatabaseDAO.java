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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.syncany.database.FileVersion;

/**
 * @author pheckel
 *
 */
public class RestoreSqlDatabaseDAO extends SqlDatabaseDAO {	
	public RestoreSqlDatabaseDAO(Connection connection) {
		super(connection);		
	}

	public Map<String, FileVersion> getFileTreeAtDate(Date date) {
		Map<String, FileVersion> fileTreeAtDate = new HashMap<String, FileVersion>();

		try {
			PreparedStatement preparedStatement = connection.prepareStatement(
				  "select fv.* "
				+ "from databaseversion dbv "
				+ "join filehistory fh on dbv.id=fh.databaseversion_id "
				+ "join fileversion fv on fh.id=fv.filehistory_id "
				+ "where dbv.localtime<=? "
				+ "  and fv.status<>'DELETED' "
				+ "  and fv.version=(select max(fv1.version) from fileversion fv1 where fv.filehistory_id=fv1.filehistory_id)");

			preparedStatement.setDate(1, new java.sql.Date(date.getTime()));
			
			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				FileVersion fileVersion = createFileVersionFromRow(resultSet);
				fileTreeAtDate.put(fileVersion.getPath(), fileVersion);
			}

			return fileTreeAtDate;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
