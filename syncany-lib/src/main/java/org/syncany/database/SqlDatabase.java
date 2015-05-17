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
package org.syncany.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.dao.ApplicationSqlDao;
import org.syncany.database.dao.ChunkSqlDao;
import org.syncany.database.dao.DatabaseVersionSqlDao;
import org.syncany.database.dao.FileContentSqlDao;
import org.syncany.database.dao.FileHistorySqlDao;
import org.syncany.database.dao.FileVersionSqlDao;
import org.syncany.database.dao.MultiChunkSqlDao;
import org.syncany.operations.cleanup.CleanupOperationOptions.TimeUnit;
import org.syncany.operations.down.DatabaseBranch;
import org.syncany.plugins.transfer.files.DatabaseRemoteFile;

/**
 * Represents the single entry point for all SQL database queries.
 * 
 * <p>This class combines all specific SQL database data access objects (DAOs) into
 * a single class, and forwards all method calls to the responsible DAO.  
 * 
 * @see ApplicationSqlDao
 * @see ChunkSqlDao
 * @see FileContentSqlDao
 * @see FileVersionSqlDao
 * @see FileHistorySqlDao
 * @see MultiChunkSqlDao
 * @see DatabaseVersionSqlDao
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class SqlDatabase {
	protected static final Logger logger = Logger.getLogger(SqlDatabase.class.getSimpleName());

	protected Connection connection;
	protected ApplicationSqlDao applicationDao;
	protected ChunkSqlDao chunkDao;
	protected FileContentSqlDao fileContentDao;
	protected FileVersionSqlDao fileVersionDao;
	protected FileHistorySqlDao fileHistoryDao;
	protected MultiChunkSqlDao multiChunkDao;
	protected DatabaseVersionSqlDao databaseVersionDao;

	public SqlDatabase(Config config) {
		this(config, false);
	}

	public SqlDatabase(Config config, boolean readOnly) {
		this.connection = config.createDatabaseConnection(readOnly);
		this.applicationDao = new ApplicationSqlDao(connection);
		this.chunkDao = new ChunkSqlDao(connection);
		this.fileContentDao = new FileContentSqlDao(connection);
		this.fileVersionDao = new FileVersionSqlDao(connection);
		this.fileHistoryDao = new FileHistorySqlDao(connection, fileVersionDao);
		this.multiChunkDao = new MultiChunkSqlDao(connection);
		this.databaseVersionDao = new DatabaseVersionSqlDao(connection, chunkDao, fileContentDao, fileVersionDao, fileHistoryDao, multiChunkDao);

	}

	// General

	public Connection getConnection() {
		return connection; // TODO [low] Exposes internal state!
	}

	public void commit() throws SQLException {
		connection.commit();
	}

	@Override
	public void finalize() {
		try {
			if (!connection.isClosed()) {
				connection.commit();
				connection.close();
			}
		}
		catch (SQLException e) {
			logger.log(Level.WARNING, "Failed to close database connection. Possible resource leak.", e);
		}
	}

	public void rollback() throws SQLException {
		connection.rollback();
	}

	public void removeUnreferencedDatabaseEntities() {
		try {
			removeUnreferencedFileHistories();
			removeUnreferencedFileContents();
			removeUnreferencedMultiChunks();
			removeUnreferencedChunks();

			removeEmptyDatabaseVersionHeaders();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	// Application

	public void writeKnownRemoteDatabases(List<DatabaseRemoteFile> remoteDatabases) throws SQLException {
		applicationDao.writeKnownRemoteDatabases(remoteDatabases);
	}

	public List<DatabaseRemoteFile> getKnownDatabases() {
		return applicationDao.getKnownDatabases();
	}
	
	public VectorClock getHighestKnownDatabaseFilenameNumbers() {
		return applicationDao.getHighestKnownDatabaseFilenameNumbers();
	}
	
	public void removeKnownDatabases() {
		applicationDao.removeKnownDatabases();
	}

	public Long getCleanupNumber() {
		return applicationDao.getCleanupNumber();
	}
	
	public Long getCleanupTime() {
		return applicationDao.getCleanupTime();
	}
	
	public void writeCleanupNumber(long cleanupNumber) {
		applicationDao.writeCleanupNumber(cleanupNumber);		
	}
	
	public void writeCleanupTime(long cleanupTime) {
		applicationDao.writeCleanupTime(cleanupTime);		
	}

	public void deleteAll() {
		applicationDao.deleteAll();
	}

	public void shutdown() {
		applicationDao.shutdown();
	}

	// Database version

	public Iterator<DatabaseVersion> getDirtyDatabaseVersions() {
		return databaseVersionDao.getDirtyDatabaseVersions();
	}

	public Iterator<DatabaseVersion> getDatabaseVersionsTo(String machineName, long maxLocalClientVersion) {
		return databaseVersionDao.getDatabaseVersionsTo(machineName, maxLocalClientVersion);
	}

	public Iterator<DatabaseVersion> getLastDatabaseVersions(int maxDatabaseVersionCount, int startDatabaseVersionIndex, int maxFileHistoryCount) {
		return databaseVersionDao.getLastDatabaseVersions(maxDatabaseVersionCount, startDatabaseVersionIndex, maxFileHistoryCount);
	}

	public DatabaseVersionHeader getLastDatabaseVersionHeader() {
		return databaseVersionDao.getLastDatabaseVersionHeader();
	}

	public DatabaseBranch getLocalDatabaseBranch() {
		return databaseVersionDao.getLocalDatabaseBranch();
	}

	public List<DatabaseVersionHeader> getNonEmptyDatabaseVersionHeaders() {
		return databaseVersionDao.getNonEmptyDatabaseVersionHeaders();
	}

	public long writeDatabaseVersion(DatabaseVersion databaseVersion) {
		return databaseVersionDao.writeDatabaseVersion(databaseVersion);
	}

	public void writeDatabaseVersionHeader(DatabaseVersionHeader databaseVersionHeader) throws SQLException {
		databaseVersionDao.writeDatabaseVersionHeader(databaseVersionHeader);
	}

	public void markDatabaseVersionDirty(VectorClock vectorClock) {
		databaseVersionDao.markDatabaseVersionDirty(vectorClock);
	}

	public void removeDirtyDatabaseVersions(long newDatabaseVersionId) {
		databaseVersionDao.removeDirtyDatabaseVersions(newDatabaseVersionId);
	}

	public void removeEmptyDatabaseVersionHeaders() {
		databaseVersionDao.removeEmptyDatabaseVersionHeaders();
	}

	public Long getMaxDirtyVectorClock(String machineName) {
		return databaseVersionDao.getMaxDirtyVectorClock(machineName);
	}

	// File History

	@Deprecated
	public Map<FileHistoryId, PartialFileHistory> getFileHistoriesWithFileVersions() {
		// TODO [medium] Note: This returns the full database. Don't use this!
		return fileHistoryDao.getFileHistoriesWithFileVersions();
	}

	public Map<FileHistoryId, PartialFileHistory> getFileHistories(List<FileHistoryId> fileHistoryIds) {
		return fileHistoryDao.getFileHistories(fileHistoryIds);
	}

	public List<PartialFileHistory> getFileHistoriesWithLastVersion() {
		return fileHistoryDao.getFileHistoriesWithLastVersion();
	}

	public Collection<PartialFileHistory> getFileHistoriesWithLastVersionByChecksumSizeAndModifiedDate(String checksum, long size, Date modifiedDate) {
		return fileHistoryDao.getFileHistoriesByChecksumSizeAndModifiedDate(checksum, size, modifiedDate);
	}

	public PartialFileHistory getFileHistoriesWithLastVersionByPath(String path) {
		return fileHistoryDao.getFileHistoryWithLastVersionByPath(path);
	}

	private void removeUnreferencedFileHistories() throws SQLException {
		fileHistoryDao.removeUnreferencedFileHistories();
	}

	public FileHistoryId expandFileHistoryId(FileHistoryId fileHistoryId) {
		return fileHistoryDao.expandFileHistoryId(fileHistoryId);
	}

	// File Version

	public Map<String, FileVersion> getCurrentFileTree() {
		return fileVersionDao.getCurrentFileTree();
	}

	public void removeSmallerOrEqualFileVersions(Map<FileHistoryId, FileVersion> purgeFileVersions) throws SQLException {
		fileVersionDao.removeFileVersions(purgeFileVersions);
	}

	public void removeFileVersions(Map<FileHistoryId, List<FileVersion>> purgeFileVersions) throws SQLException {
		fileVersionDao.removeSpecificFileVersions(purgeFileVersions);
	}
	
	public List<FileVersion> getFileList(String pathExpression, Date date, boolean fileHistoryId, boolean recursive, boolean deleted,
			Set<FileType> fileTypes) {
		
		return fileVersionDao.getFileList(pathExpression, date, fileHistoryId, recursive, deleted, fileTypes);
	}

	public List<FileVersion> getFileHistory(FileHistoryId fileHistoryId) {
		return fileVersionDao.getFileHistory(fileHistoryId);
	}

	public Map<FileHistoryId, FileVersion> getFileHistoriesWithMaxPurgeVersion(int keepVersionsCount) {
		return fileVersionDao.getFileHistoriesWithMaxPurgeVersion(keepVersionsCount);
	}

	public Map<FileHistoryId, List<FileVersion>> getFileHistoriesToPurgeInInterval(long beginTimestamp, long endTimestamp, TimeUnit timeUnit) {
		return fileVersionDao.getFileHistoriesToPurgeInInterval(beginTimestamp, endTimestamp, timeUnit);
	}

	public Map<FileHistoryId, List<FileVersion>> getFileHistoriesToPurgeBefore(long timestamp) {
		return fileVersionDao.getFileHistoriesToPurgeBefore(timestamp);
	}

	public Map<FileHistoryId, FileVersion> getDeletedFileVersions() {
		return fileVersionDao.getDeletedFileVersions();
	}

	public Map<FileHistoryId, FileVersion> getDeletedFileVersionsBefore(long timestamp) {
		return fileVersionDao.getDeletedFileVersionsBefore(timestamp);
	}

	public FileVersion getFileVersion(FileHistoryId fileHistoryId, long version) {
		return fileVersionDao.getFileVersion(fileHistoryId, version);
	}

	// Multi Chunk

	public List<MultiChunkId> getMultiChunkIds(FileChecksum fileChecksum) {
		return multiChunkDao.getMultiChunkIds(fileChecksum);
	}

	public MultiChunkId getMultiChunkId(ChunkChecksum chunkChecksum) {
		return multiChunkDao.getMultiChunkId(chunkChecksum);
	}

	public Map<ChunkChecksum, MultiChunkId> getMultiChunkIdsByChecksums(List<ChunkChecksum> chunkChecksums) {
		return multiChunkDao.getMultiChunkIdsByChecksums(chunkChecksums);
	}

	public List<MultiChunkId> getDirtyMultiChunkIds() {
		return multiChunkDao.getDirtyMultiChunkIds();
	}

	public Map<MultiChunkId, MultiChunkEntry> getUnusedMultiChunks() {
		return multiChunkDao.getUnusedMultiChunks();
	}

	private void removeUnreferencedMultiChunks() throws SQLException {
		multiChunkDao.removeUnreferencedMultiChunks();
	}

	public Map<MultiChunkId, MultiChunkEntry> getMultiChunks() {
		return multiChunkDao.getMultiChunks();
	}

	public void writeMuddyMultiChunks(Map<DatabaseVersionHeader, Collection<MultiChunkEntry>> muddyMultiChunks) throws SQLException {
		multiChunkDao.writeMuddyMultiChunks(muddyMultiChunks);
	}

	public Map<MultiChunkId, MultiChunkEntry> getMuddyMultiChunks() {
		return multiChunkDao.getMuddyMultiChunks();
	}

	public void removeNonMuddyMultiChunks() throws SQLException {
		multiChunkDao.removeNonMuddyMultiChunks();
	}

	// Chunk

	protected Map<ChunkChecksum, ChunkEntry> getChunks(VectorClock vectorClock) {
		return chunkDao.getChunks(vectorClock);
	}

	public ChunkEntry getChunk(ChunkChecksum chunkChecksum) {
		return chunkDao.getChunk(chunkChecksum);
	}

	private void removeUnreferencedChunks() {
		chunkDao.removeUnreferencedChunks();
	}

	// File Content

	public FileContent getFileContent(FileChecksum fileChecksum, boolean includeChunkChecksums) {
		return fileContentDao.getFileContent(fileChecksum, includeChunkChecksums);
	}

	private void removeUnreferencedFileContents() throws SQLException {
		fileContentDao.removeUnreferencedFileContents();
	}

}
