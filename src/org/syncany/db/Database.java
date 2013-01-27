/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.db;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.syncany.Constants;
import org.syncany.config.Profile;
import org.syncany.config.Settings;
import org.syncany.db.CloneFile.Status;
import org.syncany.db.CloneFile.SyncStatus;
import org.syncany.exceptions.CloneTreeException;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;
import org.syncany.watch.remote.FileHistory;
import org.syncany.watch.remote.FileUpdate;

/**
 * Provides access to the database.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Database {
	// private static final Config config = Config.getInstance();
	private static final Logger logger = Logger.getLogger(Database.class
			.getSimpleName());
	private static final Database instance = new Database();
	private static final ChunkCache chunkCache = new ChunkCache();

	// Path indexed with Key = Path, Value = CloneFile
	private CloneFileTree cloneFileTree;
	private List<CloneClient> cloneClients;

	private long dbversion = 0;
	
	/**
	 * Initializing database provider object
	 */
	private Database() {
		if (logger.isLoggable(Level.INFO)) {
			logger.info("Creating DB helper ...");
		}
		cloneFileTree = DatabaseIO.readCompleteCloneFileTree();
		cloneClients = new LinkedList<CloneClient>();
	}

	/**
	 * Singleton pattern for Database
	 * 
	 * @return singleton instance of the database provider object
	 */
	public static Database getInstance() {
		return instance;
	}
	
	public void incrementVersion() {
		System.out.println("--- VERSION " + dbversion + " ---");
		dbversion++;
		System.out.println("--- VERSION " + dbversion + " ---");
	}

	public CloneFile getFolder(File file) {
		return getFileOrFolder(file, true);
	}

	public CloneFile getFile(File file) {
		return getFileOrFolder(file, false);
	}

	public CloneFile getFileOrFolder(File file) {
		return getFileOrFolder(file, null);
	}

	private CloneFile getFileOrFolder(File file, Boolean folder) {
		File rootFolder = Profile.getInstance().getRoot();
		String parent = FileUtil.getRelativeParentDirectory(rootFolder, file);
		String path = (parent.isEmpty()) ? toDatabasePath(file.getName())
				: toDatabasePath(parent + File.separator + file.getName());

		CloneFileTreeItem item = cloneFileTree.getFileByPath(path);
		
		if (item == null)
			return null;
		
		CloneFile cloneFile = item.getCloneFile();
		if (cloneFile == null)
			return null;

		if (cloneFile.getStatus() == Status.DELETED
				|| cloneFile.getStatus() == Status.MERGED)
			return null;

		if (cloneFile.isFolder() && folder != null && !folder)
			return null;

		return cloneFile;
	}
	
	public synchronized List<FileHistory> getFileHistories(){
		List<FileHistory> histories = new LinkedList<FileHistory>();
		
		List<CloneFile> allFiles = cloneFileTree.getAllFiles();
		for(CloneFile cf : allFiles){
			FileHistory fh = new FileHistory(Settings.getInstance().getMachineName(), cf.getFileId());
			CloneFile f = cf;
			while(true){
				fh.add(FileUpdate.fromCloneFile(f));
				
				if(f.getPreviousVersion() != null)
					f = f.getPreviousVersion();
				else
					break;
			}
			
			histories.add(fh);
		}
		
		return histories;
	}

	public List<CloneFile> getChildren(CloneFile parentFile) {
		return getChildren(parentFile, false);
	}

	public List<CloneFile> getChildren(CloneFile parentFile, boolean recursive) {
		// Make child path
		String childPath = (parentFile.getPath().isEmpty()) ? toDatabasePath(parentFile
				.getName()) : toDatabasePath(parentFile.getPath()
				+ File.separator + parentFile.getName());

		CloneFileTreeItem item = cloneFileTree.getFileByPath(childPath);

		if (item == null)
			return null;

		if (!recursive)
			return item.getDirectCloneFileChildren(Constants.DELETED_MERGED_FILTER);
		else
			return item.getCloneFileChildren(Constants.DELETED_MERGED_FILTER);
	}

	/**
	 * Get file in exact version.
	 * 
	 * @param fileId
	 * @param version
	 * @return
	 */
	public CloneFile getFileOrFolder(long fileId, long version) {
		return cloneFileTree.getFileByIdAndVersion(fileId, version);
	}

	/**
	 * Get file in current (newest) version.
	 */
	public CloneFile getFileOrFolder(long fileId) {
		CloneFile c = cloneFileTree.getFileById(fileId);

		if (c == null || c.getStatus() == Status.DELETED
				|| c.getStatus() == Status.MERGED)
			return null;

		return c;
	}

	/**
	 * Check the files with the same checksum and don't exist anymore to
	 * determine the 'previous version' of this file.
	 * 
	 * If more file are found, i.e. files with the same checksum that don't
	 * exist, choose the one with the smallest Levenshtein distance.
	 */
	public CloneFile getNearestFile(File file, byte[] checksum) {
		List<CloneFile> sameChecksumFiles = cloneFileTree.getFilesByChecksum(
				checksum, Constants.DELETED_MERGED_FILTER);

		CloneFile nearestPreviousVersion = null;
		int previousVersionDistance = Integer.MAX_VALUE;

		for (CloneFile cf : sameChecksumFiles) {
			// Ignore if the file actually exists
			if (cf.getFile().exists()) {
				continue;
			}

			// Check Levenshtein distance
			int distance = StringUtil.computeLevenshteinDistance(
					file.getAbsolutePath(), cf.getFile().getAbsolutePath());

			if (distance < previousVersionDistance) {
				nearestPreviousVersion = cf;
				previousVersionDistance = distance;
			}
		}

		// No history if the distance exceeds the maximum
		if (previousVersionDistance > Constants.MAXIMUM_FILENAME_LEVENSHTEIN_DISTANCE) {
			nearestPreviousVersion = null;
		}

		return nearestPreviousVersion;
	}

	public List<CloneFile> getFiles() {
		return cloneFileTree.getAllFiles();
	}

	public void removeFile(CloneFile c) {
		cloneFileTree.removeCloneFile(c);

		// TODO: Improve for Performance
		DatabaseIO.writeCompleteCloneFileTree(cloneFileTree);
	}
	
	public CloneFile createFile(FileUpdate update) {
		return createFile(update, SyncStatus.SYNCING);
	}

	public CloneFile createFile(FileUpdate update, SyncStatus syncStatus) {
		CloneFile newFile = new CloneFile();

		newFile.setFileId(update.getFileId());
		newFile.setVersion(update.getVersion());
		newFile.setUpdated(update.getUpdated());
		newFile.setChecksum(update.getChecksum());
//		newFile.setRootId(update.getRootId());
		newFile.setPath(update.getPath());
		newFile.setName(update.getName());
		newFile.setLastModified(update.getLastModified());
		newFile.setClientName(update.getClientName());
		newFile.setFileSize(update.getFileSize());
		newFile.setStatus(update.getStatus());
		newFile.setSyncStatus(syncStatus);
		newFile.setIsFolder(update.isFolder());

		if (update.getMergedFileId() != 0) {
			CloneFile mergedVersion = getFileOrFolder(update.getMergedFileId(),
					update.getMergedFileVersion());
			newFile.setMergedTo(mergedVersion);
		}

		// Chunks from previous version
		if (update.getVersion() > 1) {
			// Improved with new DB
			// Replaced: CloneFile previousVersion =
			// getFileOrFolder(update.getFileId(), update.getVersion() - 1);
			CloneFile previousVersion = getFileOrFolder(update.getFileId(),
					update.getVersion() - 1);

			if (previousVersion != null) {
				newFile.setChunks(previousVersion.getChunks());
				previousVersion.setNext(newFile);
				newFile.setPrevious(previousVersion);
			} else {
				if (logger.isLoggable(Level.WARNING)) {
					logger.log(
							Level.WARNING,
							"Could not find previous version for file {0} in database.",
							newFile);
				}
			}
		}

		// Add Chunks (if there are any!)
		// Triggered for new files (= version 1) AND for grown files (= more
		// chunks)
		if (!update.getChunksAdded().isEmpty()) {
			for (String chunkIdStr : update.getChunksAdded()) {
				CloneChunk chunk = getChunk(chunkIdStr, true);
				newFile.addChunk(chunk);
			}
		}

		// Chunks removed
		else if (update.getChunksRemoved() > 0) {
			newFile.removeChunks(update.getChunksRemoved() - 1);
		}

		// Chunks changed
		if (!update.getChunksChanged().isEmpty()) {
			for (Map.Entry<Integer, String> entry : update.getChunksChanged()
					.entrySet()) {
				int chunkIndex = entry.getKey();
				String chunkIdStr = entry.getValue();

				CloneChunk chunk = getChunk(chunkIdStr, true);

				// ADD Again
				newFile.setChunk(chunkIndex, chunk);
			}
		}

		newFile.merge();

		return newFile;
	}

	public synchronized void saveClients() {
		DatabaseIO.writeCloneClients(cloneClients);
	}

	public synchronized void addClient(CloneClient client) {
		for (CloneClient c : cloneClients) {
			if (c.equals(client)) {
				return;
			}
		}

		cloneClients.add(client);
	}

	/**
	 * Retrieves the client with the given name from the database. If it does
	 * not exist and the {@code create}-parameter is true, it creates a new one
	 * and returns it.
	 * 
	 * @param machineName
	 * @param create
	 * @return Returns the client or null if it does not exist
	 */
	public synchronized CloneClient getClient(String machineName, boolean create) {
		if (cloneClients.size() == 0)
			cloneClients = DatabaseIO.readCloneClients();

		for (int i = 1; i < 3; i++) {
			try {
				CloneClient client = null;

				for (CloneClient c : cloneClients) {
					if (c.getMachineName().equals(machineName)) {
						client = c;
					}
				}

				if (create && client == null) {
					logger.info("Logger: Client '" + machineName
							+ "' unknown. Adding to DB.");

					client = new CloneClient(machineName);
					client.merge();
				}

				return client;

			} catch (Exception e) {
				logger.warning("Logger: Adding client '" + machineName
						+ "' failed. Retrying (try = " + i + ")");

				try {
					Thread.sleep(200);
				} catch (InterruptedException ex) { /* Fressen */
				}

				continue;
			}
		}

		logger.severe("Logger: Adding client '" + machineName
				+ "' FAILED completely. Retrying FAILED.");
		return null;
	}

	/**
	 * Retrieves the last chunk/file update.
	 */
	public Long getFileVersionCount() {
		return dbversion; //cloneFileTree.getVersionCount();
	}

	/**
	 * Get chunk from cache. Since multiple chunks with this chunk ID might
	 * exist, take any of them.
	 * 
	 * @param checksum
	 * @return
	 */
	public CloneChunk getChunk(byte[] checksum) {
		Set<CloneChunk> matchingChunks = chunkCache.get(checksum);

		if (matchingChunks != null) {
			// Return 'any' chunk -> take the first we can get
			for (CloneChunk chunk : matchingChunks) {
				return chunk;
			}
		}

		return null;
	}

	/**
	 * Take this exact chunk from the cache, or create it if it doesn't exist
	 * (and 'create' is true).
	 * 
	 * @param metaId
	 * @param checksum
	 * @param create
	 * @return
	 */
	public CloneChunk getChunk(byte[] metaId, byte[] checksum, boolean create) {
		CloneChunk chunk = chunkCache.get(metaId, checksum);

		// In cache; return
		if (chunk != null) {
			return chunk;
		}

		// Not in cache: add to database
		chunk = new CloneChunk();
		chunk.setChecksum(checksum);
		chunk.setMetaId(metaId);

		// Add to cache
		chunkCache.add(chunk);

		return chunk;
	}

	public CloneChunk getChunk(String chunkIdStr, boolean create) {
		byte[] metaId = CloneChunk.decodeMetaId(chunkIdStr);
		byte[] checksum = CloneChunk.decodeChecksum(chunkIdStr);

		return getChunk(metaId, checksum, create);
	}

	/*
	 * public void persist(Object... objects) { // TODO: SAVE DATABASE }
	 * 
	 * public Object merge(Object obj) { // TODO: LOOK FOR CHANGES AND ADD TO
	 * DATABASE return null; }
	 * 
	 * public void remove(Object... objects) { // TODO: MAYBE IMPLEMENT //
	 * EntityManager em = config.getDatabase().getEntityManager(); // boolean tx
	 * = em.getTransaction().isActive(); // // if (!tx) { //
	 * em.getTransaction().begin(); // } // // for (Object o : objects) { //
	 * em.remove(em.merge(o)); // } // // if (!tx) { // em.flush(); //
	 * em.getTransaction().commit(); // } }
	 */

	public List<CloneFile> getHistory() {
		return cloneFileTree.getAllFiles();
	}

	public List<CloneFile> getAddedCloneFiles() {
		List<CloneFile> ret = new LinkedList<CloneFile>(cloneFileTree.getNewAddedFile());
		return ret;
	}
	
	public void clearNewAddedFiles(){
		cloneFileTree.clearNewAddedList(); // clear new file records
	}

	public CloneFile getParent(CloneFile childFile) {
		File parentFile = childFile.getFile().getParentFile();
		String parentName = parentFile.getName();
//		String parentPath = toDatabasePath(FileUtil.getRelativeParentDirectory(
//				childFile.getRoot().getLocalFile(), parentFile));
		String parentPath = toDatabasePath(childFile.getFile().getParent());

		if (parentPath != null && !parentPath.equals(""))
			parentPath += Constants.DATABASE_FILE_SEPARATOR + parentName;
		else
			parentPath = parentName;

		CloneFileTreeItem c = cloneFileTree.getFileByPath(parentPath);
		if (c == null || c.getCloneFile() == null
				|| c.getCloneFile().getStatus() == Status.DELETED
				|| c.getCloneFile().getStatus() == Status.MERGED)
			return null;
		else
			return c.getCloneFile();
	}

	public static String toDatabasePath(String filesystemPath) {
		return convertPath(filesystemPath, File.separator,
				Constants.DATABASE_FILE_SEPARATOR);
	}

	public static String toFilesystemPath(String databasePath) {
		return convertPath(databasePath, Constants.DATABASE_FILE_SEPARATOR,
				File.separator);
	}

	private static String convertPath(String fromPath, String fromSep, String toSep) {
		String toPath = fromPath.replace(fromSep, toSep);

		// Trim (only at the end!)
		while (toPath.endsWith(toSep)) {
			toPath = toPath.substring(0, toPath.length() - toSep.length());
		}

		return toPath;
	}

	public void merge(CloneFile c) {
		cloneFileTree.updateFile(c);
		// TODO improve for performance
		DatabaseIO.writeCompleteCloneFileTree(cloneFileTree);
	}

	public void persist(CloneFile c) {
		try {
			cloneFileTree.addCloneFile(c);
			// TODO improve for performance
			DatabaseIO.writeCompleteCloneFileTree(cloneFileTree);
		} catch (CloneTreeException e) {
			// TODO handle with daemon
			e.printStackTrace();
		}
	}
	
	public void cleanChunkCache() {
		chunkCache.cleanChunkCache();
	}
	
	
	// For Debug - Prints all versions in CloneFileTree
	public void printAllVersionsInCloneFileTree() {
		for(CloneFile cf : cloneFileTree.getHistory()) {
			if(cf!=null) System.out.println("cloneFileTree-Item.toString: "+cf.toString());
		}
	}
}