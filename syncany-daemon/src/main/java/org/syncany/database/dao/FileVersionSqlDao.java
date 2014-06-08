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
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;

public class FileVersionSqlDao extends AbstractSqlDao {
	protected static final Logger logger = Logger.getLogger(FileVersionSqlDao.class.getSimpleName());
	
	public FileVersionSqlDao(Connection connection) {
		super(connection);
	}
	
	public List<DatabaseVersionHeader> getDatabaseVersionHeaders() {
		// for the date picker
	}
	
	public Map<String, ExtendedFileVersion> getFileTree(String prefix, Date date, FileType fileType) {
		try (PreparedStatement preparedStatement = getStatement("fileversion.select.master.getCurrentFileTreeWithPrefix.sql")) {
			preparedStatement.setString(1, prefix);
			preparedStatement.setString(2, prefix);
			preparedStatement.setString(3, prefix);
			
			return getFileTree(preparedStatement);				
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Map<String, ExtendedFileVersion> getFileTreeAtDate(Date date) {		
		try (PreparedStatement preparedStatement = getStatement("fileversion.select.master.getFileTreeAtDate.sql")) {
			preparedStatement.setTimestamp(1, new Timestamp(date.getTime()));
			preparedStatement.setTimestamp(2, new Timestamp(date.getTime()));
			
			return getFileTree(preparedStatement);					
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	private Map<String, ExtendedFileVersion> getFileTree(PreparedStatement preparedStatement) {
		Map<String, ExtendedFileVersion> fileTree = new HashMap<String, ExtendedFileVersion>();

		try (ResultSet resultSet = preparedStatement.executeQuery()) {
			while (resultSet.next()) {
				ExtendedFileVersion fileVersion = createFileVersionFromRow(resultSet);
				fileTree.put(fileVersion.getPath(), fileVersion);
			}

			return fileTree;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}		
	
	// TODO [low] This should be private; but it has to be public for a test
	public ExtendedFileVersion createFileVersionFromRow(ResultSet resultSet) throws SQLException {
		ExtendedFileVersion fileVersion = new ExtendedFileVersion();

		fileVersion.setFileHistoryId(FileHistoryId.parseFileId(resultSet.get);
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
