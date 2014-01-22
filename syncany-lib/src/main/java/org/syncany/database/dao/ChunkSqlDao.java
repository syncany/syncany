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
 * @author pheckel
 *
 */
public class ChunkSqlDao extends AbstractSqlDao {
	protected static final Logger logger = Logger.getLogger(ChunkSqlDao.class.getSimpleName());
	private Map<ChunkChecksum, ChunkEntry> chunkCache;

	public ChunkSqlDao(Connection connection) {
		super(connection);
		this.chunkCache = null;
	}

	// TODO [low] Mark "no commit"
	public void writeChunks(Connection connection, Collection<ChunkEntry> chunks) throws SQLException {
		if (chunks.size() > 0) {
			PreparedStatement preparedStatement = getStatement(connection, "/sql/chunk.insert.all.writeChunks.sql");

			for (ChunkEntry chunk : chunks) {
				preparedStatement.setString(1, chunk.getChecksum().toString());
				preparedStatement.setInt(2, chunk.getSize());

				preparedStatement.addBatch();
			}

			preparedStatement.executeBatch();
			preparedStatement.close();
		}
	}
	
	public synchronized ChunkEntry getChunk(ChunkChecksum chunkChecksum) {
		if (chunkCache == null) {
			loadChunkCache();
		}

		return chunkCache.get(chunkChecksum);
	}
	
	public synchronized void clearCache() {
		if (chunkCache != null) {
			chunkCache.clear();
			chunkCache = null;
		}
	}

	private void loadChunkCache() {
		try (PreparedStatement preparedStatement = getStatement("/sql/chunk.select.all.loadChunkCache.sql")) {
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				chunkCache = createChunkEntries(resultSet);
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Map<ChunkChecksum, ChunkEntry> getChunks(VectorClock vectorClock) {
		try (PreparedStatement preparedStatement = getStatement("/sql/chunk.select.all.getChunksForDatabaseVersion.sql")) {

			preparedStatement.setString(1, vectorClock.toString());
			preparedStatement.setString(2, vectorClock.toString());

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
}
