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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.PartialFileHistory.FileHistoryId;

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

	public FileContent getFileContentByChecksum(FileChecksum fileChecksum) {
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
