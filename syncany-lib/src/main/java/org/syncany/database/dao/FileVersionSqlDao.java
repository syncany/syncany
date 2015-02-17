/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.operations.cleanup.CleanupOperationOptions.TimeUnit;
import org.syncany.util.StringUtil;

import com.google.common.collect.ImmutableMap;

/**
 * The file version DAO queries and modifies the <i>fileversion</i> in
 * the SQL database. This table corresponds to the Java object {@link FileVersion}.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FileVersionSqlDao extends AbstractSqlDao {
	private static final Logger logger = Logger.getLogger(FileVersionSqlDao.class.getSimpleName());	
	private static final Map<TimeUnit, String> timeUnitSqlTimeUnitMap = new ImmutableMap.Builder<TimeUnit, String>()
           .put(TimeUnit.SECONDS, "SS")
           .put(TimeUnit.MINUTES, "MI")
           .put(TimeUnit.HOURS, "HH")
           .put(TimeUnit.DAYS, "DD")
           .put(TimeUnit.WEEKS, "WW")
           .put(TimeUnit.MONTHS, "MM")
           .put(TimeUnit.YEARS, "YYY")
           .build();	
	
	public FileVersionSqlDao(Connection connection) {
		super(connection);
	}

	/**
	 * Writes a list of {@link FileVersion} to the database table <i>fileversion</i> using <tt>INSERT</tt>s
	 * and the given connection.
	 *
	 * <p><b>Note:</b> This method executes, but <b>does not commit</b> the queries.
	 *
	 * @param connection The connection used to execute the statements
	 * @param fileHistoryId References the {@link PartialFileHistory} to which the list of file versions belongs
	 * @param databaseVersionId References the {@link PartialFileHistory} to which the list of file versions belongs
	 * @param fileVersions List of {@link FileVersion}s to be written to the database
	 * @throws SQLException If the SQL statement fails
	 */
	public void writeFileVersions(Connection connection, FileHistoryId fileHistoryId, long databaseVersionId, Collection<FileVersion> fileVersions)
			throws SQLException {
		PreparedStatement preparedStatement = getStatement(connection, "fileversion.insert.writeFileVersions.sql");

		for (FileVersion fileVersion : fileVersions) {
			String fileContentChecksumStr = (fileVersion.getChecksum() != null) ? fileVersion.getChecksum().toString() : null;

			preparedStatement.setString(1, fileHistoryId.toString());
			preparedStatement.setInt(2, Integer.parseInt("" + fileVersion.getVersion()));
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
		preparedStatement.close();
	}

	/**
	 * Removes {@link FileVersion}s from the database table <i>fileversion</i> for which the
	 * the corresponding database is marked <tt>DIRTY</tt>.
	 *
	 * <p><b>Note:</b> This method executes, but does not commit the query.
	 *
	 * @throws SQLException If the SQL statement fails
	 */
	public void removeDirtyFileVersions() throws SQLException {
		try (PreparedStatement preparedStatement = getStatement("fileversion.delete.dirty.removeDirtyFileVersions.sql")) {
			preparedStatement.executeUpdate();
		}
	}

	/**
	 * Removes all file versions with versions <b>lower or equal</b> than the given file version.
	 *
	 * <p>Note that this method does not just delete the given file version, but also all of its
	 * previous versions.
	 */
	public void removeFileVersions(Map<FileHistoryId, FileVersion> purgeFileVersions) throws SQLException {
		if (purgeFileVersions.size() > 0) {
			try (PreparedStatement preparedStatement = getStatement(connection, "fileversion.delete.all.removeFileVersionsByIds.sql")) {
				for (Map.Entry<FileHistoryId, FileVersion> purgeFileVersionEntry : purgeFileVersions.entrySet()) {
					FileHistoryId purgeFileHistoryId = purgeFileVersionEntry.getKey();
					FileVersion purgeFileVersion = purgeFileVersionEntry.getValue();

					preparedStatement.setString(1, purgeFileHistoryId.toString());
					preparedStatement.setLong(2, purgeFileVersion.getVersion());

					preparedStatement.addBatch();
				}

				preparedStatement.executeBatch();
			}
		}
	}

	public void removeSpecificFileVersions(Map<FileHistoryId, List<FileVersion>> purgeFileVersions) throws SQLException {
		if (purgeFileVersions.size() > 0) {
			try (PreparedStatement preparedStatement = getStatement(connection, "fileversion.delete.all.removeSpecificFileVersionsByIds.sql")) {
				for (FileHistoryId purgeFileHistoryId : purgeFileVersions.keySet()) {
					for (FileVersion purgeFileVersion : purgeFileVersions.get(purgeFileHistoryId)) {
						preparedStatement.setString(1, purgeFileHistoryId.toString());
						preparedStatement.setLong(2, purgeFileVersion.getVersion());

						preparedStatement.addBatch();
					}
				}

				preparedStatement.executeBatch();
			}
		}
	}

	/**
	 * Queries the database for the currently active {@link FileVersion}s and returns it
	 * as a map. If the current file tree (on the disk) has not changed, the result will
	 * match the files on the disk.
	 *
	 * <p>Keys in the returned map correspond to the file version's relative file path,
	 * and values to the actual {@link FileVersion} object.
	 *
	 * @return Returns the current file tree as a map of relative paths to {@link FileVersion} objects
	 */
	public Map<String, FileVersion> getCurrentFileTree() {
		try (PreparedStatement preparedStatement = getStatement("fileversion.select.master.getCurrentFileTree.sql")) {
			Map<String, FileVersion> fileTree = new TreeMap<>();
			List<FileVersion> fileList = getFileTree(preparedStatement);
			
			for (FileVersion fileVersion : fileList) {
				fileTree.put(fileVersion.getPath(), fileVersion);
			}
			
			return fileTree;
			
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public List<FileVersion> getFileHistory(FileHistoryId fileHistoryId) {
		try (PreparedStatement preparedStatement = getStatement("fileversion.select.master.getFileHistoryById.sql")) {
			preparedStatement.setString(1, fileHistoryId.toString());

			List<FileVersion> fileTree = new ArrayList<FileVersion>();

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					FileVersion fileVersion = createFileVersionFromRow(resultSet);
					fileTree.add(fileVersion);
				}

				return fileTree;
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public List<FileVersion> getFileList(String pathExpression, Date date, boolean fileHistoryId, boolean recursive, boolean deleted,
			Set<FileType> fileTypes) {
		
		// Determine sensible query parameters
		// Basic idea: If null/empty given, match them all!

		String fileHistoryPrefix = null;
		
		if (fileHistoryId) {
			fileHistoryPrefix = (pathExpression == null || "".equals(pathExpression)) ? "%" : pathExpression;
			pathExpression = "%";
		}
		else {
			fileHistoryPrefix = "%";
			pathExpression = (pathExpression == null || "".equals(pathExpression)) ? "%" : pathExpression;
		}
		
		date = (date == null) ? new Date(4133984461000L) : date;

		int slashCount = StringUtil.substrCount(pathExpression, "/");
		int filterMinSlashCount = (recursive || fileHistoryId) ? 0 : slashCount;
		int filterMaxSlashCount = (recursive || fileHistoryId) ? Integer.MAX_VALUE : slashCount;

		String[] fileTypesStr = createFileTypesArray(fileTypes);		
		String fileStatusNotEqualTo = (deleted) ? "INVALID" : FileStatus.DELETED.toString(); 
		
		if (logger.isLoggable(Level.INFO)) {
			logger.log(Level.INFO, " getFileTree(path = " + pathExpression + ", history = " + fileHistoryPrefix + ", minSlash = "
					+ filterMinSlashCount + ", maxSlash = " + filterMaxSlashCount + ", date <= " + date + ", types = " 
					+ StringUtil.join(fileTypesStr, ", "));
		}

		try (PreparedStatement preparedStatement = getStatement("fileversion.select.master.getFilteredFileTree.sql")) {
			preparedStatement.setString(1, fileStatusNotEqualTo);
			preparedStatement.setString(2, pathExpression);
			preparedStatement.setString(3, fileHistoryPrefix);
			preparedStatement.setInt(4, filterMinSlashCount);
			preparedStatement.setInt(5, filterMaxSlashCount);
			preparedStatement.setArray(6, connection.createArrayOf("varchar", fileTypesStr));
			preparedStatement.setTimestamp(7, new Timestamp(date.getTime()));

			return getFileTree(preparedStatement);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private String[] createFileTypesArray(Set<FileType> fileTypes) {
		String[] fileTypesStr = null;

		if (fileTypes != null) {
			fileTypesStr = new String[fileTypes.size()];

			int i = 0;

			for (Iterator<FileType> fileTypeIterator = fileTypes.iterator(); fileTypeIterator.hasNext();) {
				fileTypesStr[i++] = fileTypeIterator.next().toString();
			}
		}
		else {
			fileTypesStr = new String[] { FileType.FILE.toString(), FileType.FOLDER.toString(), FileType.SYMLINK.toString() };
		}

		return fileTypesStr;
	}

	public Map<FileHistoryId, FileVersion> getFileHistoriesWithMaxPurgeVersion(int keepVersionsCount) {
		try (PreparedStatement preparedStatement = getStatement("fileversion.select.all.getMaxPurgeVersions.sql")) {
			preparedStatement.setInt(1, keepVersionsCount);
			preparedStatement.setInt(2, keepVersionsCount);

			return getSingleVersionInHistory(preparedStatement);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Map<FileHistoryId, List<FileVersion>> getFileHistoriesToPurgeInInterval(long beginTimestamp, long endTimestamp, TimeUnit timeUnit) {
		try (PreparedStatement preparedStatement = getStatement("fileversion.select.all.getPurgeVersionsByInterval.sql")) {
			String timeUnitIdentifier = timeUnitSqlTimeUnitMap.get(timeUnit);
			
			preparedStatement.setString(1, timeUnitIdentifier);
			preparedStatement.setTimestamp(2, new Timestamp(beginTimestamp));
			preparedStatement.setTimestamp(3, new Timestamp(endTimestamp));
			
			return getAllVersionsInQuery(preparedStatement);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Map<FileHistoryId, List<FileVersion>> getFileHistoriesToPurgeBefore(long timestamp) {
		try (PreparedStatement preparedStatement = getStatement("fileversion.select.all.getPurgeVersionsBeforeTime.sql")) {
			preparedStatement.setTimestamp(1, new Timestamp(timestamp));
			return getAllVersionsInQuery(preparedStatement);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Map<FileHistoryId, FileVersion> getDeletedFileVersions() {
		try (PreparedStatement preparedStatement = getStatement("fileversion.select.all.getDeletedFileVersions.sql")) {
			return getSingleVersionInHistory(preparedStatement);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Map<FileHistoryId, FileVersion> getDeletedFileVersionsBefore(long timestamp) {
		try (PreparedStatement preparedStatement = getStatement("fileversion.select.all.getDeletedFileVersionsBefore.sql")) {
			preparedStatement.setTimestamp(1, new Timestamp(timestamp));
			return getSingleVersionInHistory(preparedStatement);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		
	}

	public FileVersion getFileVersion(FileHistoryId fileHistoryId, long version) {
		try (PreparedStatement preparedStatement = getStatement("fileversion.select.master.getFileVersionByHistoryAndVersion.sql")) {
			preparedStatement.setString(1, fileHistoryId.toString());
			preparedStatement.setLong(2, version);

			return executeAndCreateFileVersion(preparedStatement);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<FileHistoryId, FileVersion> getSingleVersionInHistory(PreparedStatement preparedStatement) throws SQLException {
		try (ResultSet resultSet = preparedStatement.executeQuery()) {
			Map<FileHistoryId, FileVersion> mostRecentPurgeFileVersions = new HashMap<FileHistoryId, FileVersion>();

			while (resultSet.next()) {
				FileHistoryId fileHistoryId = FileHistoryId.parseFileId(resultSet.getString("filehistory_id"));
				FileVersion fileVersion = createFileVersionFromRow(resultSet);

				mostRecentPurgeFileVersions.put(fileHistoryId, fileVersion);
			}

			return mostRecentPurgeFileVersions;
		}
	}

	private Map<FileHistoryId, List<FileVersion>> getAllVersionsInQuery(PreparedStatement preparedStatement) throws SQLException {
		try (ResultSet resultSet = preparedStatement.executeQuery()) {
			Map<FileHistoryId, List<FileVersion>> fileHistoryPurgeFileVersions = new HashMap<FileHistoryId, List<FileVersion>>();

			while (resultSet.next()) {
				FileHistoryId fileHistoryId = FileHistoryId.parseFileId(resultSet.getString("filehistory_id"));
				FileVersion fileVersion = createFileVersionFromRow(resultSet);

				List<FileVersion> purgeFileVersions = fileHistoryPurgeFileVersions.get(fileHistoryId);
				
				if (purgeFileVersions == null) {
					purgeFileVersions = new ArrayList<FileVersion>();
					fileHistoryPurgeFileVersions.put(fileHistoryId, purgeFileVersions);
				}
				
				purgeFileVersions.add(fileVersion);
			}

			return fileHistoryPurgeFileVersions;
		}
	}

	private List<FileVersion> getFileTree(PreparedStatement preparedStatement) {
		List<FileVersion> fileTree = new ArrayList<>();

		try (ResultSet resultSet = preparedStatement.executeQuery()) {
			while (resultSet.next()) {
				FileVersion fileVersion = createFileVersionFromRow(resultSet);
				fileTree.add(fileVersion);
			}

			return fileTree;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private FileVersion executeAndCreateFileVersion(PreparedStatement preparedStatement) {
		try (ResultSet resultSet = preparedStatement.executeQuery()) {
			if (resultSet.next()) {
				return createFileVersionFromRow(resultSet);
			}
			else {
				return null;
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	// TODO [low] This should be private; but it has to be public for a test
	public FileVersion createFileVersionFromRow(ResultSet resultSet) throws SQLException {
		FileVersion fileVersion = new FileVersion();

		fileVersion.setFileHistoryId(FileHistoryId.parseFileId(resultSet.getString("filehistory_id")));
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
