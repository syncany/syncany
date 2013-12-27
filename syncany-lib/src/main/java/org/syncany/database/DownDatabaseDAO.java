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
import java.util.Date;

import org.syncany.operations.DatabaseBranch;

/**
 * @author pheckel
 *
 */
public class DownDatabaseDAO extends BasicDatabaseDAO {
	public DownDatabaseDAO(Connection connection) {
		super(connection);
	}
	
	public DatabaseBranch getLocalDatabaseBranch() {
		DatabaseBranch databaseBranch = new DatabaseBranch();
		
		try {
			PreparedStatement preparedStatement = connection.prepareStatement(
				  "select dbv.id, dbv.localtime, dbv.client, vc.client as vc_client, vc.logicaltime as vc_logicaltime "
				+ "from databaseversion dbv "
				+ "join vectorclock vc on vc.databaseversion_id=dbv.id "
				+ "order by dbv.id asc, vc.client");
					
			ResultSet resultSet = preparedStatement.executeQuery();
			
			DatabaseVersionHeader currentDatabaseVersionHeader = null;
			int currentDatabaseVersionHeaderId = -1;
			
			while (resultSet.next()) {
				int databaseVersionHeaderId = resultSet.getInt("id");

				// Row does NOT belong to the current database version 
				if (currentDatabaseVersionHeader == null || currentDatabaseVersionHeaderId != databaseVersionHeaderId) {
					// Add to database branch
					if (currentDatabaseVersionHeader != null) {
						databaseBranch.add(currentDatabaseVersionHeader);
					}
					
					// Make a new database version header
					currentDatabaseVersionHeader = new DatabaseVersionHeader();					
					currentDatabaseVersionHeader.setClient(resultSet.getString("client"));
					currentDatabaseVersionHeader.setDate(new Date(resultSet.getDate("localtime").getTime()));					

					currentDatabaseVersionHeaderId = databaseVersionHeaderId;
				}
				
				currentDatabaseVersionHeader.getVectorClock().setClock(resultSet.getString("vc_client"), resultSet.getLong("vc_logicaltime"));						
			}
			
			// Add to database branch
			if (currentDatabaseVersionHeader != null) {
				databaseBranch.add(currentDatabaseVersionHeader);
			}

			return databaseBranch;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}	
}
