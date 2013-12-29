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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.syncany.database.ChunkEntry;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.PartialFileHistory.FileHistoryId;

/**
 * @author pheckel
 *
 */
public class IndexSqlDatabaseDAO extends SqlDatabaseDAO {
	private Map<ChunkChecksum, ChunkEntry> chunkCache; 
	
	public IndexSqlDatabaseDAO(Connection connection) {
		super(connection);		
		this.chunkCache = null;
	}
	
	public ChunkEntry getChunk(ChunkChecksum chunkChecksum) {
		if (chunkCache == null) {
			loadChunkCache();
		}
		
		return chunkCache.get(chunkChecksum);
	}
	
	private void loadChunkCache() {
		chunkCache = new HashMap<ChunkChecksum, ChunkEntry>();
		
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select * from chunk");
			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				ChunkChecksum chunkChecksum = ChunkChecksum.parseChunkChecksum(resultSet.getString("checksum"));
				ChunkEntry chunkEntry = new ChunkEntry(chunkChecksum, resultSet.getInt("size"));
				
				chunkCache.put(chunkChecksum, chunkEntry);
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}

	public PartialFileHistory getFileHistoryWithLastVersion(FileHistoryId fileHistoryId) {
		FileVersion lastFileVersion = getFileVersionByFileHistoryId(fileHistoryId);
		
		if (lastFileVersion != null) {
			PartialFileHistory fileHistory = new PartialFileHistory(fileHistoryId);				
			fileHistory.addFileVersion(lastFileVersion);
			
			return fileHistory;
		}

		return null;
	}
	
	public PartialFileHistory getFileHistoryWithLastVersion(String relativePath) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement(
					  "select * from fileversion fv " 
					+ "where fv.path=? "
					+ "and fv.status<>'DELETED' "
					+ "and fv.version=(select max(fv1.version) from fileversion fv1 where fv.filehistory_id=fv1.filehistory_id)");

			preparedStatement.setString(1, relativePath);

			ResultSet resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				FileHistoryId fileHistoryId = FileHistoryId.parseFileId(resultSet.getString("filehistory_id"));
				FileVersion lastFileVersion = createFileVersionFromRow(resultSet);
				
				PartialFileHistory fileHistory = new PartialFileHistory(fileHistoryId);				
				fileHistory.addFileVersion(lastFileVersion);
				
				return fileHistory;
			}

			return null;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}
	
	public List<PartialFileHistory> getFileHistoriesWithLastVersion() {
		List<PartialFileHistory> currentFileTree = new ArrayList<PartialFileHistory>();

		try {
			PreparedStatement preparedStatement = connection.prepareStatement(
					  "select * from fileversion fv "
					+ "where fv.status<>'DELETED' "
					+ "  and fv.version=(select max(fv1.version) from fileversion fv1 where fv.filehistory_id=fv1.filehistory_id)");

			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				FileHistoryId fileHistoryId = FileHistoryId.parseFileId(resultSet.getString("filehistory_id"));
				FileVersion lastFileVersion = createFileVersionFromRow(resultSet);
				
				PartialFileHistory fileHistory = new PartialFileHistory(fileHistoryId);				
				fileHistory.addFileVersion(lastFileVersion);
				
				currentFileTree.add(fileHistory);
			}

			return currentFileTree;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}

	public List<PartialFileHistory> getFileHistoriesWithLastVersionByChecksum(FileChecksum fileContentChecksum) {
		List<PartialFileHistory> currentFileTree = new ArrayList<PartialFileHistory>();

		try {
			PreparedStatement preparedStatement = connection.prepareStatement(
					  "select * from fileversion fv "
					+ "where fv.status<>'DELETED' "
					+ "  and fv.version=(select max(fv1.version) from fileversion fv1 where fv.filehistory_id=fv1.filehistory_id)"
					+ "  and fv.filecontent_checksum=?");

			preparedStatement.setString(1, fileContentChecksum.toString());
			
			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				FileHistoryId fileHistoryId = FileHistoryId.parseFileId(resultSet.getString("filehistory_id"));
				FileVersion lastFileVersion = createFileVersionFromRow(resultSet);
				
				PartialFileHistory fileHistory = new PartialFileHistory(fileHistoryId);				
				fileHistory.addFileVersion(lastFileVersion);
				
				currentFileTree.add(fileHistory);
			}

			return currentFileTree;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}

}
