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
package org.syncany.operations.up;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.chunk.Chunk;
import org.syncany.chunk.Deduper;
import org.syncany.chunk.DeduperListener;
import org.syncany.chunk.MultiChunk;
import org.syncany.config.Config;
import org.syncany.config.LocalEventBus;
import org.syncany.database.ChunkEntry;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileContent;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.FileVersionComparator;
import org.syncany.database.FileVersionComparator.FileProperties;
import org.syncany.database.FileVersionComparator.FileVersionComparison;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.daemon.messages.UpIndexChangesDetectedSyncExternalEvent;
import org.syncany.operations.daemon.messages.UpIndexEndSyncExternalEvent;
import org.syncany.operations.daemon.messages.UpIndexStartSyncExternalEvent;
import org.syncany.util.EnvironmentUtil;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

/**
 * The indexer combines the chunking process with the corresponding database
 * lookups for the resulting chunks. It implements the deduplication mechanism 
 * of Syncany.
 * 
 * <p>The class takes a list of files as input and uses the {@link Deduper} to
 * break these files into individual chunks. By implementing the {@link DeduperListener},
 * it reacts on chunking events and creates a new database version (with the newly
 * added/changed/removed files. This functionality is entirely implemented by the
 * {@link #index(List) index()} method.
 * 
 * <p>The class uses the currently loaded {@link MemoryDatabase} as well as a potential  
 * dirty database into account. Lookups for chunks and file histories are performed 
 * on both databases.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Indexer {
	private static final Logger logger = Logger.getLogger(Indexer.class.getSimpleName());
	private static final String DEFAULT_POSIX_PERMISSIONS_FILE = "rw-r--r--";
	private static final String DEFAULT_POSIX_PERMISSIONS_FOLDER = "rwxr-xr-x";
	private static final String DEFAULT_DOS_ATTRIBUTES = "--a-";

	private Config config;
	private Deduper deduper;
	private SqlDatabase localDatabase;

	private LocalEventBus eventBus;

	public Indexer(Config config, Deduper deduper) {
		this.config = config;
		this.deduper = deduper;
		this.localDatabase = new SqlDatabase(config);

		this.eventBus = LocalEventBus.getInstance();
	}

	/**
	 * This method implements the index/deduplication functionality of Syncany. It uses a {@link Deduper}
	 * to break files down, compares them to the local database and creates a new {@link DatabaseVersion}
	 * as a result. 
	 * 
	 * <p>Depending on what has changed, the new database version will contain new instances of 
	 * {@link PartialFileHistory}, {@link FileVersion}, {@link FileContent}, {@link ChunkEntry} and 
	 * {@link MultiChunkEntry}.
	 * 
	 * @param files List of files to be deduplicated
	 * @return New database version containing new/changed/deleted entities
	 * @throws IOException If the chunking/deduplication cannot read/process any of the files
	 */
	public DatabaseVersion index(List<File> files) throws IOException {
		DatabaseVersion newDatabaseVersion = new DatabaseVersion();

		// Load file history cache
		List<PartialFileHistory> fileHistoriesWithLastVersion = localDatabase.getFileHistoriesWithLastVersion();

		// TODO [medium] This should be in FileHistoryDao
		Map<FileChecksum, List<PartialFileHistory>> fileChecksumCache = fillFileChecksumCache(fileHistoriesWithLastVersion);
		Map<String, PartialFileHistory> filePathCache = fillFilePathCache(fileHistoriesWithLastVersion);

		// Find and index new files
		deduper.deduplicate(files, new IndexerDeduperListener(newDatabaseVersion, fileChecksumCache, filePathCache));

		// Find and remove deleted files
		removeDeletedFiles(newDatabaseVersion, fileHistoriesWithLastVersion);

		return newDatabaseVersion;
	}

	private Map<String, PartialFileHistory> fillFilePathCache(List<PartialFileHistory> fileHistoriesWithLastVersion) {
		Map<String, PartialFileHistory> filePathCache = new HashMap<String, PartialFileHistory>();

		for (PartialFileHistory fileHistory : fileHistoriesWithLastVersion) {
			filePathCache.put(fileHistory.getLastVersion().getPath(), fileHistory);
		}

		return filePathCache;
	}

	private Map<FileChecksum, List<PartialFileHistory>> fillFileChecksumCache(List<PartialFileHistory> fileHistoriesWithLastVersion) {
		Map<FileChecksum, List<PartialFileHistory>> fileChecksumCache = new HashMap<FileChecksum, List<PartialFileHistory>>();

		for (PartialFileHistory fileHistory : fileHistoriesWithLastVersion) {
			FileChecksum fileChecksum = fileHistory.getLastVersion().getChecksum();

			if (fileChecksum != null) {
				List<PartialFileHistory> fileHistoriesWithSameChecksum = fileChecksumCache.get(fileChecksum);

				if (fileHistoriesWithSameChecksum == null) {
					fileHistoriesWithSameChecksum = new ArrayList<PartialFileHistory>();
				}

				fileHistoriesWithSameChecksum.add(fileHistory);
				fileChecksumCache.put(fileChecksum, fileHistoriesWithSameChecksum);
			}
		}

		return fileChecksumCache;
	}

	private void removeDeletedFiles(DatabaseVersion newDatabaseVersion, List<PartialFileHistory> fileHistoriesWithLastVersion) {
		logger.log(Level.FINER, "- Looking for deleted files ...");

		for (PartialFileHistory fileHistory : fileHistoriesWithLastVersion) {
			// Ignore this file history if it has been updated in this database version before (file probably renamed!)
			if (newDatabaseVersion.getFileHistory(fileHistory.getFileHistoryId()) != null) {
				continue;
			}

			// Check if file exists, remove if it doesn't
			FileVersion lastLocalVersion = fileHistory.getLastVersion();
			File lastLocalVersionOnDisk = new File(config.getLocalDir() + File.separator + lastLocalVersion.getPath());

			// Ignore this file history if the last version is marked "DELETED"
			if (lastLocalVersion.getStatus() == FileStatus.DELETED) {
				continue;
			}

			// Add this file history if a new file with this name has been added (file type change)
			PartialFileHistory newFileWithSameName = getFileHistoryByPathFromDatabaseVersion(newDatabaseVersion, fileHistory.getLastVersion()
					.getPath());

			// If file has VANISHED, mark as DELETED
			if (!FileUtil.exists(lastLocalVersionOnDisk) || newFileWithSameName != null) {
				PartialFileHistory deletedFileHistory = new PartialFileHistory(fileHistory.getFileHistoryId());
				FileVersion deletedVersion = lastLocalVersion.clone();

				deletedVersion.setStatus(FileStatus.DELETED);
				deletedVersion.setVersion(fileHistory.getLastVersion().getVersion() + 1);
				deletedVersion.setUpdated(new Date());

				logger.log(Level.FINER, "  + Deleted: Adding DELETED version: {0}", deletedVersion);
				logger.log(Level.FINER, "                           based on: {0}", lastLocalVersion);

				deletedFileHistory.addFileVersion(deletedVersion);
				newDatabaseVersion.addFileHistory(deletedFileHistory);
			}
		}
	}

	private PartialFileHistory getFileHistoryByPathFromDatabaseVersion(DatabaseVersion databaseVersion, String path) {
		// TODO [medium] Extremely performance intensive, because this is called inside a loop above. Implement better caching for database version!!!

		for (PartialFileHistory fileHistory : databaseVersion.getFileHistories()) {
			FileVersion lastVersion = fileHistory.getLastVersion();

			if (lastVersion.getStatus() != FileStatus.DELETED && lastVersion.getPath().equals(path)) {
				return fileHistory;
			}
		}

		return null;
	}

	public static class IndexerException extends RuntimeException {
		private static final long serialVersionUID = 5247751938336036877L;

		public IndexerException(String message) {
			super(message);
		}
	}

	private class IndexerDeduperListener implements DeduperListener {
		private FileVersionComparator fileVersionComparator;
		private SecureRandom secureRandom;
		private DatabaseVersion newDatabaseVersion;

		private Map<FileChecksum, List<PartialFileHistory>> fileChecksumCache;
		private Map<String, PartialFileHistory> filePathCache;

		private ChunkEntry chunkEntry;
		private MultiChunkEntry multiChunkEntry;
		private FileContent fileContent;

		private FileProperties startFileProperties;
		private FileProperties endFileProperties;

		public IndexerDeduperListener(DatabaseVersion newDatabaseVersion, Map<FileChecksum, List<PartialFileHistory>> fileChecksumCache,
				Map<String, PartialFileHistory> filePathCache) {

			this.fileVersionComparator = new FileVersionComparator(config.getLocalDir(), config.getChunker().getChecksumAlgorithm());
			this.secureRandom = new SecureRandom();
			this.newDatabaseVersion = newDatabaseVersion;

			this.fileChecksumCache = fileChecksumCache;
			this.filePathCache = filePathCache;
		}

		@Override
		public boolean onFileFilter(File file) {
			logger.log(Level.FINER, "- +File {0}", file);

			startFileProperties = fileVersionComparator.captureFileProperties(file, null, false);

			// Check if file has vanished
			if (!startFileProperties.exists() || startFileProperties.isLocked()) {
				logger.log(Level.FINER, "- /File: {0}", file);
				logger.log(Level.INFO, "   * NOT ADDING because file has VANISHED (exists = {0}) or is LOCKED (locked = {1}).", new Object[] {
						startFileProperties.exists(), startFileProperties.isLocked() });

				resetFileEnd();
				return false;
			}

			// Content
			if (startFileProperties.getType() == FileType.FILE) {
				logger.log(Level.FINER, "- +FileContent: {0}", file);
				fileContent = new FileContent();
			}

			return true;
		}

		@Override
		public boolean onFileStart(File file, int fileIndex) {
			boolean processFile = startFileProperties.getType() == FileType.FILE; // Ignore directories and symlinks!

			// We could fire an event here, but firing for every file
			// is very exhausting for the event bus.

			return processFile;
		}

		@Override
		public void onFileEnd(File file, byte[] rawFileChecksum) {
			// Get file attributes (get them while file exists)
			
			// Note: Do NOT move any File-methods (file.anything()) below the file.exists()-part,
			// because the file could vanish!
			
			FileChecksum fileChecksum = (rawFileChecksum != null) ? new FileChecksum(rawFileChecksum) : null;
			endFileProperties = fileVersionComparator.captureFileProperties(file, fileChecksum, false);

			// Check if file has vanished
			boolean fileIsLocked = endFileProperties.isLocked();
			boolean fileVanished = !endFileProperties.exists();
			boolean fileHasChanged = startFileProperties.getSize() != endFileProperties.getSize()
					|| startFileProperties.getLastModified() != endFileProperties.getLastModified();

			if (fileVanished || fileIsLocked || fileHasChanged) {
				logger.log(Level.FINER, "- /File: {0}", file);
				logger.log(Level.INFO, "   * NOT ADDING because file has VANISHED (" + !endFileProperties.exists() + "), is LOCKED ("
						+ endFileProperties.isLocked() + "), or has CHANGED (" + fileHasChanged + ")");

				resetFileEnd();
				return;
			}

			// If it's still there, add it to the database
			addFileVersion(endFileProperties);

			// Reset
			resetFileEnd();
		}

		private void addFileVersion(FileProperties fileProperties) {
			if (fileProperties.getChecksum() != null) {
				logger.log(Level.FINER, "- /File: {0} (checksum {1})",
						new Object[] { fileProperties.getRelativePath(), fileProperties.getChecksum() });
			}
			else {
				logger.log(Level.FINER, "- /File: {0} (directory/symlink/0-byte-file)", fileProperties.getRelativePath());
			}

			// 1. Determine if file already exists in database
			PartialFileHistory lastFileHistory = guessLastFileHistory(fileProperties);
			FileVersion lastFileVersion = (lastFileHistory != null) ? lastFileHistory.getLastVersion() : null;

			// 2. Create new file history/version
			PartialFileHistory fileHistory = createNewFileHistory(lastFileHistory);
			FileVersion fileVersion = createNewFileVersion(lastFileVersion, fileProperties);

			// 3. Compare new and last version
			FileProperties lastFileVersionProperties = fileVersionComparator.captureFileProperties(lastFileVersion);
			FileVersionComparison lastToNewFileVersionComparison = fileVersionComparator.compare(fileProperties, lastFileVersionProperties, true);

			boolean newVersionDiffersFromToLastVersion = !lastToNewFileVersionComparison.equals();

			if (newVersionDiffersFromToLastVersion) {
				fileHistory.addFileVersion(fileVersion);
				newDatabaseVersion.addFileHistory(fileHistory);

				logger.log(Level.INFO, "   * Added file version:    " + fileVersion);
				logger.log(Level.INFO, "     based on file version: " + lastFileVersion);
				
				fireHasChangesEvent();
			}
			else {
				logger.log(Level.INFO, "   * NOT ADDING file version: " + fileVersion);
				logger.log(Level.INFO, "         b/c IDENTICAL prev.: " + lastFileVersion);
			}

			// 4. Add file content (if not a directory)
			if (fileProperties.getChecksum() != null && fileContent != null) {
				fileContent.setSize(fileProperties.getSize());
				fileContent.setChecksum(fileProperties.getChecksum());

				// Check if content already exists, throw gathered content away if it does!
				FileContent existingContent = localDatabase.getFileContent(fileProperties.getChecksum(), false);

				if (existingContent == null) {
					newDatabaseVersion.addFileContent(fileContent);
				}
				else {
					// Uses existing content (already in database); ref. by checksum
				}
			}
		}

		private void fireHasChangesEvent() {
			boolean firstNewFileDetected = newDatabaseVersion.getFileHistories().size() == 1;
			
			if (firstNewFileDetected) { // Only fires once!
				eventBus.post(new UpIndexChangesDetectedSyncExternalEvent(config.getLocalDir().getAbsolutePath()));
			}
		}

		private PartialFileHistory createNewFileHistory(PartialFileHistory lastFileHistory) {
			if (lastFileHistory == null) {
				FileHistoryId newFileHistoryId = FileHistoryId.secureRandomFileId();
				return new PartialFileHistory(newFileHistoryId);
			}
			else {
				return new PartialFileHistory(lastFileHistory.getFileHistoryId());
			}
		}

		private FileVersion createNewFileVersion(FileVersion lastFileVersion, FileProperties fileProperties) {
			FileVersion fileVersion = null;

			// Version
			if (lastFileVersion == null) {
				fileVersion = new FileVersion();
				fileVersion.setVersion(1L);
				fileVersion.setStatus(FileStatus.NEW);
			}
			else {
				fileVersion = lastFileVersion.clone();
				fileVersion.setVersion(lastFileVersion.getVersion() + 1);
			}

			// Simple attributes
			fileVersion.setPath(fileProperties.getRelativePath());
			fileVersion.setLinkTarget(fileProperties.getLinkTarget());
			fileVersion.setType(fileProperties.getType());
			fileVersion.setSize(fileProperties.getSize());
			fileVersion.setChecksum(fileProperties.getChecksum());
			fileVersion.setLastModified(new Date(fileProperties.getLastModified()));
			fileVersion.setUpdated(new Date());

			// Permissions
			if (EnvironmentUtil.isWindows()) {
				fileVersion.setDosAttributes(fileProperties.getDosAttributes());
				
				if (fileVersion.getType() == FileType.FOLDER) {
					fileVersion.setPosixPermissions(DEFAULT_POSIX_PERMISSIONS_FOLDER);
				}
				else {
					fileVersion.setPosixPermissions(DEFAULT_POSIX_PERMISSIONS_FILE);
				}
			}
			else if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
				fileVersion.setPosixPermissions(fileProperties.getPosixPermissions());
				fileVersion.setDosAttributes(DEFAULT_DOS_ATTRIBUTES);
			}

			// Status
			if (lastFileVersion != null) {
				if (fileVersion.getType() == FileType.FILE
						&& FileChecksum.fileChecksumEquals(fileVersion.getChecksum(), lastFileVersion.getChecksum())) {
					
					fileVersion.setStatus(FileStatus.CHANGED);
				}
				else if (!fileVersion.getPath().equals(lastFileVersion.getPath())) {
					fileVersion.setStatus(FileStatus.RENAMED);
				}
				else {
					fileVersion.setStatus(FileStatus.CHANGED);
				}
			}

			return fileVersion;
		}

		private void resetFileEnd() {
			fileContent = null;
			startFileProperties = null;
			endFileProperties = null;
		}

		private PartialFileHistory guessLastFileHistory(FileProperties fileProperties) {
			if (fileProperties.getType() == FileType.FILE) {
				return guessLastFileHistoryForFile(fileProperties);
			}
			else if (fileProperties.getType() == FileType.SYMLINK) {
				return guessLastFileHistoryForSymlink(fileProperties);
			}
			else if (fileProperties.getType() == FileType.FOLDER) {
				return guessLastFileHistoryForFolder(fileProperties);
			}
			else {
				throw new RuntimeException("This should not happen.");
			}
		}

		private PartialFileHistory guessLastFileHistoryForSymlink(FileProperties fileProperties) {
			return guessLastFileHistoryForFolderOrSymlink(fileProperties);
		}

		private PartialFileHistory guessLastFileHistoryForFolder(FileProperties fileProperties) {
			return guessLastFileHistoryForFolderOrSymlink(fileProperties);
		}

		private PartialFileHistory guessLastFileHistoryForFolderOrSymlink(FileProperties fileProperties) {
			PartialFileHistory lastFileHistory = filePathCache.get(fileProperties.getRelativePath());

			if (lastFileHistory == null) {
				logger.log(Level.FINER, "   * No old file history found, starting new history (path: " + fileProperties.getRelativePath() + ", "
						+ fileProperties.getType() + ")");
				return null;
			}
			else {
				FileVersion lastFileVersion = lastFileHistory.getLastVersion();

				if (lastFileVersion.getStatus() != FileStatus.DELETED && lastFileVersion.getType() == fileProperties.getType()) {
					logger.log(Level.FINER,
							"   * Found old file history " + lastFileHistory.getFileHistoryId() + " (by path: " + fileProperties.getRelativePath()
									+ "), " + fileProperties.getType() + ", appending new version.");
					return lastFileHistory;
				}
				else {
					logger.log(Level.FINER, "   * No old file history found, starting new history (path: " + fileProperties.getRelativePath() + ", "
							+ fileProperties.getType() + ")");
					return null;
				}
			}
		}

		/**
		 * Tries to guess a matching file history, first by path and then by matching checksum.
		 * 
		 * <p>If the path matches the path of an existing file in the database, the file history 
		 * from the database is used, and a new file version is appended. If there is no file 
		 * in the database with that path, checksums are compared.
		 * 
		 * <p>If there are more than one file with the same checksum (potential matches), the file
		 * with the closest path is chosen.
		 */
		private PartialFileHistory guessLastFileHistoryForFile(FileProperties fileProperties) {
			PartialFileHistory lastFileHistory = null;

			// a) Try finding a file history for which the last version has the same path
			lastFileHistory = filePathCache.get(fileProperties.getRelativePath());

			// b) If that fails, try finding files with a matching checksum
			if (lastFileHistory == null) {
				if (fileProperties.getChecksum() != null) {
					Collection<PartialFileHistory> fileHistoriesWithSameChecksum = fileChecksumCache.get(fileProperties.getChecksum());

					if (fileHistoriesWithSameChecksum != null && fileHistoriesWithSameChecksum.size() > 0) {
						lastFileHistory = guessLastFileHistoryForFileWithMatchingChecksum(fileProperties, fileHistoriesWithSameChecksum);

						// Remove the lastFileHistory we are basing this one on from the
						// cache, so no other history will be
						fileHistoriesWithSameChecksum.remove(lastFileHistory);

						if (fileHistoriesWithSameChecksum.isEmpty()) {
							fileChecksumCache.remove(fileProperties.getChecksum());
						}
					}
				}

				if (lastFileHistory == null) {
					logger.log(Level.FINER, "   * No old file history found, starting new history (path: " + fileProperties.getRelativePath()
							+ ", checksum: " + fileProperties.getChecksum() + ")");
					return null;
				}
				else {
					logger.log(Level.FINER,
							"   * Found old file history " + lastFileHistory.getFileHistoryId() + " (by checksum: " + fileProperties.getChecksum()
									+ "), appending new version.");
					return lastFileHistory;
				}
			}
			else {
				if (fileProperties.getType() != lastFileHistory.getLastVersion().getType()) {
					logger.log(Level.FINER, "   * No old file history found, starting new history (path: " + fileProperties.getRelativePath()
							+ ", checksum: " + fileProperties.getChecksum() + ")");
					return null;
				}
				else {
					logger.log(Level.FINER,
							"   * Found old file history " + lastFileHistory.getFileHistoryId() + " (by path: " + fileProperties.getRelativePath()
									+ "), appending new version.");
					return lastFileHistory;
				}
			}
		}

		private PartialFileHistory guessLastFileHistoryForFileWithMatchingChecksum(FileProperties fileProperties,
				Collection<PartialFileHistory> fileHistoriesWithSameChecksum) {
			PartialFileHistory lastFileHistory = null;

			// Check if they do not exist anymore --> assume it has moved!
			// We choose the best fileHistory to base on as follows:

			// 1. Ensure that it was modified at the same time and is the same size
			// 2. Check the fileHistory was deleted and the file does not actually exists
			// 3. Choose the one with the longest matching tail of the path to the new path

			for (PartialFileHistory fileHistoryWithSameChecksum : fileHistoriesWithSameChecksum) {
				FileVersion lastVersion = fileHistoryWithSameChecksum.getLastVersion();

				if (fileProperties.getLastModified() != lastVersion.getLastModified().getTime() || fileProperties.getSize() != lastVersion.getSize()) {
					continue;
				}

				File lastVersionOnLocalDisk = new File(config.getLocalDir() + File.separator + lastVersion.getPath());

				if (lastVersion.getStatus() != FileStatus.DELETED && !FileUtil.exists(lastVersionOnLocalDisk)) {
					if (lastFileHistory == null) {
						lastFileHistory = fileHistoryWithSameChecksum;
					}
					else {
						String filePath = fileProperties.getRelativePath();
						String currentPreviousPath = lastFileHistory.getLastVersion().getPath();
						String candidatePreviousPath = fileHistoryWithSameChecksum.getLastVersion().getPath();

						for (int i = 0; i < filePath.length(); i++) {
							if (!filePath.regionMatches(filePath.length() - i, candidatePreviousPath, candidatePreviousPath.length() - i, i)) {
								// The candidate no longer matches, take the current path.
								break;
							}
							if (!filePath.regionMatches(filePath.length() - i, currentPreviousPath, currentPreviousPath.length() - i, i)) {
								// The current previous path no longer matches, take the new candidate
								lastFileHistory = fileHistoryWithSameChecksum;
								break;
							}
						}
					}
				}
			}

			return lastFileHistory;
		}

		@Override
		public void onMultiChunkOpen(MultiChunk multiChunk) {
			logger.log(Level.FINER, "- +MultiChunk {0}", multiChunk.getId());
			multiChunkEntry = new MultiChunkEntry(multiChunk.getId(), 0); // size unknown so far
		}

		@Override
		public void onMultiChunkWrite(MultiChunk multiChunk, Chunk chunk) {
			logger.log(Level.FINER, "- Chunk > MultiChunk: {0} > {1}", new Object[] { StringUtil.toHex(chunk.getChecksum()), multiChunk.getId() });
			multiChunkEntry.addChunk(chunkEntry.getChecksum());
		}

		@Override
		public void onMultiChunkClose(MultiChunk multiChunk) {
			logger.log(Level.FINER, "- /MultiChunk {0}", multiChunk.getId());

			multiChunkEntry.setSize(multiChunk.getSize());

			newDatabaseVersion.addMultiChunk(multiChunkEntry);
			multiChunkEntry = null;
		}

		@Override
		public File getMultiChunkFile(MultiChunkId multiChunkId) {
			return config.getCache().getEncryptedMultiChunkFile(multiChunkId);
		}

		@Override
		public MultiChunkId createNewMultiChunkId(Chunk firstChunk) {
			byte[] newMultiChunkId = new byte[firstChunk.getChecksum().length];
			secureRandom.nextBytes(newMultiChunkId);

			return new MultiChunkId(newMultiChunkId);
		}

		@Override
		public void onFileAddChunk(File file, Chunk chunk) {
			logger.log(Level.FINER, "- Chunk > FileContent: {0} > {1}", new Object[] { StringUtil.toHex(chunk.getChecksum()), file });
			fileContent.addChunk(new ChunkChecksum(chunk.getChecksum()));
		}

		@Override
		public void onStart(int fileCount) {
			eventBus.post(new UpIndexStartSyncExternalEvent(config.getLocalDir().getAbsolutePath(), fileCount));
		}

		@Override
		public void onFinish() {
			eventBus.post(new UpIndexEndSyncExternalEvent(config.getLocalDir().getAbsolutePath()));
		} 

		/**
		 * Checks if chunk already exists in all database versions
		 * Afterwards checks if chunk exists in new introduced database version. 
		 */
		@Override
		public boolean onChunk(Chunk chunk) {
			ChunkChecksum chunkChecksum = new ChunkChecksum(chunk.getChecksum());
			chunkEntry = localDatabase.getChunk(chunkChecksum);

			if (chunkEntry == null) {
				chunkEntry = newDatabaseVersion.getChunk(chunkChecksum);

				if (chunkEntry == null) {
					logger.log(Level.FINER, "- Chunk new: {0}", chunkChecksum.toString());

					chunkEntry = new ChunkEntry(chunkChecksum, chunk.getSize());
					newDatabaseVersion.addChunk(chunkEntry);

					return true;
				}
			}

			logger.log(Level.FINER, "- Chunk exists: {0}", StringUtil.toHex(chunk.getChecksum()));
			return false;
		}
	}
}
