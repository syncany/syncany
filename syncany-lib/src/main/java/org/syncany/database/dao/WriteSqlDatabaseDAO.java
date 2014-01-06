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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.database.ChunkEntry;
import org.syncany.database.DatabaseConnectionFactory;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersion.DatabaseVersionStatus;
import org.syncany.database.DatabaseVersionHeader;
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
	
	public void persistNewKnownRemoteDatabases(List<DatabaseRemoteFile> remoteDatabases) throws SQLException {
		PreparedStatement preparedStatement = connection.prepareStatement(
				"insert into known_databases (database_name) values (?)");

		for (DatabaseRemoteFile databaseRemoteFile : remoteDatabases) {
			preparedStatement.setString(1, databaseRemoteFile.getName());
			preparedStatement.addBatch();
		}
		
		preparedStatement.executeBatch();
		connection.commit();
	}
	
	public void persistDatabaseVersion(DatabaseVersion databaseVersion) throws IOException, SQLException {		
		try {
			logger.log(Level.INFO, "Persisting database version ... ");
			
			writeDatabaseVersion(connection, databaseVersion);			
			connection.commit();
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "SQL Error: ", e);
			
			if (connection != null) {
				connection.rollback();
			}
		}		
	}
	
	public void markDatabaseVersion(DatabaseVersionHeader databaseVersionHeader, DatabaseVersionStatus status) throws SQLException {
		PreparedStatement preparedStatement = connection.prepareStatement(DatabaseConnectionFactory.getStatement("/sql.update.markDatabaseVersion.sql"));

		preparedStatement.setString(1, status.toString());
		preparedStatement.setString(2, databaseVersionHeader.getVectorClock().toString());

		preparedStatement.executeUpdate();		
		connection.commit();
	}

	private void writeDatabaseVersion(Connection connection, DatabaseVersion databaseVersion) throws SQLException {		
		PreparedStatement preparedStatement = connection.prepareStatement(
				  "insert into databaseversion (status, localtime, client, vectorclock_serialized) "
				+ "values (?, ?, ?, ?)", 
				Statement.RETURN_GENERATED_KEYS);

		preparedStatement.setString(1, DatabaseVersionStatus.MASTER.toString());
		preparedStatement.setDate(2, new java.sql.Date(databaseVersion.getHeader().getDate().getTime()));
		preparedStatement.setString(3, databaseVersion.getHeader().getClient());
		preparedStatement.setString(4, databaseVersion.getHeader().getVectorClock().toString());

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
			
			writeFileVersions(connection, fileHistory.getFileId(), databaseVersionId, fileHistory.getFileVersions().values());
		}
	}

	private void writeFileVersions(Connection connection, FileHistoryId fileHistoryId, long databaseVersionId, Collection<FileVersion> fileVersions) throws SQLException {
		for (FileVersion fileVersion : fileVersions) {
			String fileContentChecksumStr = (fileVersion.getChecksum() != null) ? fileVersion.getChecksum().toString() : null;					  
			
			PreparedStatement preparedStatement = connection.prepareStatement(
					"insert into fileversion "
					+ "(filehistory_id, version, databaseversion_id, path, type, status, size, lastmodified, linktarget, filecontent_checksum, updated, posixperms, dosattrs) "
					+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

			preparedStatement.setString(1, fileHistoryId.toString());
			preparedStatement.setInt(2, Integer.parseInt(""+fileVersion.getVersion()));
			preparedStatement.setLong(3, databaseVersionId);
			preparedStatement.setString(4, fileVersion.getPath());
			preparedStatement.setString(5, fileVersion.getType().toString());
			preparedStatement.setString(6, fileVersion.getStatus().toString());
			preparedStatement.setLong(7, fileVersion.getSize());
			preparedStatement.setDate(8, new java.sql.Date(fileVersion.getLastModified().getTime()));
			preparedStatement.setString(9, fileVersion.getLinkTarget());
			preparedStatement.setString(10, fileContentChecksumStr);
			preparedStatement.setDate(11, new java.sql.Date(fileVersion.getUpdated().getTime()));
			preparedStatement.setString(12, fileVersion.getPosixPermissions());
			preparedStatement.setString(13, fileVersion.getDosAttributes());
			
			preparedStatement.executeUpdate();			
		}				
	}

	private void writeVectorClock(Connection connection, long databaseVersionId, VectorClock vectorClock) throws SQLException {			
		for (Map.Entry<String, Long> vectorClockEntry : vectorClock.entrySet()) {
			PreparedStatement preparedStatement = connection.prepareStatement(
					"insert into databaseversion_vectorclock (databaseversion_id, client, logicaltime) values (?, ?, ?)");

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
