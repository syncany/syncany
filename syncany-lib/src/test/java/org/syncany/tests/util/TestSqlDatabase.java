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
package org.syncany.tests.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.syncany.config.Config;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.SqlDatabase;
import org.syncany.database.dao.AbstractSqlDao;

public class TestSqlDatabase extends SqlDatabase {
	private TestSqlDao testDao;
	
	public TestSqlDatabase(Config config) {
		super(config);
		this.testDao = new TestSqlDao(config.createDatabaseConnection());
	}
	
	public PartialFileHistory getFileHistoryWithFileVersions(String relativePath) {
		return testDao.getFileHistoryWithFileVersions(relativePath);
	}
	
	public PartialFileHistory getFileHistoryWithLastVersion(String relativePath) {
		return testDao.getFileHistoryWithLastVersion(relativePath);
	}

	public class TestSqlDao extends AbstractSqlDao {		
		public TestSqlDao(Connection connection) {
			super(connection);
		}

		public PartialFileHistory getFileHistoryWithFileVersions(String relativePath) {
			try {
				PreparedStatement preparedStatement = getStatement("filehistory.select.master.getFileHistoryWithFileVersions.sql");
	
				preparedStatement.setString(1, relativePath);
				preparedStatement.setString(2, FileStatus.DELETED.toString());
	
				ResultSet resultSet = preparedStatement.executeQuery();
	
				PartialFileHistory fileHistory = null;
	
				while (resultSet.next()) {
					if (fileHistory == null) {
						FileHistoryId fileHistoryId = FileHistoryId.parseFileId(resultSet.getString("filehistory_id"));
						fileHistory = new PartialFileHistory(fileHistoryId);
					}
	
					FileVersion fileVersion = fileVersionDao.createFileVersionFromRow(resultSet);
					fileHistory.addFileVersion(fileVersion);
				}
	
				return fileHistory;
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		public PartialFileHistory getFileHistoryWithLastVersion(String relativePath) {
			try (PreparedStatement preparedStatement = getStatement("filehistory.select.master.getFileHistoryWithLastVersion.sql")) {
				preparedStatement.setString(1, relativePath);

				try (ResultSet resultSet = preparedStatement.executeQuery()) {
					if (resultSet.next()) {
						FileHistoryId fileHistoryId = FileHistoryId.parseFileId(resultSet.getString("filehistory_id"));
						FileVersion lastFileVersion = fileVersionDao.createFileVersionFromRow(resultSet);
		
						PartialFileHistory fileHistory = new PartialFileHistory(fileHistoryId);
						fileHistory.addFileVersion(lastFileVersion);
		
						return fileHistory;
					}
				}

				return null;
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
