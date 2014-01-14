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
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.PartialFileHistory.FileHistoryId;

/**
 * @author pheckel
 *
 */
public class FileVersionSqlDao extends AbstractSqlDao {
	protected static final Logger logger = Logger.getLogger(FileVersionSqlDao.class.getSimpleName());
	
	public FileVersionSqlDao(Connection connection) {
		super(connection);
	}
	
	public void writeFileVersions(Connection connection, FileHistoryId fileHistoryId, long databaseVersionId, Collection<FileVersion> fileVersions) throws SQLException {
		PreparedStatement preparedStatement = getStatement(connection, "/sql/insert.writeFileVersions.sql");

		for (FileVersion fileVersion : fileVersions) {
			String fileContentChecksumStr = (fileVersion.getChecksum() != null) ? fileVersion.getChecksum().toString() : null;					  		

			preparedStatement.setString(1, fileHistoryId.toString());
			preparedStatement.setInt(2, Integer.parseInt(""+fileVersion.getVersion()));
			preparedStatement.setLong(3, databaseVersionId);
			preparedStatement.setString(4, fileVersion.getPath());
			preparedStatement.setString(5, fileVersion.getType().toString());
			preparedStatement.setString(6, fileVersion.getStatus().toString());
			preparedStatement.setLong(7, fileVersion.getSize());
			preparedStatement.setTimestamp(8, new Timestamp(fileVersion.getLastModified().getTime()));
			preparedStatement.setString(9, fileVersion.getLinkTarget());
			preparedStatement.setString(10, fileContentChecksumStr);
			preparedStatement.setTimestamp(11, new Timestamp(fileVersion.getUpdated().getTime()));
			preparedStatement.setString(12, fileVersion.getPosixPermissions());
			preparedStatement.setString(13, fileVersion.getDosAttributes());
			
			preparedStatement.addBatch();
		}				
		
		preparedStatement.executeBatch();
	}
	
	public Map<String, FileVersion> getCurrentFileTree() {		
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getCurrentFileTree.sql");		
			return getFileTree(preparedStatement);				
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Map<String, FileVersion> getFileTreeAtDate(Date date) {		
		try {
			PreparedStatement preparedStatement = getStatement("/sql/select.getFileTreeAtDate.sql");
			preparedStatement.setTimestamp(1, new Timestamp(date.getTime()));
			
			return getFileTree(preparedStatement);					
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Map<String, FileVersion> getFileTree(PreparedStatement preparedStatement) {
		Map<String, FileVersion> fileTree = new HashMap<String, FileVersion>();

		try {
			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				FileVersion fileVersion = createFileVersionFromRow(resultSet);
				fileTree.put(fileVersion.getPath(), fileVersion);
			}

			return fileTree;
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
