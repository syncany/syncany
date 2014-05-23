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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.database.ChunkEntry;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.VectorClock;

/**
 * The chunk data access object (DAO) writes and queries the SQL database for information
 * on {@link ChunkEntry}s. It translates the relational data in the "chunk" table to
 * Java objects.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class ChunkSqlDao extends AbstractSqlDao {
	protected static final Logger logger = Logger.getLogger(ChunkSqlDao.class.getSimpleName());
	private Map<ChunkChecksum, ChunkEntry> chunkCache;

	public ChunkSqlDao(Connection connection) {
		super(connection);
		this.chunkCache = null;
	}

	/**
	 * Writes a list of {@link ChunkEntry}s to the database using <tt>INSERT</tt>s and the given connection.
	 * 
	 * <p><b>Note:</b> This method executes, but <b>does not commit</b> the query.
	 * 
	 * @param connection The connection used to execute the statements
	 * @param databaseVersionId 
	 * @param chunks List of {@link ChunkEntry}s to be inserted in the database
	 * @throws SQLException If the SQL statement fails
	 */
	public void writeChunks(Connection connection, long databaseVersionId, Collection<ChunkEntry> chunks) throws SQLException {
		if (chunks.size() > 0) {
			PreparedStatement preparedStatement = getStatement(connection, "chunk.insert.all.writeChunks.sql");

			for (ChunkEntry chunk : chunks) {
				preparedStatement.setString(1, chunk.getChecksum().toString());
				preparedStatement.setLong(2, databaseVersionId);
				preparedStatement.setInt(3, chunk.getSize());

				preparedStatement.addBatch();
			}

			preparedStatement.executeBatch();
			preparedStatement.close();
		}
	}	

	/**
	 * Removes unreferenced chunks from the database. Unreferenced chunks are chunks
	 * that are not referenced by any file content or multichunk. 
	 * 
	 * <p>During the cleanup process, when file versions are deleted, unused chunks 
	 * are left over. This method removes these chunks from the database.
	 * 
	 * <p><b>Note:</b> This method executes, but <b>does not commit</b> the query. 
	 */
	public void removeUnreferencedChunks() {
		try (PreparedStatement preparedStatement = getStatement("chunk.delete.all.removeUnreferencesChunks.sql")) {
			preparedStatement.execute();
			preparedStatement.close();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Queries the database of a chunk with the given checksum. 
	 * 
	 * <p>Note: When first called, this method loads the <b>chunk cache</b> and keeps
	 * this cache until it is cleared explicitly with {@link #clearCache()}. 
	 * 
	 * <p>Also note that this method will return <tt>null</tt> if the chunk has been
	 * added after the cache has been filled. 
	 * 
	 * @param chunkChecksum Chunk checksum of the chunk to be selected
	 * @return Returns the chunk entry, or <tt>null</tt> if the chunk does not exist.
	 */	
	public synchronized ChunkEntry getChunk(ChunkChecksum chunkChecksum) {
		if (chunkCache == null) {
			loadChunkCache();
		}

		return chunkCache.get(chunkChecksum);
	}
	
	/**
	 * Clears the chunk cache loaded by {@link #getChunk(ChunkChecksum) getChunk()}
	 * and resets the cache. If {@link #getChunk(ChunkChecksum) getChunk()} is called
	 * after the cache is cleared, it is re-populated.
	 */
	public synchronized void clearCache() {
		if (chunkCache != null) {
			chunkCache.clear();
			chunkCache = null;
		}
	}

	/**
	 * Queries the SQL database for all chunks that <b>originally appeared</b> in the
	 * database version identified by the given vector clock.
	 * 
	 * <p><b>Note:</b> This method does <b>not</b> select all the chunks that are referenced
	 * in the database version. In particular, it <b>does not return</b> chunks that appeared
	 * in previous other database versions.
	 * 
	 * @param vectorClock Vector clock that identifies the database version
	 * @return Returns all chunks that originally belong to a database version
	 */
	public Map<ChunkChecksum, ChunkEntry> getChunks(VectorClock vectorClock) {
		try (PreparedStatement preparedStatement = getStatement("chunk.select.all.getChunksForDatabaseVersion.sql")) {
			preparedStatement.setString(1, vectorClock.toString());

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				return createChunkEntries(resultSet);
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	protected Map<ChunkChecksum, ChunkEntry> createChunkEntries(ResultSet resultSet) throws SQLException {
		Map<ChunkChecksum, ChunkEntry> chunks = new HashMap<ChunkChecksum, ChunkEntry>();

		while (resultSet.next()) {
			ChunkEntry chunkEntry = createChunkEntryFromRow(resultSet);
			chunks.put(chunkEntry.getChecksum(), chunkEntry);
		}

		return chunks;
	}

	protected ChunkEntry createChunkEntryFromRow(ResultSet resultSet) throws SQLException {
		ChunkChecksum chunkChecksum = ChunkChecksum.parseChunkChecksum(resultSet.getString("checksum"));
		return new ChunkEntry(chunkChecksum, resultSet.getInt("size"));
	}
	
	protected void loadChunkCache() {
		try (PreparedStatement preparedStatement = getStatement("chunk.select.all.loadChunkCache.sql")) {
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				chunkCache = createChunkEntries(resultSet);
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * no commit
	 */
	public void updateDirtyChunksNewDatabaseId(long newDatabaseVersionId) {
		try (PreparedStatement preparedStatement = getStatement("chunk.update.dirty.updateDirtyChunksNewDatabaseId.sql")) {
			preparedStatement.setLong(1, newDatabaseVersionId);
			preparedStatement.executeUpdate();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}
}
