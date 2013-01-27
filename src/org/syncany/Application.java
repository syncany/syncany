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
package org.syncany;

import org.syncany.config.Profile;
import org.syncany.connection.Uploader;
import org.syncany.exceptions.InitializationException;
import org.syncany.index.Indexer;
import org.syncany.watch.remote.ChangeManager;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the application.
 * 
 * <ul>
 * <li>{@link Watcher}: Listens to changes of the file system in the given local
 * sync folder. Passes changes to the indexer.
 * <li>{@link Indexer}: Reads local files and compares them to the versions in
 * local database. If necessary, it creates DB versions of new or altered files
 * and passes them to the storage manager for upload.
 * <li>{@link Uploader}: Uploads and downloads remote files from the shared
 * storage. Receives upload requests by the {@link Indexer}, and download
 * requests by the {@link PeriodicStorageMonitor}.
 * <li>{@link PeriodicStorageMonitor}: Checks the online storage for changes in
 * regular intervals, then downloads changes and notifies the
 * {@link ChangeManager}.
 * </ul>
 * 
 * <p>
 * Medium priority To-Do list:
 * <ul>
 * <li>TODO [medium] Connectivity management: Handle broken connections in every
 * single class
 * <li>TODO [medium] Make checksum long value instead of int, cp.
 * </ul>
 * 
 * <p>
 * Low priority To-Do list:
 * <ul>
 * <li>TODO [low] make platform specific file manager integration (windows
 * explorer, mac finder, ...)
 * <li>TODO [low] cache: implement a cache-cleaning functionality for the local
 * and online storage.
 * <li>TODO [low] cache: implement a cache-size parameter for the local cache.
 * </ul>
 * 
 * <p>
 * Wish list:
 * <ul>
 * <li>TODO [wish] strategy for down/uploading : FIFO, larget first, ...
 * </ul>
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Application {
	private static final Logger logger = Logger.getLogger(Application.class.getSimpleName());

	private Indexer indexer;

	public Application() {
		// Nothing.
	}

	public void start() throws InitializationException {
		if (logger.isLoggable(Level.INFO)) {
			logger.info("Starting Application ...");
		}

		// Do NOT change the order of these method calls!
		// They strongly depend on each other.
		initDependencies();

		// This is done in a thread, so the application can finish
		// initializing. The profile stuff is separate from the rest!
		new Thread(new Runnable() {
			@Override
			public void run() {
				// Profile
				Profile profile = Profile.getInstance();
				
				// We can start the uploader already; it does no harm
				profile.getUploader().start();

				// Synchronously index files and add file system watches
				indexer.index(profile);

				// Start threads
				profile.getRemoteWatcher().start();
			}
		}, "InitProfile").start();
	}

	private void initDependencies() {
		if (logger.isLoggable(Level.INFO)) {
			logger.info("Instantiating dependencies ...");
		}

		indexer = Indexer.getInstance();
	}

	public void doShutdown() {
		logger.info("Shutting down ...");

		Profile.getInstance().getUploader().stop();
		Profile.getInstance().getRemoteWatcher().stop();

		System.exit(0);
	}

}
