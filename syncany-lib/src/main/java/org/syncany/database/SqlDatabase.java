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
package org.syncany.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.DatabaseVersion.DatabaseVersionStatus;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.dao.ApplicationSqlDao;
import org.syncany.database.dao.ChunkSqlDao;
import org.syncany.database.dao.DatabaseVersionSqlDao;
import org.syncany.database.dao.FileContentSqlDao;
import org.syncany.database.dao.FileHistorySqlDao;
import org.syncany.database.dao.FileVersionSqlDao;
import org.syncany.database.dao.MultiChunkSqlDao;
import org.syncany.operations.DatabaseBranch;

/**
 * Represents the single entry point for all SQL database queries.
 * 
 * <p>This class combines all specific SQL database data access objects (DAOs) into
 * a single class, and forwards all method calls to the responsible DAO.  
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class SqlDatabase {
	protected static final Logger logger = Logger.getLogger(SqlDatabase.class.getSimpleName());

	protected ApplicationSqlDao applicationDao;
	protected ChunkSqlDao chunkDao;
	protected FileContentSqlDao fileContentDao;
	protected FileVersionSqlDao fileVersionDao;
	protected FileHistorySqlDao fileHistoryDao;
	protected MultiChunkSqlDao multiChunkDao;
	protected DatabaseVersionSqlDao databaseVersionDao;

	public SqlDatabase(Config config) {
		Connection connection = config.createDatabaseConnection();

		this.applicationDao = new ApplicationSqlDao(connection);
		this.chunkDao = new ChunkSqlDao(connection);
		this.fileContentDao = new FileContentSqlDao(connection);
		this.fileVersionDao = new FileVersionSqlDao(connection);
		this.fileHistoryDao = new FileHistorySqlDao(connection, fileVersionDao);
		this.multiChunkDao = new MultiChunkSqlDao(connection);
		this.databaseVersionDao = new DatabaseVersionSqlDao(connection, chunkDao, fileContentDao, fileHistoryDao, multiChunkDao);
	}

	// Application

	public void persistNewKnownRemoteDatabases(List<DatabaseRemoteFile> remoteDatabases) throws SQLException {
		applicationDao.persistNewKnownRemoteDatabases(remoteDatabases);
	}

	public List<String> getKnownDatabases() {
		return applicationDao.getKnownDatabases();
	}

	public void shutdown() {
		applicationDao.shutdown();
	}

	// Database version

	public Iterator<DatabaseVersion> getDatabaseVersions(DatabaseVersionStatus status) {
		return databaseVersionDao.getDatabaseVersions(status);
	}

	public Iterator<DatabaseVersion> getDatabaseVersionsTo(String machineName, long maxLocalClientVersion) {
		return databaseVersionDao.getDatabaseVersionsTo(machineName, maxLocalClientVersion);
	}

	public DatabaseVersionHeader getLastDatabaseVersionHeader() {
		return databaseVersionDao.getLastDatabaseVersionHeader();
	}

	public DatabaseBranch getLocalDatabaseBranch() {
		return databaseVersionDao.getLocalDatabaseBranch();
	}

	public void persistDatabaseVersion(DatabaseVersion databaseVersion) {
		databaseVersionDao.persistDatabaseVersion(databaseVersion);
	}

	public void markDatabaseVersion(DatabaseVersionHeader databaseVersionHeader, DatabaseVersionStatus status) {
		databaseVersionDao.markDatabaseVersion(databaseVersionHeader, status);
	}

	public void removeDirtyDatabaseVersions() {
		databaseVersionDao.removeDirtyDatabaseVersions();
	}

	public Long getMaxDirtyVectorClock(String machineName) {
		return databaseVersionDao.getMaxDirtyVectorClock(machineName);
	}

	// File History

	@Deprecated
	public List<PartialFileHistory> getFileHistoriesForDatabaseVersion(VectorClock databaseVersionVectorClock) {
		// TODO [medium] This is not used anywhere, remove it!
		return fileHistoryDao.getFileHistoriesWithFileVersions(databaseVersionVectorClock);
	}

	@Deprecated	
	public List<PartialFileHistory> getFileHistoriesWithFileVersions() {
		// TODO [medium] Note: This returns the full database. Don't use this!
		return fileHistoryDao.getFileHistoriesWithFileVersions();
	}

	public PartialFileHistory getFileHistoryWithLastVersion(FileHistoryId fileHistoryId) {
		return fileHistoryDao.getFileHistoryWithLastVersion(fileHistoryId);
	}

	public PartialFileHistory getFileHistoryWithLastVersion(String relativePath) {
		return fileHistoryDao.getFileHistoryWithLastVersion(relativePath);
	}

	public List<PartialFileHistory> getFileHistoriesWithLastVersion() {
		return fileHistoryDao.getFileHistoriesWithLastVersion();
	}

	public List<PartialFileHistory> getFileHistoriesWithLastVersionByChecksum(FileChecksum fileContentChecksum) {
		return fileHistoryDao.getFileHistoriesWithLastVersionByChecksum(fileContentChecksum);
	}

	// File Version

	public Map<String, FileVersion> getFilesInDatabase() {
		return fileVersionDao.getCurrentFileTree();
	}

	public FileVersion getFileVersionByPath(String path) {
		return fileVersionDao.getFileVersionByPath(path);
	}

	public FileVersion getFileVersionByFileHistoryId(FileHistoryId fileHistoryId) {
		return fileVersionDao.getFileVersionByFileHistoryId(fileHistoryId);
	}

	public Map<String, FileVersion> getFileTreeAtDate(Date date) {
		return fileVersionDao.getFileTreeAtDate(date);
	}

	// Multi Chunk

	public List<MultiChunkEntry> getMultiChunksForFileChecksum(FileChecksum fileChecksum) {
		return multiChunkDao.getMultiChunksForFileChecksum(fileChecksum);
	}

	public MultiChunkEntry getMultiChunkForChunk(ChunkChecksum chunkChecksum) {
		return multiChunkDao.getMultiChunkForChunk(chunkChecksum);
	}

	public MultiChunkEntry getMultiChunkWithStatus(MultiChunkId multiChunkId, DatabaseVersionStatus status) {
		return multiChunkDao.getMultiChunkWithStatus(multiChunkId, status);
	}

	// Chunk

	protected Map<ChunkChecksum, ChunkEntry> getChunksForDatabaseVersion(VectorClock vectorClock) {
		return chunkDao.getChunksForDatabaseVersion(vectorClock);
	}

	public ChunkEntry getChunk(ChunkChecksum chunkChecksum) {
		return chunkDao.getChunk(chunkChecksum);
	}

	// File Content

	public FileContent getFileContentByChecksum(FileChecksum fileChecksum, boolean includeChunkChecksums) {
		return fileContentDao.getFileContent(fileChecksum, includeChunkChecksums);
	}
}
