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
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.database.ChunkEntry;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.DatabaseConnectionFactory;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersion.DatabaseVersionStatus;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.FileContent;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.VectorClock;
import org.syncany.operations.DatabaseBranch;

/**
 * @author pheckel
 *
 */
public class DatabaseVersionSqlDao extends AbstractSqlDao {
	protected static final Logger logger = Logger.getLogger(DatabaseVersionSqlDao.class.getSimpleName());

	private ChunkSqlDao chunkDao;
	private FileContentSqlDao fileContentDao;
	private FileHistorySqlDao fileHistoryDao;
	private MultiChunkSqlDao multiChunkDao;

	public DatabaseVersionSqlDao(Connection connection, ChunkSqlDao chunkDao, FileContentSqlDao fileContentDao, FileHistorySqlDao fileHistoryDao,
			MultiChunkSqlDao multiChunkDao) {
		
		super(connection);

		this.chunkDao = chunkDao;
		this.fileContentDao = fileContentDao;
		this.fileHistoryDao = fileHistoryDao;
		this.multiChunkDao = multiChunkDao;
	}

	public void markDatabaseVersionDirty(VectorClock vectorClock) {
		try (PreparedStatement preparedStatement = getStatement("/sql/databaseversion.update.master.markDatabaseVersionDirty.sql")){
			preparedStatement.setString(1, DatabaseVersionStatus.DIRTY.toString());
			preparedStatement.setString(2, vectorClock.toString());

			preparedStatement.executeUpdate();
			connection.commit();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void persistDatabaseVersion(DatabaseVersion databaseVersion) {
		try {
			// Insert & commit database version
			writeDatabaseVersion(connection, databaseVersion);
			connection.commit();
			
			// Clear local caches
			chunkDao.clearCache();
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "SQL Error: ", e);
			throw new RuntimeException("Cannot persist database.", e);
		}
	}

	private void writeDatabaseVersion(Connection connection, DatabaseVersion databaseVersion) throws SQLException {
		long databaseVersionId = -1;
		
		try(PreparedStatement preparedStatement = connection.prepareStatement(
				DatabaseConnectionFactory.getStatement("/sql/databaseversion.insert.all.writeDatabaseVersion.sql"), Statement.RETURN_GENERATED_KEYS)) {
	
			preparedStatement.setString(1, DatabaseVersionStatus.MASTER.toString());
			preparedStatement.setTimestamp(2, new Timestamp(databaseVersion.getHeader().getDate().getTime()));
			preparedStatement.setString(3, databaseVersion.getHeader().getClient());
			preparedStatement.setString(4, databaseVersion.getHeader().getVectorClock().toString());
	
			preparedStatement.executeUpdate();	
			
			try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {				
				if (resultSet.next()) {
					databaseVersionId = resultSet.getLong(1);
				}
				else {
					throw new SQLException("Cannot get new database version ID");
				}
			}
		}

		writeVectorClock(connection, databaseVersionId, databaseVersion.getVectorClock());

		chunkDao.writeChunks(connection, databaseVersion.getChunks());
		multiChunkDao.writeMultiChunks(connection, databaseVersion.getMultiChunks());
		fileContentDao.writeFileContents(connection, databaseVersion.getFileContents());
		fileHistoryDao.writeFileHistories(connection, databaseVersionId, databaseVersion.getFileHistories());
	}

	private void writeVectorClock(Connection connection, long databaseVersionId, VectorClock vectorClock) throws SQLException {
		for (Map.Entry<String, Long> vectorClockEntry : vectorClock.entrySet()) {
			PreparedStatement preparedStatement = getStatement(connection, "/sql/databaseversion.insert.all.writeVectorClock.sql");

			preparedStatement.setLong(1, databaseVersionId);
			preparedStatement.setString(2, vectorClockEntry.getKey());
			preparedStatement.setLong(3, vectorClockEntry.getValue());

			preparedStatement.executeUpdate();
			preparedStatement.close();
		}
	}

	public Long getMaxDirtyVectorClock(String machineName) {
		try (PreparedStatement preparedStatement = getStatement("/sql/databaseversion.select.dirty.getMaxDirtyVectorClock.sql")) {
			preparedStatement.setString(1, machineName);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					return resultSet.getLong("logicaltime");
				}
			}

			return null;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Iterator<DatabaseVersion> getDatabaseVersions(DatabaseVersionStatus status) {
		try (PreparedStatement preparedStatement = getStatement("/sql/select.getDatabaseVersionsByStatus.sql")) {
			preparedStatement.setString(1, status.toString());

			return new DatabaseVersionIteration(preparedStatement.executeQuery());
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Iterator<DatabaseVersion> getDatabaseVersionsTo(String machineName, long maxLocalClientVersion) {
		try (PreparedStatement preparedStatement = getStatement("/sql/databaseversion.select.master.getDatabaseVersionsTo.sql")) {
			preparedStatement.setString(1, machineName);
			preparedStatement.setString(2, machineName);
			preparedStatement.setLong(3, maxLocalClientVersion);

			return new DatabaseVersionIteration(preparedStatement.executeQuery());
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private class DatabaseVersionIteration implements Iterator<DatabaseVersion> {
		private ResultSet resultSet;
		private boolean hasNext;

		public DatabaseVersionIteration(ResultSet resultSet) throws SQLException {
			this.resultSet = resultSet;
			this.hasNext = resultSet.next();
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public DatabaseVersion next() {
			if (hasNext) {
				try {
					DatabaseVersion databaseVersion = createDatabaseVersionFromRow(resultSet);
					hasNext = resultSet.next();

					return databaseVersion;
				}
				catch (Exception e) {
					throw new RuntimeException("Cannot load next SQL row.", e);
				}
			}
			else {
				return null;
			}
		}

		@Override
		public void remove() {
			throw new RuntimeException("Not implemented.");
		}

	}

	protected DatabaseVersion createDatabaseVersionFromRow(ResultSet resultSet) throws SQLException {
		DatabaseVersion databaseVersion = new DatabaseVersion();

		DatabaseVersionHeader databaseVersionHeader = createDatabaseVersionHeaderFromRow(resultSet);
		databaseVersion.setHeader(databaseVersionHeader);

		Map<ChunkChecksum, ChunkEntry> chunks = chunkDao.getChunks(databaseVersionHeader.getVectorClock());
		Map<MultiChunkId, MultiChunkEntry> multiChunks = multiChunkDao.getMultiChunks(databaseVersionHeader.getVectorClock());
		Map<FileChecksum, FileContent> fileContents = fileContentDao.getFileContents(databaseVersionHeader.getVectorClock());
		List<PartialFileHistory> fileHistories = fileHistoryDao.getFileHistoriesWithFileVersions(databaseVersionHeader.getVectorClock());

		for (ChunkEntry chunk : chunks.values()) {
			databaseVersion.addChunk(chunk);
		}

		for (MultiChunkEntry multiChunk : multiChunks.values()) {
			databaseVersion.addMultiChunk(multiChunk);
		}

		for (FileContent fileContent : fileContents.values()) {
			databaseVersion.addFileContent(fileContent);
		}

		for (PartialFileHistory fileHistory : fileHistories) {
			databaseVersion.addFileHistory(fileHistory);
		}

		return databaseVersion;
	}

	public DatabaseVersionHeader getLastDatabaseVersionHeader() {
		try (PreparedStatement preparedStatement = getStatement("/sql/databaseversion.select.master.getLastDatabaseVersionHeader.sql")) {
			preparedStatement.setMaxRows(1);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					DatabaseVersionHeader databaseVersionHeader = createDatabaseVersionHeaderFromRow(resultSet);
					return databaseVersionHeader;
				}
			}

			return null;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private DatabaseVersionHeader createDatabaseVersionHeaderFromRow(ResultSet resultSet) throws SQLException {
		DatabaseVersionHeader databaseVersionHeader = new DatabaseVersionHeader();

		databaseVersionHeader.setClient(resultSet.getString("client"));
		databaseVersionHeader.setDate(new Date(resultSet.getTimestamp("localtime").getTime()));
		databaseVersionHeader.setVectorClock(getVectorClockByDatabaseVersionId(resultSet.getInt("id")));

		return databaseVersionHeader;
	}

	public DatabaseBranch getLocalDatabaseBranch() {
		DatabaseBranch databaseBranch = new DatabaseBranch();

		try (PreparedStatement preparedStatement = getStatement("/sql/databaseversion.select.master.getLocalDatabaseBranch.sql")) {
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
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
						currentDatabaseVersionHeader.setDate(new Date(resultSet.getTimestamp("localtime").getTime()));
	
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
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	protected VectorClock getVectorClockByDatabaseVersionId(int databaseVersionId) throws SQLException {
		PreparedStatement preparedStatement = getStatement("/sql/select.getVectorClockByDatabaseVersionId.sql");
		preparedStatement.setInt(1, databaseVersionId);

		ResultSet resultSet = preparedStatement.executeQuery();

		VectorClock vectorClock = new VectorClock();

		while (resultSet.next()) {
			vectorClock.setClock(resultSet.getString("client"), resultSet.getLong("logicaltime"));
		}
		
		resultSet.close();
		preparedStatement.close();
		
		return vectorClock;
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
		preparedStatement.close();
	}
	
	private void removeDirtyDatabaseVersionsInt() throws SQLException {
		PreparedStatement preparedStatement = getStatement("/sql/delete.removeDirtyDatabaseVersionsInt.sql");
		preparedStatement.executeUpdate();		
		preparedStatement.close();
	}

	private void removeDirtyFileHistories() throws SQLException {
		PreparedStatement preparedStatement = getStatement("/sql/delete.removeDirtyFileHistories.sql");
		preparedStatement.executeUpdate();
		preparedStatement.close();
	}

	private void removeDirtyFileVersions() throws SQLException {
		PreparedStatement preparedStatement = getStatement("/sql/delete.removeDirtyFileVersions.sql");
		preparedStatement.executeUpdate();	
		preparedStatement.close();
	}

	private void removeDirtyFileContents() {
		// TODO Auto-generated method stub

	}

	private void removeDirtyChunks() throws SQLException {
		PreparedStatement preparedStatement = getStatement("/sql/delete.removeDirtyUnreferencedChunks.sql");
		preparedStatement.executeUpdate();
		preparedStatement.close();
	}

	private void removeDirtyMultiChunks() throws SQLException {
		//PreparedStatement preparedStatement = getStatement("/sql/delete.removeDirtyMultiChunks.sql");
		//preparedStatement.executeUpdate();
	}
}
