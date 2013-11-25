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
package org.syncany.operations;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.operations.NotificationListener.NotificationListenerListener;
import org.syncany.operations.UpOperation.UpOperationResult;
import org.syncany.operations.UpOperation.UpOperationResult.UpResultCode;
import org.syncany.util.StringUtil;

public class WatchOperation extends Operation implements NotificationListenerListener {
	private static final Logger logger = Logger.getLogger(WatchOperation.class.getSimpleName());
	
	private WatchOperationOptions options;

	private Database database;
	private NotificationListener notificationListener;
	private boolean syncRunning;

	public WatchOperation(Config config, WatchOperationOptions options) {
		super(config);

		this.options = options;

		this.database = null;
		this.notificationListener = null;
		this.syncRunning = false;
	}

	@Override
	public WatchOperationResult execute() throws Exception {
		database = loadLocalDatabase();

		if (options.announcementsEnabled()) {
			startNotificationListener();
		}

		while (true) {
			try {
				runSync();

				logger.log(Level.INFO, "Sync done, waiting {0} seconds ...", options.getInterval() / 1000);
				Thread.sleep(options.getInterval());
			}
			catch (Exception e) {
				logger.log(Level.INFO, "Sync FAILED, waiting {0} seconds ...", options.getInterval() / 1000);
				Thread.sleep(options.getInterval());
			}
		}
	}

	private void startNotificationListener() {
		notificationListener = new NotificationListener(options.getAnnouncementsHost(), options.getAnnouncementsPort(), this);
		notificationListener.subscribe(StringUtil.toHex(config.getRepoId()));

		notificationListener.start();
	}

	private synchronized void runSync() throws Exception {
		if (!syncRunning) {
			syncRunning = true;

			logger.log(Level.INFO, "Running sync ...");

			try {
				new DownOperation(config, database).execute();

				UpOperationResult upOperationResult = new UpOperation(config, database).execute();

				if (upOperationResult.getResultCode() == UpResultCode.OK_APPLIED_CHANGES && upOperationResult.getChangeSet().hasChanges()) {
					notifyChanges();
				}
			}
			finally {
				syncRunning = false;
			}
		}
	}

	@Override
	public void messageReceived(String channel, String message) {
		try {
			runSync();
		}
		catch (Exception e) {
			logger.log(Level.INFO, "Sync FAILED (event-triggered).");
		}
	}
	

	private void notifyChanges() {
		if (notificationListener != null) {
			notificationListener.announce(StringUtil.toHex(config.getRepoId()), "1"); 
		}
	}

	public static class WatchOperationOptions implements OperationOptions {
		private int interval = 30000;
		private boolean announcements = false;
		private String announcementsHost;
		private int announcementsPort;

		public int getInterval() {
			return interval;
		}

		public void setInterval(int interval) {
			this.interval = interval;
		}

		public boolean announcementsEnabled() {
			return announcements;
		}

		public void setAnnouncements(boolean announcements) {
			this.announcements = announcements;
		}

		public String getAnnouncementsHost() {
			return announcementsHost;
		}

		public void setAnnouncementsHost(String announcementsHost) {
			this.announcementsHost = announcementsHost;
		}

		public int getAnnouncementsPort() {
			return announcementsPort;
		}

		public void setAnnouncementsPort(int announcementsPort) {
			this.announcementsPort = announcementsPort;
		}
	}

	public static class WatchOperationResult implements OperationResult {
		// Fressen
	}
}
