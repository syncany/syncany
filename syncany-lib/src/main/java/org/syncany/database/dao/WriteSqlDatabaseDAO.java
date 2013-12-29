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
package org.syncany.database.dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;

import org.syncany.database.ChunkEntry;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.VectorClock;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.PartialFileHistory.FileHistoryId;

/**
 * @author pheckel
 *
 */
public class WriteSqlDatabaseDAO extends SqlDatabaseDAO {
	public WriteSqlDatabaseDAO(Connection connection) {
		super(connection);
	}
	
	public void persistDatabaseVersion(DatabaseVersion databaseVersion) throws IOException {
		try {
			writeDatabaseVersion(connection, databaseVersion);
			
			connection.commit();
			connection.close();
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "SQL Error: ", e);
			
			if (connection != null) {
				try {
					connection.rollback();
				}
				catch (SQLException e1) {
					// Don't care!
				}
			}
		}
		finally {
			if (connection != null) {
				try {
					connection.close();
				}
				catch (SQLException e) {
					// Don't care!
				}
			}
		}
	}

	private void writeDatabaseVersion(Connection connection, DatabaseVersion databaseVersion) throws SQLException {		
		PreparedStatement preparedStatement = connection.prepareStatement(
				"insert into databaseversion (localtime, client) values (?, ?)", Statement.RETURN_GENERATED_KEYS);

		preparedStatement.setDate(1, new java.sql.Date(databaseVersion.getHeader().getDate().getTime()));
		preparedStatement.setString(2, databaseVersion.getHeader().getClient());

		preparedStatement.executeUpdate();
		
		long databaseVersionId = -1;
	    ResultSet resultSet = preparedStatement.getGeneratedKeys();
	    
	    if (resultSet.next()) {
	        databaseVersionId = resultSet.getLong(1);
	    } 
	    else {
	        throw new SQLException("Cannot get new database version ID");
	    }
	    
	    writeVectorClock(connection, databaseVersionId, databaseVersion.getVectorClock());

		writeChunks(connection, databaseVersion.getChunks());
		writeMultiChunks(connection, databaseVersion.getMultiChunks());
	    writeFileContents(connection, databaseVersion.getFileContents());	    
	    writeFileHistories(connection, databaseVersionId, databaseVersion.getFileHistories());
	}

	private void writeMultiChunks(Connection connection, Collection<MultiChunkEntry> multiChunks) throws SQLException {
		for (MultiChunkEntry multiChunk : multiChunks) {
			PreparedStatement preparedStatement = connection.prepareStatement(
					"insert into multichunk (id) values (?)");

			preparedStatement.setString(1, multiChunk.getId().toString());
			preparedStatement.executeUpdate();
						
			for (ChunkChecksum chunkChecksum : multiChunk.getChunks()) {
				PreparedStatement preparedStatement1 = connection.prepareStatement(
						"insert into multichunk_chunk (multichunk_id, chunk_checksum) values (?, ?)");

				preparedStatement1.setString(1, multiChunk.getId().toString());
				preparedStatement1.setString(2, chunkChecksum.toString());
				
				preparedStatement1.executeUpdate();
			}
		}
	}

	private void writeFileContents(Connection connection, Collection<FileContent> fileContents) throws SQLException {
		for (FileContent fileContent : fileContents) {
			PreparedStatement preparedStatement = connection.prepareStatement(
					"insert into filecontent (checksum, size) values (?, ?)");

			preparedStatement.setString(1, fileContent.getChecksum().toString());
			preparedStatement.setLong(2, fileContent.getSize());
			
			preparedStatement.executeUpdate();
						
			int order = 0;
			for (ChunkChecksum chunkChecksum : fileContent.getChunks()) {
				PreparedStatement preparedStatement1 = connection.prepareStatement(
						"insert into filecontent_chunk (filecontent_checksum, chunk_checksum, num) values (?, ?, ?)");

				preparedStatement1.setString(1, fileContent.getChecksum().toString());
				preparedStatement1.setString(2, chunkChecksum.toString());
				preparedStatement1.setInt(3, order);
				
				preparedStatement1.executeUpdate();
				
				order++;				
			}
		}
	}

	private void writeFileHistories(Connection connection, long databaseVersionId, Collection<PartialFileHistory> fileHistories) throws SQLException {
		for (PartialFileHistory fileHistory : fileHistories) {
			PreparedStatement preparedStatement = connection.prepareStatement(
					"insert into filehistory (id, databaseversion_id) values (?, ?)");

			preparedStatement.setString(1, fileHistory.getFileId().toString());
			preparedStatement.setLong(2, databaseVersionId);
			
			preparedStatement.executeUpdate();
			
			writeFileVersions(connection, fileHistory.getFileId(), fileHistory.getFileVersions().values());
		}
	}

	private void writeFileVersions(Connection connection, FileHistoryId fileHistoryId, Collection<FileVersion> fileVersions) throws SQLException {
		for (FileVersion fileVersion : fileVersions) {
			String fileContentChecksumStr = (fileVersion.getChecksum() != null) ? fileVersion.getChecksum().toString() : null;					  
			
			PreparedStatement preparedStatement = connection.prepareStatement(
					"insert into fileversion "
					+ "(filehistory_id, version, path, type, status, size, lastmodified, linktarget, filecontent_checksum, updated, posixperms, dosattrs) "
					+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

			preparedStatement.setString(1, fileHistoryId.toString());
			preparedStatement.setInt(2, Integer.parseInt(""+fileVersion.getVersion()));
			preparedStatement.setString(3, fileVersion.getPath());
			preparedStatement.setString(4, fileVersion.getType().toString());
			preparedStatement.setString(5, fileVersion.getStatus().toString());
			preparedStatement.setLong(6, fileVersion.getSize());
			preparedStatement.setDate(7, new java.sql.Date(fileVersion.getLastModified().getTime()));
			preparedStatement.setString(8, fileVersion.getLinkTarget());
			preparedStatement.setString(9, fileContentChecksumStr);
			preparedStatement.setDate(10, new java.sql.Date(fileVersion.getUpdated().getTime()));
			preparedStatement.setString(11, fileVersion.getPosixPermissions());
			preparedStatement.setString(12, fileVersion.getDosAttributes());
			
			preparedStatement.executeUpdate();			
		}				
	}

	private void writeVectorClock(Connection connection, long databaseVersionId, VectorClock vectorClock) throws SQLException {			
		for (Map.Entry<String, Long> vectorClockEntry : vectorClock.entrySet()) {
			PreparedStatement preparedStatement = connection.prepareStatement(
					"insert into vectorclock (databaseversion_id, client, logicaltime) values (?, ?, ?)");

			preparedStatement.setLong(1, databaseVersionId);
			preparedStatement.setString(2, vectorClockEntry.getKey());
			preparedStatement.setLong(3, vectorClockEntry.getValue());
			
			preparedStatement.executeUpdate();			
		}
	}

	private void writeChunks(Connection connection, Collection<ChunkEntry> chunks) throws SQLException {
		if (chunks.size() > 0) {
			for (ChunkEntry chunk : chunks) {
				PreparedStatement preparedStatement = connection.prepareStatement("insert into chunk (checksum, size) values (?, ?)");

				preparedStatement.setString(1, chunk.getChecksum().toString());
				preparedStatement.setInt(2, chunk.getSize());

				preparedStatement.executeUpdate();
			}
		}
	}
}
