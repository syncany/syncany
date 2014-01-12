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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.database.ChunkEntry;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersion.DatabaseVersionStatus;
import org.syncany.database.DatabaseConnectionFactory;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.FileContent;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.VectorClock;
import org.syncany.operations.DatabaseBranch;

/**
 * @author pheckel
 *
 */
public class SqlDatabaseDAO {
	protected static final Logger logger = Logger.getLogger(SqlDatabaseDAO.class.getSimpleName());
	protected Connection connection;

	public SqlDatabaseDAO(Connection connection) {
		this.connection = connection;
	}
	
	public Connection getConnection() {
		return connection;
	}

	public Map<String, FileVersion> getFilesInDatabase() {
		Map<String, FileVersion> currentFileTree = new HashMap<String, FileVersion>();

		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getCurrentFileTree.sql");
			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				FileVersion fileVersion = createFileVersionFromRow(resultSet);
				currentFileTree.put(fileVersion.getPath(), fileVersion);
			}

			return currentFileTree;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Long getMaxDirtyVectorClock(String machineName) {
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getMaxDirtyVectorClock.sql");
			preparedStatement.setString(1, machineName);
			
			ResultSet resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				return resultSet.getLong("logicaltime");
			}

			return null;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public FileVersion getFileVersionByPath(String path) {
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getFileVersionByPath.sql");
			preparedStatement.setString(1, path);

			ResultSet resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				return createFileVersionFromRow(resultSet);
			}

			return null;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public FileVersion getFileVersionByFileHistoryId(FileHistoryId fileHistoryId) {
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getFileVersionByFileHistoryId.sql");
			preparedStatement.setString(1, fileHistoryId.toString());

			ResultSet resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				return createFileVersionFromRow(resultSet);
			}

			return null;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public List<MultiChunkEntry> getMultiChunksForFileChecksum(FileChecksum fileChecksum) {
		if (fileChecksum == null) {
			return new ArrayList<MultiChunkEntry>();			
		}
		else {
			try {
				PreparedStatement preparedStatement = getStatement("/sql/select.getMultiChunksForFileChecksum.sql");
				preparedStatement.setString(1, fileChecksum.toString());
	
				ResultSet resultSet = preparedStatement.executeQuery();
				List<MultiChunkEntry> multiChunkEntries = new ArrayList<MultiChunkEntry>();
				
				while (resultSet.next()) {
					MultiChunkId multiChunkId = MultiChunkId.parseMultiChunkId(resultSet.getString("multichunk_id"));
					MultiChunkEntry multiChunkEntry = new MultiChunkEntry(multiChunkId);
					
					multiChunkEntries.add(multiChunkEntry);
				}
	
				return multiChunkEntries;
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}	
	
	public Iterator<DatabaseVersion> getDatabaseVersions(DatabaseVersionStatus status) {
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getDatabaseVersionsByStatus.sql");			
			preparedStatement.setString(1, status.toString());

			return new DatabaseVersionIteration(preparedStatement.executeQuery());			
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Iterator<DatabaseVersion> getDatabaseVersionsTo(String machineName, long maxLocalClientVersion) {
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getDatabaseVersionsTo.sql");
			
			preparedStatement.setString(1, machineName);
			preparedStatement.setLong(2, maxLocalClientVersion);

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
		
		Map<ChunkChecksum, ChunkEntry> chunks = getChunksForDatabaseVersion(databaseVersionHeader.getVectorClock());
		Map<MultiChunkId, MultiChunkEntry> multiChunks = getMultiChunksForDatabaseVersion(databaseVersionHeader.getVectorClock());
		Map<FileChecksum, FileContent>fileContents = getFileContentsForDatabaseVersion(databaseVersionHeader.getVectorClock());
		List<PartialFileHistory> fileHistories = getFileHistoriesForDatabaseVersion(databaseVersionHeader.getVectorClock());
		
		for (ChunkEntry chunk: chunks.values()) {
			databaseVersion.addChunk(chunk);
		}
		
		for (MultiChunkEntry multiChunk: multiChunks.values()) {
			databaseVersion.addMultiChunk(multiChunk);
		}
		
		for (FileContent fileContent: fileContents.values()) {
			databaseVersion.addFileContent(fileContent);
		}
		
		for (PartialFileHistory fileHistory : fileHistories) {
			databaseVersion.addFileHistory(fileHistory);
		}
		
		return databaseVersion;
	}
	
	private Map<FileChecksum, FileContent> getFileContentsForDatabaseVersion(VectorClock vectorClock) {
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getFileContentByDatabaseVersionWithChunkChecksums.sql");			
			preparedStatement.setString(1, vectorClock.toString());

			ResultSet resultSet = preparedStatement.executeQuery();
			return createFileContents(resultSet);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<FileChecksum, FileContent> createFileContents(ResultSet resultSet) throws SQLException {
		Map<FileChecksum, FileContent> fileContents = new HashMap<FileChecksum, FileContent>();		
		FileChecksum currentFileChecksum = null;
		
		while (resultSet.next()) {			
			FileChecksum fileChecksum = FileChecksum.parseFileChecksum(resultSet.getString("checksum"));
			FileContent fileContent = null;
			
			if (currentFileChecksum != null && currentFileChecksum.equals(fileChecksum)) {
				fileContent = fileContents.get(fileChecksum);	
			}
			else {
				fileContent = new FileContent();
				
				fileContent.setChecksum(fileChecksum);
				fileContent.setSize(resultSet.getLong("size"));
			}
			
			ChunkChecksum chunkChecksum = ChunkChecksum.parseChunkChecksum(resultSet.getString("chunk_checksum"));
			fileContent.addChunk(chunkChecksum);

			fileContents.put(fileChecksum, fileContent); 
			currentFileChecksum = fileChecksum;
		}
		
		return fileContents;
	}

	private Map<MultiChunkId, MultiChunkEntry> getMultiChunksForDatabaseVersion(VectorClock vectorClock) {
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getMultiChunksWithChunksForDatabaseVersion.sql");			
			preparedStatement.setString(1, vectorClock.toString());

			ResultSet resultSet = preparedStatement.executeQuery();
			return createMultiChunkEntries(resultSet);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected Map<ChunkChecksum, ChunkEntry> getChunksForDatabaseVersion(VectorClock vectorClock) {
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getChunksForDatabaseVersion.sql");			
			preparedStatement.setString(1, vectorClock.toString());

			ResultSet resultSet = preparedStatement.executeQuery();
			return createChunkEntries(resultSet);
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

	public List<PartialFileHistory> getFileHistoriesForDatabaseVersion(VectorClock databaseVersionVectorClock) {
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getFileHistoriesForDatabaseVersion.sql");			
			preparedStatement.setString(1, databaseVersionVectorClock.toString());

			ResultSet resultSet = preparedStatement.executeQuery();
			return createFileHistoriesFromResult(resultSet);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public FileContent getFileContentByChecksum(FileChecksum fileChecksum, boolean includeChunkChecksums) {
		if (fileChecksum == null) {
			return null;
		}
		else if (includeChunkChecksums) {
			return getFileContentByChecksumWithChunkChecksums(fileChecksum);			
		}
		else {
			return getFileContentByChecksumWithoutChunkChecksums(fileChecksum);			
		}
	}

	public FileContent getFileContentByChecksumWithoutChunkChecksums(FileChecksum fileChecksum) {
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getFileContentByChecksumWithoutChunkChecksums.sql");
			preparedStatement.setString(1, fileChecksum.toString());

			ResultSet resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				FileContent fileContent = new FileContent();

				fileContent.setChecksum(FileChecksum.parseFileChecksum(resultSet.getString("checksum")));
				fileContent.setSize(resultSet.getLong("size"));

				return fileContent;
			}

			return null;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public FileContent getFileContentByChecksumWithChunkChecksums(FileChecksum fileChecksum) {
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getFileContentByChecksumWithChunkChecksums.sql");				
			preparedStatement.setString(1, fileChecksum.toString());

			ResultSet resultSet = preparedStatement.executeQuery();
			FileContent fileContent = null;
			
			while (resultSet.next()) {
				if (fileContent == null) {
					fileContent = new FileContent();
					
					fileContent.setChecksum(FileChecksum.parseFileChecksum(resultSet.getString("checksum")));
					fileContent.setSize(resultSet.getLong("size"));
				}
				
				// Add chunk references
				ChunkChecksum chunkChecksum = ChunkChecksum.parseChunkChecksum(resultSet.getString("chunk_checksum"));
				fileContent.addChunk(chunkChecksum);
			}

			return fileContent;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public List<PartialFileHistory> getFileHistoriesWithFileVersions() {
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getFileHistoriesWithFileVersions.sql");
			ResultSet resultSet = preparedStatement.executeQuery();

			return createFileHistoriesFromResult(resultSet);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected List<PartialFileHistory> createFileHistoriesFromResult(ResultSet resultSet) throws SQLException {
		List<PartialFileHistory> fileHistories = new ArrayList<PartialFileHistory>();
		PartialFileHistory fileHistory = null;
		
		while (resultSet.next()) {
			FileVersion lastFileVersion = createFileVersionFromRow(resultSet);
			FileHistoryId fileHistoryId = FileHistoryId.parseFileId(resultSet.getString("filehistory_id"));
			
			if (fileHistory != null && fileHistory.getFileId().equals(fileHistoryId)) { // Same history!
					fileHistory.addFileVersion(lastFileVersion);
			}
			else { // New history!
				fileHistory = new PartialFileHistory(fileHistoryId);
				fileHistory.addFileVersion(lastFileVersion);
			}				
				
			fileHistories.add(fileHistory);
		}

		return fileHistories;
	}

	public PartialFileHistory getFileHistoryWithFileVersions(String relativePath) {
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getFileHistoryWithFileVersions.sql");

			preparedStatement.setString(1, relativePath);
			preparedStatement.setString(2, FileStatus.DELETED.toString());

			ResultSet resultSet = preparedStatement.executeQuery();

			PartialFileHistory fileHistory = null;				
			
			while (resultSet.next()) {
				if (fileHistory == null) {
					FileHistoryId fileHistoryId = FileHistoryId.parseFileId(resultSet.getString("filehistory_id"));
					fileHistory = new PartialFileHistory(fileHistoryId);
				}
				
				FileVersion fileVersion = createFileVersionFromRow(resultSet);				
				fileHistory.addFileVersion(fileVersion);
			}

			return fileHistory;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}

	public DatabaseVersionHeader getLastDatabaseVersionHeader() {
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getLastDatabaseVersionHeader.sql");
			preparedStatement.setMaxRows(1);

			ResultSet resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				DatabaseVersionHeader databaseVersionHeader = createDatabaseVersionHeaderFromRow(resultSet);
				return databaseVersionHeader;
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

	public MultiChunkEntry getMultiChunkForChunk(ChunkChecksum chunkChecksum) {
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getMultiChunkForChunk.sql");
			preparedStatement.setString(1, chunkChecksum.toString());
					
			ResultSet resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				MultiChunkId multiChunkId = MultiChunkId.parseMultiChunkId(resultSet.getString("multichunk_id"));
				MultiChunkEntry multiChunkEntry = new MultiChunkEntry(multiChunkId);
				
				return multiChunkEntry;
			}

			return null;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public MultiChunkEntry getMultiChunkWithStatus(MultiChunkId multiChunkId, DatabaseVersionStatus status) {
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getMultiChunkWithStatus.sql");
			
			preparedStatement.setString(1, status.toString());
			preparedStatement.setString(2, multiChunkId.toString());
					
			ResultSet resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				MultiChunkEntry multiChunkEntry = new MultiChunkEntry(multiChunkId);				
				return multiChunkEntry;
			}

			return null;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public DatabaseBranch getLocalDatabaseBranch() {
		DatabaseBranch databaseBranch = new DatabaseBranch();
		
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getLocalDatabaseBranch.sql");
			ResultSet resultSet = preparedStatement.executeQuery();
			
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
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}	
	
	public List<String> getKnownDatabases() {
		List<String> knownDatabases = new ArrayList<String>();
		
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getKnownDatabases.sql");					
			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				knownDatabases.add(resultSet.getString("database_name"));
			}

			return knownDatabases;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void shutdown() {
		try {
			connection.prepareStatement("shutdown").execute();
		}
		catch (SQLException e) {
			// Don't care
		}
		finally {
			try {
				connection.close();
			}
			catch (SQLException e) {
				// Don't care
			}
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

		return vectorClock;
	}

	protected FileVersion createFileVersionFromRow(ResultSet resultSet) throws SQLException {
		FileVersion fileVersion = new FileVersion();

		fileVersion.setVersion(resultSet.getLong("version"));
		fileVersion.setPath(resultSet.getString("path"));
		fileVersion.setType(FileType.valueOf(resultSet.getString("type")));
		fileVersion.setStatus(FileStatus.valueOf(resultSet.getString("status")));
		fileVersion.setSize(resultSet.getLong("size"));
		fileVersion.setLastModified(new Date(resultSet.getTimestamp("lastmodified").getTime()));

		if (resultSet.getString("linktarget") != null) {
			fileVersion.setLinkTarget(resultSet.getString("linktarget"));
		}

		if (resultSet.getString("filecontent_checksum") != null) {
			FileChecksum fileChecksum = FileChecksum.parseFileChecksum(resultSet.getString("filecontent_checksum"));
			fileVersion.setChecksum(fileChecksum);
		}

		if (resultSet.getString("updated") != null) {
			fileVersion.setUpdated(new Date(resultSet.getTimestamp("updated").getTime()));
		}

		if (resultSet.getString("posixperms") != null) {
			fileVersion.setPosixPermissions(resultSet.getString("posixperms"));
		}

		if (resultSet.getString("dosattrs") != null) {
			fileVersion.setDosAttributes(resultSet.getString("dosattrs"));
		}

		return fileVersion;
	}
	
	protected PreparedStatement getStatement(String resourceId) throws SQLException {
		return connection.prepareStatement(DatabaseConnectionFactory.getStatement(resourceId));
	}
}
