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
package org.syncany.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.syncany.database.DatabaseVersion.DatabaseVersionStatus;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.VectorClock;

/**
 * The file history DAO queries and modifies the <i>filehistory</i> in
 * the SQL database. This table corresponds to the Java object {@link PartialFileHistory}.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FileHistorySqlDao extends AbstractSqlDao {
	protected static final Logger logger = Logger.getLogger(FileHistorySqlDao.class.getSimpleName());

	private FileVersionSqlDao fileVersionDao;	
	
	public FileHistorySqlDao(Connection connection, FileVersionSqlDao fileVersionDao) {
		super(connection);		
		this.fileVersionDao = fileVersionDao;		
	}

	/**
	 * Writes a list of {@link PartialFileHistory}s to the database table <i>filehistory</i> using <tt>INSERT</tt>s
	 * and the given connection. In addition, this method also writes the corresponding {@link FileVersion}s of 
	 * each file history to the database using 
	 * {@link FileVersionSqlDao#writeFileVersions(Connection, FileHistoryId, long, Collection) FileVersionSqlDao#writeFileVersions}.
	 * 
	 * <p><b>Note:</b> This method executes, but <b>does not commit</b> the queries.
	 * 
	 * @param connection The connection used to execute the statements
	 * @param databaseVersionId References the {@link PartialFileHistory} to which the list of file versions belongs
	 * @param fileHistories List of {@link PartialFileHistory}s to be written to the database
	 * @throws SQLException If the SQL statement fails
	 */
	public void writeFileHistories(Connection connection, long databaseVersionId, Collection<PartialFileHistory> fileHistories) throws SQLException {
		for (PartialFileHistory fileHistory : fileHistories) {
			PreparedStatement preparedStatement = getStatement(connection, "filehistory.insert.all.writeFileHistories.sql");

			preparedStatement.setString(1, fileHistory.getFileHistoryId().toString());
			preparedStatement.setLong(2, databaseVersionId);

			int affectedRows = preparedStatement.executeUpdate();
			
			if (affectedRows == 0) {
				throw new SQLException("Cannot add database version header. Affected rows is zero.");
			}

			preparedStatement.close();
			
			fileVersionDao.writeFileVersions(connection, fileHistory.getFileHistoryId(), databaseVersionId, fileHistory.getFileVersions().values());
		}
	}

	
	public void removeDirtyFileHistories() throws SQLException {
		try (PreparedStatement preparedStatement = getStatement("filehistory.delete.dirty.removeDirtyFileHistories.sql")) {
			preparedStatement.executeUpdate();
		}		
	}
	
	/**
	 * Removes unreferenced {@link PartialFileHistory}s from the database table 
	 * <i>filehistory</i>. This method <b>does not</b> remove the corresponding {@link FileVersion}s.
	 * 
	 * <p><b>Note:</b> This method executes, but <b>does not commit</b> the query.
	 * 
	 * @throws SQLException If the SQL statement fails
	 */	
	public void removeUnreferencedFileHistories() throws SQLException {
		try (PreparedStatement preparedStatement = getStatement("filehistory.delete.all.removeUnreferencedFileHistories.sql")) {
			preparedStatement.executeUpdate();
		}	
	}

	/**
	 * Note: Also selects versions marked as {@link DatabaseVersionStatus#DIRTY DIRTY}
	 */
	public List<PartialFileHistory> getFileHistoriesWithFileVersions(VectorClock databaseVersionVectorClock) {
		try (PreparedStatement preparedStatement = getStatement("filehistory.select.all.getFileHistoriesWithFileVersionsByVectorClock.sql")) {
			preparedStatement.setString(1, databaseVersionVectorClock.toString());

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				return createFileHistoriesFromResult(resultSet);
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public List<PartialFileHistory> getFileHistoriesWithFileVersions() {
		try (PreparedStatement preparedStatement = getStatement("filehistory.select.master.getFileHistoriesWithFileVersions.sql")) {
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				return createFileHistoriesFromResult(resultSet);
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	protected List<PartialFileHistory> createFileHistoriesFromResult(ResultSet resultSet) throws SQLException {
		List<PartialFileHistory> fileHistories = new ArrayList<PartialFileHistory>();;
		PartialFileHistory fileHistory = null;

		while (resultSet.next()) {
			FileVersion lastFileVersion = fileVersionDao.createFileVersionFromRow(resultSet);
			FileHistoryId fileHistoryId = FileHistoryId.parseFileId(resultSet.getString("filehistory_id"));

			// Old history (= same filehistory identifier)
			if (fileHistory != null && fileHistory.getFileHistoryId().equals(fileHistoryId)) { // Same history!
				fileHistory.addFileVersion(lastFileVersion);
			}
			
			// New history!			
			else { 
				// Add the old history
				if (fileHistory != null) { 
					fileHistories.add(fileHistory);
				}
				
				// Create a new one
				fileHistory = new PartialFileHistory(fileHistoryId);
				fileHistory.addFileVersion(lastFileVersion);
			}			
		}
		
		// Add the last history
		if (fileHistory != null) { 
			fileHistories.add(fileHistory);
		}

		return fileHistories;
	}

	public List<PartialFileHistory> getFileHistoriesWithLastVersion() {
		List<PartialFileHistory> fileHistories = new ArrayList<PartialFileHistory>();

		try (PreparedStatement preparedStatement = getStatement("filehistory.select.master.getFileHistoriesWithLastVersion.sql")) {
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					FileHistoryId fileHistoryId = FileHistoryId.parseFileId(resultSet.getString("filehistory_id"));
					FileVersion lastFileVersion = fileVersionDao.createFileVersionFromRow(resultSet);
	
					PartialFileHistory fileHistory = new PartialFileHistory(fileHistoryId);
					fileHistory.addFileVersion(lastFileVersion);
	
					fileHistories.add(fileHistory);
				}
			}

			return fileHistories;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public List<PartialFileHistory> getFileHistoriesWithLastVersionByChecksum(FileChecksum fileContentChecksum) {
		List<PartialFileHistory> currentFileTree = new ArrayList<PartialFileHistory>();

		try (PreparedStatement preparedStatement = getStatement("filehistory.select.master.getFileHistoriesWithLastVersionByChecksum.sql")) {
			preparedStatement.setString(1, fileContentChecksum.toString());

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					FileHistoryId fileHistoryId = FileHistoryId.parseFileId(resultSet.getString("filehistory_id"));
					FileVersion lastFileVersion = fileVersionDao.createFileVersionFromRow(resultSet);
	
					PartialFileHistory fileHistory = new PartialFileHistory(fileHistoryId);
					fileHistory.addFileVersion(lastFileVersion);
	
					currentFileTree.add(fileHistory);
				}	
			}

			return currentFileTree;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}	
}
