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
package org.syncany.watch.remote;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Profile;
import org.syncany.config.Settings;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.db.CloneClient;
import org.syncany.db.CloneFile;
import org.syncany.db.Database;
import org.syncany.exceptions.InconsistentFileSystemException;
import org.syncany.exceptions.RemoteFileNotFoundException;
import org.syncany.exceptions.StorageException;
import org.syncany.watch.remote.files.RemoteFile;
import org.syncany.watch.remote.files.StructuredFileList;
import org.syncany.watch.remote.files.UpdateFile;

/**
 * Does periodical checks on the online storage, and applies them locally.
 * 
 * <p>
 * This currently includes the following steps:
 * <ul>
 * <li>List files, identify available update files
 * <li>Download required update files
 * <li>Identify conflicts and apply updates/changes
 * <li>Create local update files
 * <li>Upload local update file
 * <li>Delete old update files online
 * </ul>
 * 
 * Unlike the {@link StorageManager}, this class uses its own
 * {@link TransferManager} to be able to synchronously wait for the storage.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class RemoteWatcher {
	/**
	 * 
	 */
	private static final Logger logger = Logger.getLogger(RemoteWatcher.class
			.getSimpleName());
	private static final int DEFAULT_INTERVAL = 10000;
	private static final boolean DEBUG_GZIP_AND_ENCRYPT_UPDATE_FILES = false;

	private Database db;
	private int interval = DEFAULT_INTERVAL;
	private Profile profile;
	private ChangeManager changeManager;
	private Timer timer;
	private Map<String, RemoteFile> remoteFileList;
	private StructuredFileList fileList;
	private UpdateQueue updateList;
	private TransferManager transfer;

	private Long lastFileVersionCount;
	// TODO this should be in the DB cached somewhere.
	private Date lastRepoFileUpdate;
	// TODO this should be in the DB cached somewhere.
	private Date lastUpdateFileDate;


	public RemoteWatcher(Profile profile) {
		this.changeManager = new ChangeManager(profile);
		this.timer = null;
		this.lastFileVersionCount = null;

		// cp. start()
		this.db = null;

		// cp. doUpdateCheck()
		this.remoteFileList = null;
		this.updateList = null;
		this.transfer = null;
	}

	public synchronized void setInterval(int interval) {
		if (interval >= 1000) {
			this.interval = interval;
		}
		startUpdateCheck();
	}

	public int getInterval() {
		return this.interval;
	}

	public synchronized void start() {
		// Dependencies
		if (db == null) {
			db = Database.getInstance();
		}

		// Reset connection
		reset();
		startUpdateCheck();
	}

	public synchronized void stop() {
		if (timer == null) {
			return;
		}

		timer.cancel();
		timer = null;
	}

	private void startUpdateCheck() {
		logger.log(Level.INFO, "Staring remote watcher...");
		if (timer != null) {
			try {
				timer.cancel();
			} catch (IllegalStateException ex) {
				logger.log(Level.SEVERE, ex.getMessage());
			}
		}
		timer = new Timer("RemoteWatcher");
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				logger.log(Level.INFO, "Do Update Check Interval = " + interval);
				try {
					doUpdateCheck();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}, 0, interval);
	}

	private void reset() {
		profile = Profile.getInstance();
		transfer = profile.getRepository().getConnection().createTransferManager();
		updateList = new UpdateQueue();
	}

	private void doUpdateCheck() throws ClassNotFoundException {
		if (logger.isLoggable(Level.INFO)) {
			logger.info("STARTING PERIODIC UPDATE CHECK ...");
		}

		reset();

		try {
			updateFileList();

			// check for merging
			// cleanup process, merge multiple db files to one/n remote repositories
			if (mergeUpdatesIfPossible()) // merge executed, so refresh filelist
				updateFileList();

			// 3. Analyzing updates & looking for conflicts
			processUpdates();

			// 4. Create and upload local updates ///////
			commitLocalUpdateFile();
		} catch (StorageException ex) {
			logger.log(
					Level.WARNING,
					"Update check failed. Trying again in a couple of seconds.",
					ex);
		} catch (IOException e) {
			logger.log(
					Level.SEVERE,
					"Update check failed. Trying again in a couple of seconds.",
					e);
		} finally {
			if (logger.isLoggable(Level.INFO)) {
				logger.info("DONE WITH PERIODIC UPDATE CHECK ...");
			}

			try {
				transfer.disconnect();
			} catch (StorageException ex) { /* Fressen! */
			}
		}

	}

	// TODO: export to config file
	final static int MAX_UPDATE_FILES = 3;

	// final static long MAX_UPDATE_FILE_SIZE = 1024*1024;

	private boolean mergeUpdatesIfPossible()
			throws RemoteFileNotFoundException, StorageException, IOException, ClassNotFoundException {

		Collection<UpdateFile> remoteUpdates = fileList
				.getLocalUpdateFilesList();
		if (logger.isLoggable(Level.INFO)) {
			logger.info("STARTING MERGE CHECK ...");
		}

		int c = 0;
		List<UpdateFile> updateFilesToMerge = new LinkedList<UpdateFile>();
		// count files smaller than MAX_UPDATE_FILE_SIZE
		for (UpdateFile uf : remoteUpdates) {
			if (uf.getSize() < MAX_UPDATE_FILES) {
				updateFilesToMerge.add(uf);
				c++;
			}
		}

		if (c > MAX_UPDATE_FILES) {
			// need for merging
			// TODO: care for filesize in merging, maybe build more than one
			// merge file
			if (logger.isLoggable(Level.INFO)) {
				logger.info("STARTING MERGE OF " + c + " UPDATE FILES ...");
			}
			// get the latestUpdate of the files to be merged
			Date latestUpdate = null;
			for (UpdateFile uf : updateFilesToMerge) {
				if (latestUpdate == null
						|| uf.getLastUpdate().after(latestUpdate))
					latestUpdate = uf.getLastUpdate();
			}

			UpdateFile newUpdateFile = new UpdateFile(profile.getRepository(),
					Settings.getInstance().getMachineName(), latestUpdate);
			File localUpdateFile = Profile
					.getInstance()
					.getCache()
					.createTempFile(
							"update-" + Settings.getInstance().getMachineName());
			ObjectOutputStream oos = newUpdateFile.getObjectOutputStream(
					localUpdateFile, DEBUG_GZIP_AND_ENCRYPT_UPDATE_FILES);

			for (UpdateFile uf : updateFilesToMerge) {

				File tempUpdateFile = Profile
						.getInstance()
						.getCache()
						.createTempFile(
								"update-"
										+ Settings.getInstance()
												.getMachineName() + "-tomerge");
				transfer.download(uf, tempUpdateFile);

				// Read & delete update file
				if (logger.isLoggable(Level.INFO)) {
					logger.log(Level.INFO, "Merging update for ''{0}'' ...", uf);
				}

				UpdateFile.append(tempUpdateFile,
						DEBUG_GZIP_AND_ENCRYPT_UPDATE_FILES, oos);
				oos.flush();
				tempUpdateFile.delete();

			}

			oos.close();

			for (UpdateFile uf : updateFilesToMerge)
				transfer.delete(uf);

			transfer.upload(localUpdateFile, newUpdateFile);

			return true;
		}

		if (logger.isLoggable(Level.INFO)) {
			logger.info("FINISHED MERGE CHECK ...");
		}

		return false;
	}

	private void updateFileList() throws StorageException {
		remoteFileList = transfer.list();
		fileList = new StructuredFileList(profile.getRepository(),
				remoteFileList);
	}

	private Map<CloneClient, UpdateFile> downloadUpdates()
			throws StorageException {
		Map<CloneClient, UpdateFile> remoteUpdates = new HashMap<CloneClient, UpdateFile>();

		if (logger.isLoggable(Level.INFO)) {
			logger.info("2. Downloading update lists ...");
		}

		// Find newest client update files
		Collection<UpdateFile> newestUpdateFiles = fileList
				.getRemoteUpdateFiles().values();

		for (UpdateFile updateFile : newestUpdateFiles) {
			// Get client from DB (or create it!)
			CloneClient client = db
					.getClient(updateFile.getMachineName(), true);

			// Ignore if we are up-to-date
			if (client.getLastUpdate() != null
					&& !updateFile.getLastUpdate()
							.after(client.getLastUpdate())) {
				if (logger.isLoggable(Level.INFO)) {
					logger.log(Level.INFO, "   - Client ''{0}'' is up-to-date",
							updateFile.getMachineName());
				}

				continue;
			}

			try {
				// Download update file
				if (logger.isLoggable(Level.INFO)) {
					logger.log(Level.INFO,
							"   - Downloading update for ''{0}'' ...",
							client.getMachineName());
				}

				File tempUpdateFile = Profile.getInstance().getCache()
						.createTempFile("update-" + client.getMachineName());
				transfer.download(updateFile, tempUpdateFile);

				// Read & delete update file
				if (logger.isLoggable(Level.INFO)) {
					logger.log(Level.INFO,
							"     --> Parsing update for ''{0}'' ...",
							client.getMachineName());
				}

				updateFile.read(tempUpdateFile,
						DEBUG_GZIP_AND_ENCRYPT_UPDATE_FILES);
				tempUpdateFile.delete();

				// Add to update manager

				remoteUpdates.put(client, updateFile);
			} catch (Exception ex) {
				if (logger.isLoggable(Level.WARNING)) {
					logger.log(
							Level.WARNING,
							"Reading update file of client {0} failed. Skipping update check.",
							client.getMachineName());
				}

				throw new StorageException(ex);
			}
		}

		return remoteUpdates;
	}

	private void processUpdates() throws StorageException {
		Map<CloneClient, UpdateFile> remoteUpdates = downloadUpdates(); // TODO Philipp XXXXXXXXXXX pro client eine?? --> müssten mehrere sein!
                                                                        // TODO Philipp XXXXXXXXXXX mehrere pro client --> zu einer zusammen aneinanderhängen
		// Skip if there are no remote updates!
		if (remoteUpdates.isEmpty()) {
			if (logger.isLoggable(Level.INFO)) {
				logger.info("NO NEW UPDATES IN REPO: Skipping.");
			}

			return;
		}

		// Repeat the processUpdates()-method until we don't get
		// InconsistentFileSystem
		// exceptions any more. We wait 2sec to give the indexer time to catch
		// up!
		boolean done = false;

		while (!done) {
			// Make update list
			updateList = new UpdateQueue();

			for (Map.Entry<CloneClient, UpdateFile> e : remoteUpdates
					.entrySet()) {
				updateList.addRemoteUpdateFile(e.getKey(), e.getValue()); // TODO Philipp XXXX entweder mehrere pro client adden oder die gemergte datei
			}

			try {
				changeManager.processUpdates(updateList);
				done = true;
			} catch (InconsistentFileSystemException ex) {
				logger.log(
						Level.SEVERE,
						"Inconsistent file system. Waiting 2sec (for indexer) and trying again.",
						ex);

				try {
					Thread.sleep(2000);
				} catch (InterruptedException ex1) {
				}
			}
		}

		if (logger.isLoggable(Level.INFO)) {
			logger.info("DONE APPLYING UPDATES !");
		}

		// TODO should the changes be synchronous?
		// TODO because setting the clients' lastUpdate value assumes that the
		// change mgr doesnt crash

		// Update last-updated date of clients
		if (logger.isLoggable(Level.INFO)) {
			logger.info("3b. Updating client DB entries ...");
		}

		for (Map.Entry<CloneClient, UpdateFile> e : updateList
				.getRemoteUpdateFiles().entrySet()) {
			CloneClient client = e.getKey();
			UpdateFile updateFile = e.getValue();

			if (client.getLastUpdate() == null
					|| client.getLastUpdate()
							.before(updateFile.getLastUpdate())) {
				client.setLastUpdate(updateFile.getLastUpdate());
				client.merge();
			}
		}
	}

	private void commitLocalUpdateFile() throws StorageException {
		// Check if new update file needs to be created/uploaded
		Long fileVersionCount = Database.getInstance().getFileVersionCount();
		logger.info("---- VERSION " + fileVersionCount + " ----");
		
		
		if (fileVersionCount != null && lastFileVersionCount != null
				&& fileVersionCount.equals(lastFileVersionCount)) {
			logger.info("4. No local changes. Skipping step upload.");
			return;
		}

		if (fileVersionCount == null || fileVersionCount == 0) {
			logger.warning("4. Nothing in DB. Skipping step upload.");
			return;
		}

		// Start making/uploading the file
		lastUpdateFileDate = new Date();
		lastFileVersionCount = fileVersionCount;

		File localUpdateFile = null;
		UpdateFile remoteUpdateFile = new UpdateFile(profile.getRepository(),
				Settings.getInstance().getMachineName(), lastUpdateFileDate);

		try {
			// Make temp. update file
			localUpdateFile = Profile
					.getInstance()
					.getCache()
					.createTempFile(
							"update-" + Settings.getInstance().getMachineName());
			logger.info("4. Writing local changes to '" + localUpdateFile
					+ "' ...");

			// changed due to profile deletion
			List<CloneFile> updatedFiles = db.getAddedCloneFiles(); // clear only if need
			remoteUpdateFile.setVersions(updatedFiles);
			remoteUpdateFile.write(localUpdateFile,
					DEBUG_GZIP_AND_ENCRYPT_UPDATE_FILES);

			// Upload
			logger.info("  - Uploading file to temp. file '"
					+ remoteUpdateFile.getName() + "' ...");
			transfer.upload(localUpdateFile, remoteUpdateFile);

			localUpdateFile.delete();
			
			db.clearNewAddedFiles();
		} catch (IOException ex) {
			if (localUpdateFile != null) {
				localUpdateFile.delete();
			}

			logger.log(Level.SEVERE, null, ex);
		}
	}

	private UpdateFile getLocalUpdates() {
		try {
			UpdateFile localUpdateFile = new UpdateFile(
					profile.getRepository(), Settings.getInstance()
							.getMachineName(), new Date());

			// Make temp. update file
			File tempLocalUpdateFile = Profile
					.getInstance()
					.getCache()
					.createTempFile(
							"update-" + Settings.getInstance().getMachineName());
			logger.info("4. Writing local changes to '" + tempLocalUpdateFile
					+ "' ...");

			// changed due to profile deletion
			List<CloneFile> updatedFiles = db.getAddedCloneFiles();
			localUpdateFile.setVersions(updatedFiles);
			localUpdateFile.write(tempLocalUpdateFile,
					DEBUG_GZIP_AND_ENCRYPT_UPDATE_FILES);
			localUpdateFile.read(tempLocalUpdateFile,
					DEBUG_GZIP_AND_ENCRYPT_UPDATE_FILES);

			List<CloneFile> allFiles = db.getFiles();
			UpdateFile localUpdateFile2 = new UpdateFile(
					profile.getRepository(), Settings.getInstance()
							.getMachineName(), new Date());
			File tempLocalUpdateFile2 = Profile
					.getInstance()
					.getCache()
					.createTempFile(
							"update-" + Settings.getInstance().getMachineName());
			localUpdateFile2.setVersions(allFiles);
			localUpdateFile2.write(tempLocalUpdateFile2,
					DEBUG_GZIP_AND_ENCRYPT_UPDATE_FILES);
			localUpdateFile2.read(tempLocalUpdateFile2,
					DEBUG_GZIP_AND_ENCRYPT_UPDATE_FILES);

			tempLocalUpdateFile.delete();
			
			db.clearNewAddedFiles();
			
			return localUpdateFile;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	
}
