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
