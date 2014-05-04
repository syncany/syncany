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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.syncany.chunk.MultiChunk;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.DatabaseVersion.DatabaseVersionStatus;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.VectorClock;

/**
 * The multi-chunk data access object (DAO) queries and modifies the <i>multichunk</i> and
 * <i>multichunk_chunk</t> table in the SQL database. These tables correspond to the Java
 * object {@link MultiChunk}.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class MultiChunkSqlDao extends AbstractSqlDao {
	protected static final Logger logger = Logger.getLogger(MultiChunkSqlDao.class.getSimpleName());	

	public MultiChunkSqlDao(Connection connection) {
		super(connection);
	}

	public void writeMultiChunks(Connection connection, long databaseVersionId, Collection<MultiChunkEntry> multiChunks) throws SQLException {
		for (MultiChunkEntry multiChunk : multiChunks) {
			PreparedStatement preparedStatement = getStatement(connection, "/sql/multichunk.insert.all.writeMultiChunks.sql");

			preparedStatement.setString(1, multiChunk.getId().toString());
			preparedStatement.setLong(2, databaseVersionId);
			preparedStatement.setLong(3, multiChunk.getSize());
			
			preparedStatement.executeUpdate();
			preparedStatement.close();
			
			writeMultiChunkRefs(connection, multiChunk);			
		}
	}

	private void writeMultiChunkRefs(Connection connection, MultiChunkEntry multiChunk) throws SQLException {
		PreparedStatement preparedStatement = getStatement("/sql/multichunk.insert.all.writeMultiChunkRefs.sql");
		
		for (ChunkChecksum chunkChecksum : multiChunk.getChunks()) {
			preparedStatement.setString(1, multiChunk.getId().toString());
			preparedStatement.setString(2, chunkChecksum.toString());
			
			preparedStatement.addBatch();			
		}
		
		preparedStatement.executeBatch();
		preparedStatement.close();
	}	

	public void removeUnreferencedMultiChunks() throws SQLException {
		// Note: Chunk references (multichunk_chunk) must be removed first, because
		//       of the foreign key constraints. 
		
		removeUnreferencedMultiChunkChunkRefs();
		removeUnreferencedMultiChunksInt();
	}
	
	private void removeUnreferencedMultiChunksInt() throws SQLException {
		PreparedStatement preparedStatement = getStatement("/sql/multichunk.delete.all.removeUnreferencedMultiChunks.sql");
		preparedStatement.executeUpdate();	
		preparedStatement.close();
	}
	
	private void removeUnreferencedMultiChunkChunkRefs() throws SQLException {
		PreparedStatement preparedStatement = getStatement("/sql/multichunk.delete.all.removeUnreferencedMultiChunkChunkRefs.sql");
		preparedStatement.executeUpdate();	
		preparedStatement.close();
	}
	
	/**
	 * Note: This method selects also {@link DatabaseVersionStatus#DIRTY DIRTY}.
	 */
	public List<MultiChunkId> getMultiChunkIds(FileChecksum fileChecksum) {
		List<MultiChunkId> multiChunkIds = new ArrayList<MultiChunkId>();

		if (fileChecksum == null) {
			return multiChunkIds;
		}
		else {
			try (PreparedStatement preparedStatement = getStatement("/sql/multichunk.select.all.getMultiChunkIdsForFileChecksum.sql")) {
				preparedStatement.setString(1, fileChecksum.toString());
	
				try (ResultSet resultSet = preparedStatement.executeQuery()) {
					while (resultSet.next()) {
						multiChunkIds.add(MultiChunkId.parseMultiChunkId(resultSet.getString("multichunk_id")));
					}
		
					return multiChunkIds;
				}
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}	
	
	/**
	 * Note: This method selects also {@link DatabaseVersionStatus#DIRTY DIRTY}.
	 */
	public Map<MultiChunkId, MultiChunkEntry> getMultiChunks(VectorClock vectorClock) {
		try (PreparedStatement preparedStatement = getStatement("/sql/multichunk.select.all.getMultiChunksWithChunksForDatabaseVersion.sql")) {
			preparedStatement.setString(1, vectorClock.toString());

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				return createMultiChunkEntriesWithChunks(resultSet);
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}	

	/**
	 * no commit
	 */
	public void updateDirtyMultiChunksNewDatabaseId(long newDatabaseVersionId) {
		try (PreparedStatement preparedStatement = getStatement("/sql/multichunk.update.dirty.updateDirtyMultiChunksNewDatabaseId.sql")) {
			preparedStatement.setLong(1, newDatabaseVersionId);
			preparedStatement.executeUpdate();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}
	
	/**
	 * Note: This method selects also {@link DatabaseVersionStatus#DIRTY DIRTY}.
	 */
	public MultiChunkId getMultiChunkId(ChunkChecksum chunkChecksum) {
		try (PreparedStatement preparedStatement = getStatement("/sql/multichunk.select.all.getMultiChunkIdForChunk.sql")) {
			preparedStatement.setString(1, chunkChecksum.toString());
					
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					return MultiChunkId.parseMultiChunkId(resultSet.getString("multichunk_id"));
				}
			}

			return null;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Note: This method selects also {@link DatabaseVersionStatus#DIRTY DIRTY}.
	 */
	public Map<ChunkChecksum,MultiChunkId> getMultiChunkIdsByChecksums(List<ChunkChecksum> chunkChecksums) {
		// Gather a unique array of checksum strings (required for query!)
		Set<ChunkChecksum> chunkChecksumSet = new HashSet<ChunkChecksum>(chunkChecksums);
		String[] checksums = new String[chunkChecksumSet.size()];
		int i = 0;
		for (ChunkChecksum checksum : chunkChecksumSet) {
			checksums[i] = checksum.toString();
			i++;
		}
		
		// Execute query
		Map<ChunkChecksum, MultiChunkId> result = new HashMap<ChunkChecksum, MultiChunkId>();
		try (PreparedStatement preparedStatement = getStatement("/sql/multichunk.select.all.getMultiChunkIdForChunks.sql")) {
			preparedStatement.setArray(1, connection.createArrayOf("varchar", checksums));	
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					result.put(ChunkChecksum.parseChunkChecksum(resultSet.getString("chunk_checksum")),
							MultiChunkId.parseMultiChunkId(resultSet.getString("multichunk_id")));
					
				}
			}

			return result;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public List<MultiChunkId> getDirtyMultiChunkIds() {
		List<MultiChunkId> dirtyMultiChunkIds = new ArrayList<MultiChunkId>();		
		
		try (PreparedStatement preparedStatement = getStatement("/sql/multichunk.select.dirty.getDirtyMultiChunkIds.sql")) {
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					dirtyMultiChunkIds.add(MultiChunkId.parseMultiChunkId(resultSet.getString("multichunk_id")));
				}
				
				return dirtyMultiChunkIds;
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Map<MultiChunkId, MultiChunkEntry> getUnusedMultiChunks() {
		try (PreparedStatement preparedStatement = getStatement("/sql/multichunk.select.all.getUnusedMultiChunks.sql")) {
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				return createMultiChunkEntriesWithoutChunks(resultSet);
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Map<MultiChunkId, MultiChunkEntry> getMultiChunks() {
		try (PreparedStatement preparedStatement = getStatement("/sql/multichunk.select.all.getMultiChunks.sql")) {
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				return createMultiChunkEntriesWithoutChunks(resultSet);
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<MultiChunkId, MultiChunkEntry> createMultiChunkEntriesWithoutChunks(ResultSet resultSet) throws SQLException {		
		Map<MultiChunkId, MultiChunkEntry> unusedMultiChunkIds = new HashMap<MultiChunkId, MultiChunkEntry>();		
		
		while (resultSet.next()) {
			MultiChunkId multiChunkId = MultiChunkId.parseMultiChunkId(resultSet.getString("id"));
			long multiChunkSize = resultSet.getLong("size");
						
			unusedMultiChunkIds.put(multiChunkId, new MultiChunkEntry(multiChunkId, multiChunkSize));
		}
		
		return unusedMultiChunkIds;
	}
	
	private Map<MultiChunkId, MultiChunkEntry> createMultiChunkEntriesWithChunks(ResultSet resultSet) throws SQLException {
		Map<MultiChunkId, MultiChunkEntry> multiChunkEntries = new HashMap<MultiChunkId, MultiChunkEntry>();		
		MultiChunkId currentMultiChunkId = null;
		
		while (resultSet.next()) {			
			MultiChunkId multiChunkId = MultiChunkId.parseMultiChunkId(resultSet.getString("multichunk_id"));
			long multiChunkSize = resultSet.getLong("size");
			
			MultiChunkEntry multiChunkEntry = null;
			
			if (currentMultiChunkId != null && currentMultiChunkId.equals(multiChunkId)) {
				multiChunkEntry = multiChunkEntries.get(multiChunkId);	
			}
			else {
				multiChunkEntry = new MultiChunkEntry(multiChunkId, multiChunkSize);
			}
			
			multiChunkEntry.addChunk(ChunkChecksum.parseChunkChecksum(resultSet.getString("chunk_checksum")));
			multiChunkEntries.put(multiChunkId, multiChunkEntry); 
			
			currentMultiChunkId = multiChunkId;
		}
		
		return multiChunkEntries;
	}
}
