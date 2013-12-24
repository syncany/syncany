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
package org.syncany.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.syncany.database.PartialFileHistory.FileHistoryId;

/**
 * @author pheckel
 *
 */
public class LogDatabaseDAO extends BasicDatabaseDAO {
	public LogDatabaseDAO(Connection connection) {
		super(connection);
	}

	public List<PartialFileHistory> getFileHistoriesWithFileVersions() {
		List<PartialFileHistory> currentFileTree = new ArrayList<PartialFileHistory>();

		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select * from fileversion order by filehistory_id, version");
			ResultSet resultSet = preparedStatement.executeQuery();

			PartialFileHistory fileHistory = null;
			
			while (resultSet.next()) {
				FileVersion lastFileVersion = createFileVersionFromRow(resultSet);
				FileHistoryId fileHistoryId = FileHistoryId.parseFileId(resultSet.getString("filehistory_id"));
				
				if (fileHistory != null && fileHistory.getFileId().equals(fileHistoryId)) { // Same history!
						fileHistory.addFileVersion(lastFileVersion);
				}
				else { // New history!
					fileHistory = new PartialFileHistory(fileHistoryId);
					fileHistory.addFileVersion(lastFileVersion);
				}				
					
				currentFileTree.add(fileHistory);
			}

			return currentFileTree;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

}
