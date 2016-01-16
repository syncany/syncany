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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
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
import org.syncany.database.FileVersion;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.VectorClock;
import org.syncany.operations.down.DatabaseBranch;

/**
 * The database version data access object (DAO) writes and queries the SQL database for information
 * on {@link DatabaseVersion}s. It translates the relational data in the "databaseversion" table to
 * Java objects; but also uses the other DAOs to persist entire {@link DatabaseVersion} objects. 
 * 
 * @see ChunkSqlDao
 * @see FileContentSqlDao
 * @see FileVersionSqlDao
 * @see FileHistorySqlDao
 * @see MultiChunkSqlDao
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class DatabaseVersionSqlDao extends AbstractSqlDao {
	protected static final Logger logger = Logger.getLogger(DatabaseVersionSqlDao.class.getSimpleName());

	private ChunkSqlDao chunkDao;
	private FileContentSqlDao fileContentDao;
	private FileVersionSqlDao fileVersionDao;
	private FileHistorySqlDao fileHistoryDao;
	private MultiChunkSqlDao multiChunkDao;

	public DatabaseVersionSqlDao(Connection connection, ChunkSqlDao chunkDao, FileContentSqlDao fileContentDao, FileVersionSqlDao fileVersionDao,
			FileHistorySqlDao fileHistoryDao,
			MultiChunkSqlDao multiChunkDao) {

		super(connection);

		this.chunkDao = chunkDao;
		this.fileContentDao = fileContentDao;
		this.fileVersionDao = fileVersionDao;
		this.fileHistoryDao = fileHistoryDao;
		this.multiChunkDao = multiChunkDao;
	}

	/**
	 * Marks the database version with the given vector clock as DIRTY, i.e.
	 * sets the {@link DatabaseVersionStatus} to {@link DatabaseVersionStatus#DIRTY DIRTY}.
	 * Marking a database version dirty will lead to a deletion in the next sync up
	 * cycle.
	 * 
	 * @param vectorClock Identifies the database version to mark dirty
	 */
	public void markDatabaseVersionDirty(VectorClock vectorClock) {
		try (PreparedStatement preparedStatement = getStatement("databaseversion.update.master.markDatabaseVersionDirty.sql")) {
			preparedStatement.setString(1, DatabaseVersionStatus.DIRTY.toString());
			preparedStatement.setString(2, vectorClock.toString());

			preparedStatement.executeUpdate();
			connection.commit();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public long writeDatabaseVersion(DatabaseVersion databaseVersion) {
		try {
			// Insert & commit database version
			long databaseVersionId = writeDatabaseVersion(connection, databaseVersion);
			
			// Commit & clear local caches
			clearCaches();

			return databaseVersionId;
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "SQL Error: ", e);

			throw new RuntimeException("Cannot persist database.", e);
		}
	}

	/**
	 * Writes the given {@link DatabaseVersionHeader} to the database, including the
	 * contained {@link VectorClock}. Be aware that the method writes the header independent
	 * of whether or not a corresponding database version actually exists.
	 * 
	 * <p>This method can be used to add empty database versions to the database. Current use
	 * case is adding an empty purge database version to the database.
	 * 
	 * <p><b>Note:</b> This method executes, but <b>does not commit</b> the query.
	 * 
	 * @param databaseVersionHeader The database version header to write to the database
	 * @return Returns the SQL-internal primary key of the new database version
	 */
	public long writeDatabaseVersionHeader(DatabaseVersionHeader databaseVersionHeader) throws SQLException {
		long databaseVersionId = writeDatabaseVersionHeaderInternal(connection, databaseVersionHeader);
		writeVectorClock(connection, databaseVersionId, databaseVersionHeader.getVectorClock());

		return databaseVersionId;
	}

	private long writeDatabaseVersion(Connection connection, DatabaseVersion databaseVersion) throws SQLException {
		long databaseVersionId = writeDatabaseVersionHeaderInternal(connection, databaseVersion.getHeader()); // TODO [low] Use writeDatabaseVersion()?
		writeVectorClock(connection, databaseVersionId, databaseVersion.getHeader().getVectorClock());

		chunkDao.writeChunks(connection, databaseVersionId, databaseVersion.getChunks());
		multiChunkDao.writeMultiChunks(connection, databaseVersionId, databaseVersion.getMultiChunks());
		fileContentDao.writeFileContents(connection, databaseVersionId, databaseVersion.getFileContents());
		fileHistoryDao.writeFileHistories(connection, databaseVersionId, databaseVersion.getFileHistories());

		return databaseVersionId;
	}

	private long writeDatabaseVersionHeaderInternal(Connection connection, DatabaseVersionHeader databaseVersionHeader) throws SQLException {
		try (PreparedStatement preparedStatement = connection.prepareStatement(
				DatabaseConnectionFactory.getStatement("databaseversion.insert.all.writeDatabaseVersion.sql"), Statement.RETURN_GENERATED_KEYS)) {

			preparedStatement.setString(1, DatabaseVersionStatus.MASTER.toString());
			preparedStatement.setTimestamp(2, new Timestamp(databaseVersionHeader.getDate().getTime()));
			preparedStatement.setString(3, databaseVersionHeader.getClient());
			preparedStatement.setString(4, databaseVersionHeader.getVectorClock().toString());

			int affectedRows = preparedStatement.executeUpdate();

			if (affectedRows == 0) {
				throw new SQLException("Cannot add database version header. Affected rows is zero.");
			}

			try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
				if (resultSet.next()) {
					return resultSet.getLong(1);
				}
				else {
					throw new SQLException("Cannot get new database version ID");
				}
			}
		}
	}

	private void writeVectorClock(Connection connection, long databaseVersionId, VectorClock vectorClock) throws SQLException {
		try (PreparedStatement preparedStatement = getStatement(connection, "databaseversion.insert.all.writeVectorClock.sql")) {
			for (Map.Entry<String, Long> vectorClockEntry : vectorClock.entrySet()) {
				preparedStatement.setLong(1, databaseVersionId);
				preparedStatement.setString(2, vectorClockEntry.getKey());
				preparedStatement.setLong(3, vectorClockEntry.getValue());

				preparedStatement.addBatch();
			}

			preparedStatement.executeBatch();
		}
	}

	/**
	 * Removes dirty {@link DatabaseVersion}s, {@link FileVersion}s, {@link PartialFileHistory}s and {@link FileContent}s
	 * from the database, but leaves stale/unreferenced chunks/multichunks untouched (must be cleaned up at a later stage).
	 * @param newDatabaseVersionId 
	 */
	public void removeDirtyDatabaseVersions(long newDatabaseVersionId) {
		try {
			// IMPORTANT: The order is important, because of 
			//            the database foreign key consistencies!

			// First, remove dirty file histories, then file versions
			fileVersionDao.removeDirtyFileVersions();
			fileHistoryDao.removeDirtyFileHistories();

			// Now, remove all unreferenced file contents
			fileContentDao.removeUnreferencedFileContents();

			// Change foreign key of multichunks
			multiChunkDao.updateDirtyMultiChunksNewDatabaseId(newDatabaseVersionId);
			fileContentDao.updateDirtyFileContentsNewDatabaseId(newDatabaseVersionId);
			chunkDao.updateDirtyChunksNewDatabaseId(newDatabaseVersionId);

			// And the database versions
			removeDirtyVectorClocks();
			removeDirtyDatabaseVersionsInt();

			// Commit & clear local caches
			connection.commit();
			clearCaches();
		}
		catch (SQLException e) {
			throw new RuntimeException("Unable to remove dirty database versions.", e);
		}
	}

	public void removeEmptyDatabaseVersionHeaders() {		
		// Delete vector clocks
		try (PreparedStatement preparedStatement = getStatement("databaseversion.delete.all.removeEmptyDatabaseVersionHeadersVectorClocks.sql")) {
			preparedStatement.executeUpdate();		
			preparedStatement.close();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		
		// Delete database version headers
		try (PreparedStatement preparedStatement = getStatement("databaseversion.delete.all.removeEmptyDatabaseVersionHeaders.sql")) {
			preparedStatement.executeUpdate();		
			preparedStatement.close();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void clearCaches() {
		chunkDao.clearCache();
	}

	public Long getMaxDirtyVectorClock(String machineName) {
		try (PreparedStatement preparedStatement = getStatement("databaseversion.select.dirty.getMaxDirtyVectorClock.sql")) {
			preparedStatement.setMaxRows(1);
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

	public List<DatabaseVersionHeader> getNonEmptyDatabaseVersionHeaders() {
		List<DatabaseVersionHeader> databaseVersionHeaders = new ArrayList<>();

		try (PreparedStatement preparedStatement = getStatement("databaseversion.select.master.getNonEmptyDatabaseVersionHeaders.sql")) {
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					DatabaseVersionHeader databaseVersionHeader = createDatabaseVersionHeaderFromRow(resultSet);
					databaseVersionHeaders.add(databaseVersionHeader);
				}
			}

			return databaseVersionHeaders;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Iterator<DatabaseVersion> getDirtyDatabaseVersions() {
		try (PreparedStatement preparedStatement = getStatement("databaseversion.select.dirty.getDirtyDatabaseVersions.sql")) {
			preparedStatement.setString(1, DatabaseVersionStatus.DIRTY.toString());

			return new DatabaseVersionIterator(preparedStatement.executeQuery());
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Iterator<DatabaseVersion> getDatabaseVersionsTo(String machineName, long maxLocalClientVersion) {
		try (PreparedStatement preparedStatement = getStatement("databaseversion.select.master.getDatabaseVersionsTo.sql")) {
			preparedStatement.setString(1, machineName);
			preparedStatement.setLong(2, maxLocalClientVersion);

			return new DatabaseVersionIterator(preparedStatement.executeQuery());
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Iterator<DatabaseVersion> getLastDatabaseVersions(int maxDatabaseVersionCount, int startDatabaseVersionIndex, int maxFileHistoryCount) {
		try (PreparedStatement preparedStatement = getStatement("databaseversion.select.master.getLastDatabaseVersions.sql")) {
			maxDatabaseVersionCount = (maxDatabaseVersionCount > 0) ? maxDatabaseVersionCount : Integer.MAX_VALUE;
			startDatabaseVersionIndex = (startDatabaseVersionIndex > 0) ? startDatabaseVersionIndex : 0;
			
			preparedStatement.setInt(1, maxDatabaseVersionCount);
			preparedStatement.setInt(2, startDatabaseVersionIndex);
			
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				return new DatabaseVersionIterator(preparedStatement.executeQuery(), true, maxFileHistoryCount);
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}


	private class DatabaseVersionIterator implements Iterator<DatabaseVersion> {
		private ResultSet resultSet;
		private boolean excludeChunkData;
		private int fileHistoryMaxCount;

		private boolean hasNext;

		public DatabaseVersionIterator(ResultSet resultSet) throws SQLException {
			this(resultSet, false, -1);
		}

		public DatabaseVersionIterator(ResultSet resultSet, boolean excludeChunkData, int fileHistoryMaxCount) throws SQLException {
			this.resultSet = resultSet;
			this.excludeChunkData = excludeChunkData;
			this.fileHistoryMaxCount = fileHistoryMaxCount;
					
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
					DatabaseVersion databaseVersion = createDatabaseVersionFromRow(resultSet, excludeChunkData, fileHistoryMaxCount);
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

	protected DatabaseVersion createDatabaseVersionFromRow(ResultSet resultSet, boolean excludeChunkData, int fileHistoryMaxCount) throws SQLException {
		DatabaseVersionHeader databaseVersionHeader = createDatabaseVersionHeaderFromRow(resultSet);

		DatabaseVersion databaseVersion = new DatabaseVersion();
		databaseVersion.setHeader(databaseVersionHeader);

		// Add chunk/multichunk/filecontent data
		if (!excludeChunkData) {
			Map<ChunkChecksum, ChunkEntry> chunks = chunkDao.getChunks(databaseVersionHeader.getVectorClock());
			Map<MultiChunkId, MultiChunkEntry> multiChunks = multiChunkDao.getMultiChunks(databaseVersionHeader.getVectorClock());
			Map<FileChecksum, FileContent> fileContents = fileContentDao.getFileContents(databaseVersionHeader.getVectorClock());
			
			for (ChunkEntry chunk : chunks.values()) {
				databaseVersion.addChunk(chunk);
			}

			for (MultiChunkEntry multiChunk : multiChunks.values()) {
				databaseVersion.addMultiChunk(multiChunk);
			}

			for (FileContent fileContent : fileContents.values()) {
				databaseVersion.addFileContent(fileContent);
			}
		}
		
		// Add file histories
		Map<FileHistoryId, PartialFileHistory> fileHistories = fileHistoryDao
				.getFileHistoriesWithFileVersions(databaseVersionHeader.getVectorClock(), fileHistoryMaxCount);

		for (PartialFileHistory fileHistory : fileHistories.values()) {
			databaseVersion.addFileHistory(fileHistory);
		}

		return databaseVersion;
	}

	public DatabaseVersionHeader getLastDatabaseVersionHeader() {
		try (PreparedStatement preparedStatement = getStatement("databaseversion.select.master.getLastDatabaseVersionHeader.sql")) {
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

		try (PreparedStatement preparedStatement = getStatement("databaseversion.select.master.getLocalDatabaseBranch.sql")) {
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
		PreparedStatement preparedStatement = getStatement("databaseversion.select.all.getVectorClockByDatabaseVersionId.sql");
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

	private void removeDirtyVectorClocks() throws SQLException {
		PreparedStatement preparedStatement = getStatement("databaseversion.delete.dirty.removeDirtyVectorClocks.sql");
		preparedStatement.executeUpdate();
		preparedStatement.close();
	}

	private void removeDirtyDatabaseVersionsInt() throws SQLException {
		PreparedStatement preparedStatement = getStatement("databaseversion.delete.dirty.removeDirtyDatabaseVersionsInt.sql");
		preparedStatement.executeUpdate();
		preparedStatement.close();
	}
}
