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
package org.syncany.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.operations.DatabaseBranch;

/**
 * @author pheckel
 *
 */
public class BasicDatabaseDAO {
	protected static final Logger logger = Logger.getLogger(BasicDatabaseDAO.class.getSimpleName());
	protected Connection connection;

	public BasicDatabaseDAO(Connection connection) {
		this.connection = connection;
	}

	public Map<String, FileVersion> getCurrentFileTree() {
		Map<String, FileVersion> currentFileTree = new HashMap<String, FileVersion>();

		try {
			PreparedStatement preparedStatement = connection.prepareStatement(
					"select * from fileversion fv "
					+ "where fv.status<>'DELETED' "
					+ "  and fv.version=(select max(fv1.version) from fileversion fv1 where fv.filehistory_id=fv1.filehistory_id)");

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

	public FileVersion getFileVersionByPath(String path) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement(
					"select * from fileversion fv " 
					+ "where fv.path=? "
					+ "and fv.status<>'DELETED' "
					+ "and fv.version=(select max(fv1.version) from fileversion fv1 where fv.filehistory_id=fv1.filehistory_id)");

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
			PreparedStatement preparedStatement = connection.prepareStatement(
					"select * from fileversion fv " 
					+ "where fv.filehistory_id=? "
					+ "and fv.status<>'DELETED' "
					+ "and fv.version=(select max(fv1.version) from fileversion fv1 where fv.filehistory_id=fv1.filehistory_id)");

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
		try {
			PreparedStatement preparedStatement = connection.prepareStatement(
				  "select distinct mcc.multichunk_id "
				+ "from filecontent fc "
				+ "join filecontent_chunk fcc on fc.checksum=fcc.filecontent_checksum "
				+ "join multichunk_chunk mcc on fcc.chunk_checksum=mcc.chunk_checksum "
				+ "where fc.checksum=?");
			
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
	
	public FileContent getFileContentByChecksum(FileChecksum fileChecksum, boolean includeChunkChecksums) {
		if (fileChecksum == null) {
			return null;
		}
		else if (includeChunkChecksums) {
			try {
				PreparedStatement preparedStatement = connection.prepareStatement(
					  "select fc.checksum, fc.size, fcc.chunk_checksum, fcc.num "
					+ "from filecontent fc "
					+ "join filecontent_chunk fcc on fc.checksum=fcc.filecontent_checksum "
					+ "where fc.checksum=? "
					+ "order by fcc.num asc");
				
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
		else {
			try {
				PreparedStatement preparedStatement = connection.prepareStatement("select * from filecontent fc where fc.checksum=?");
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
	}

	public List<PartialFileHistory> getFileHistoriesWithFileVersions() {
		List<PartialFileHistory> currentFileTree = new ArrayList<PartialFileHistory>();

		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select * from fileversion order by filehistory_id, version");
			ResultSet resultSet = preparedStatement.executeQuery();

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
					
				currentFileTree.add(fileHistory);
			}

			return currentFileTree;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public DatabaseVersionHeader getLastDatabaseVersionHeader() {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select * from databaseversion order by id desc");
			preparedStatement.setMaxRows(1);

			ResultSet resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				DatabaseVersionHeader databaseVersionHeader = new DatabaseVersionHeader();

				databaseVersionHeader.setClient(resultSet.getString("client"));
				databaseVersionHeader.setDate(new Date(resultSet.getDate("localtime").getTime()));
				databaseVersionHeader.setVectorClock(getVectorClockByDatabaseVersionId(resultSet.getInt("id")));

				return databaseVersionHeader;
			}

			return null;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public MultiChunkEntry getMultiChunkForChunk(ChunkChecksum chunkChecksum) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement(
				  "select mcc.multichunk_id "
				+ "from multichunk_chunk mcc "
				+ "join multichunk mc on mc.id=mcc.multichunk_id "
				+ "where mcc.chunk_checksum=?"
			);
			
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

	public DatabaseBranch getLocalDatabaseBranch() {
		DatabaseBranch databaseBranch = new DatabaseBranch();
		
		try {
			PreparedStatement preparedStatement = connection.prepareStatement(
				  "select dbv.id, dbv.localtime, dbv.client, vc.client as vc_client, vc.logicaltime as vc_logicaltime "
				+ "from databaseversion dbv "
				+ "join vectorclock vc on vc.databaseversion_id=dbv.id "
				+ "order by dbv.id asc, vc.client");
					
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
					currentDatabaseVersionHeader.setDate(new Date(resultSet.getDate("localtime").getTime()));					

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
	

	public DatabaseVersion getDatabaseVersion(VectorClock vectorClock) {
		throw new RuntimeException("Not implemented");
	}
	

	public void removeDatabaseVersion(DatabaseVersion databaseVersion) {
		throw new RuntimeException("Not implemented");
	}
	
	protected VectorClock getVectorClockByDatabaseVersionId(int databaseVersionId) throws SQLException {
		PreparedStatement preparedStatement = connection.prepareStatement("select * from vectorclock where databaseversion_id=?");
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
}
