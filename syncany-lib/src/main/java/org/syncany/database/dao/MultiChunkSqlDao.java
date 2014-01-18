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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.DatabaseVersion.DatabaseVersionStatus;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.VectorClock;

/**
 * @author pheckel
 *
 */
public class MultiChunkSqlDao extends AbstractSqlDao {
	protected static final Logger logger = Logger.getLogger(MultiChunkSqlDao.class.getSimpleName());	

	public MultiChunkSqlDao(Connection connection) {
		super(connection);
	}

	public void writeMultiChunks(Connection connection, Collection<MultiChunkEntry> multiChunks) throws SQLException {
		for (MultiChunkEntry multiChunk : multiChunks) {
			PreparedStatement preparedStatement = getStatement(connection, "/sql/insert.writeMultiChunks.sql");

			preparedStatement.setString(1, multiChunk.getId().toString());
			preparedStatement.executeUpdate();
			preparedStatement.close();
			writeMultiChunkRefs(connection, multiChunk);			
		}
	}

	private void writeMultiChunkRefs(Connection connection, MultiChunkEntry multiChunk) throws SQLException {
		PreparedStatement preparedStatement = getStatement("/sql/insert.writeMultiChunkRefs.sql");
		
		for (ChunkChecksum chunkChecksum : multiChunk.getChunks()) {
			preparedStatement.setString(1, multiChunk.getId().toString());
			preparedStatement.setString(2, chunkChecksum.toString());
			
			preparedStatement.addBatch();			
		}
		
		preparedStatement.executeBatch();
		preparedStatement.close();
	}
	
	public List<MultiChunkEntry> getMultiChunksForFileChecksum(FileChecksum fileChecksum) {
		if (fileChecksum == null) {
			return new ArrayList<MultiChunkEntry>();			
		}
		else {
			try (PreparedStatement preparedStatement = getStatement("/sql/select.getMultiChunksForFileChecksum.sql")) {
				preparedStatement.setString(1, fileChecksum.toString());
	
				try (ResultSet resultSet = preparedStatement.executeQuery()) {
					List<MultiChunkEntry> multiChunkEntries = new ArrayList<MultiChunkEntry>();
					
					while (resultSet.next()) {
						MultiChunkId multiChunkId = MultiChunkId.parseMultiChunkId(resultSet.getString("multichunk_id"));
						MultiChunkEntry multiChunkEntry = new MultiChunkEntry(multiChunkId);
						
						multiChunkEntries.add(multiChunkEntry);
					}
		
					return multiChunkEntries;
				}
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}	
	
	public Map<MultiChunkId, MultiChunkEntry> getMultiChunksForDatabaseVersion(VectorClock vectorClock) {
		try (PreparedStatement preparedStatement = getStatement("/sql/select.getMultiChunksWithChunksForDatabaseVersion.sql")) {
			preparedStatement.setString(1, vectorClock.toString());
			preparedStatement.setString(2, vectorClock.toString());

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				return createMultiChunkEntries(resultSet);
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}	
	
	protected Map<MultiChunkId, MultiChunkEntry> createMultiChunkEntries(ResultSet resultSet) throws SQLException {
		Map<MultiChunkId, MultiChunkEntry> multiChunkEntries = new HashMap<MultiChunkId, MultiChunkEntry>();		
		MultiChunkId currentMultiChunkId = null;
		
		while (resultSet.next()) {			
			MultiChunkId multiChunkId = MultiChunkId.parseMultiChunkId(resultSet.getString("multichunk_id"));
			MultiChunkEntry multiChunkEntry = null;
			
			if (currentMultiChunkId != null && currentMultiChunkId.equals(multiChunkId)) {
				multiChunkEntry = multiChunkEntries.get(multiChunkId);	
			}
			else {
				multiChunkEntry = new MultiChunkEntry(multiChunkId);
			}
			
			multiChunkEntry.addChunk(ChunkChecksum.parseChunkChecksum(resultSet.getString("chunk_checksum")));
			multiChunkEntries.put(multiChunkId, multiChunkEntry); 
			
			currentMultiChunkId = multiChunkId;
		}
		
		return multiChunkEntries;
	}
	
	public MultiChunkEntry getMultiChunkForChunk(ChunkChecksum chunkChecksum) {
		try (PreparedStatement preparedStatement = getStatement("/sql/select.getMultiChunkForChunk.sql")) {
			preparedStatement.setString(1, chunkChecksum.toString());
					
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					MultiChunkId multiChunkId = MultiChunkId.parseMultiChunkId(resultSet.getString("multichunk_id"));
					MultiChunkEntry multiChunkEntry = new MultiChunkEntry(multiChunkId);
					
					return multiChunkEntry;
				}
			}

			return null;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public MultiChunkEntry getMultiChunkWithStatus(MultiChunkId multiChunkId, DatabaseVersionStatus status) {
		try (PreparedStatement preparedStatement = getStatement("/sql/select.getMultiChunkWithStatus.sql")) {
			preparedStatement.setString(1, status.toString());
			preparedStatement.setString(2, multiChunkId.toString());
					
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					MultiChunkEntry multiChunkEntry = new MultiChunkEntry(multiChunkId);				
					return multiChunkEntry;
				}
			}

			return null;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
