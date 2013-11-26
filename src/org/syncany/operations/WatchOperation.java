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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.operations.NotificationListener.NotificationListenerListener;
import org.syncany.operations.RecursiveWatcher.WatchListener;
import org.syncany.operations.UpOperation.UpOperationResult;
import org.syncany.operations.UpOperation.UpOperationResult.UpResultCode;
import org.syncany.util.StringUtil;

/**
 * The watch operation implements the constant synchronization known from other
 * sync tools. 
 * 
 * <p>In order to sync instantly, it offers the following strategies:
 * <ul>
 *  <li>It monitors the local file system using the {@link RecursiveWatcher}.
 *      Whenever a file or folder changes, the sync is started (after a short
 *      settlement wait period).</li>
 *  <li>It subscribes to a repo-specific channel on the Syncany pub/sub server,
 *      using the {@link NotificationListener}, and publishes updates to this 
 *      channel.</li>
 *  <li>It periodically runs the sync, i.e. the {@link DownOperation} and 
 *      subsequently the {@link UpOperation}. If the other two mechanisms are
 *      disabled or fail to register changes, this method will make sure that
 *      changes are synced eventually.</li>
 * </ul>
 * 
 * As of now, this operation never returns, because it runs in a loop. The user
 * has to manually abort the operation on the command line.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class WatchOperation extends Operation implements NotificationListenerListener, WatchListener {
	private static final Logger logger = Logger.getLogger(WatchOperation.class.getSimpleName());

	private WatchOperationOptions options;

	private Database database;
	private boolean syncRunning;

	private RecursiveWatcher recursiveWatcher;
	private NotificationListener notificationListener;
	
	private String notificationChannel;
	private String notificationInstanceId;

	public WatchOperation(Config config, WatchOperationOptions options) {
		super(config);

		this.options = options;

		this.database = null;
		this.syncRunning = false;
		
		this.recursiveWatcher = null;
		this.notificationListener = null;
		
		this.notificationChannel = StringUtil.toHex(config.getRepoId());
		this.notificationInstanceId = ""+Math.abs(new Random().nextLong());
	}

	@Override
	public WatchOperationResult execute() throws Exception {
		database = loadLocalDatabase();

		if (options.announcementsEnabled()) {
			startNotificationListener();
		}
		
		if (options.watcherEnabled()) {
			startRecursiveWatcher();
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

	private void startRecursiveWatcher() {
		Path localDir = Paths.get(config.getLocalDir().getAbsolutePath());
		List<Path> ignorePaths = new ArrayList<Path>();
		
		ignorePaths.add(Paths.get(config.getAppDir().getAbsolutePath()));
		ignorePaths.add(Paths.get(config.getCacheDir().getAbsolutePath()));
		ignorePaths.add(Paths.get(config.getDatabaseDir().getAbsolutePath()));
		ignorePaths.add(Paths.get(config.getLogDir().getAbsolutePath()));
		
		recursiveWatcher = new RecursiveWatcher(localDir, ignorePaths, options.getSettleDelay(), this);
		
		try {
			recursiveWatcher.start();
		}
		catch (IOException e) {
			logger.log(Level.WARNING, "Cannot initiate file watcher. Relying on regular tree walks.");
		}
	}

	private void startNotificationListener() {
		notificationListener = new NotificationListener(options.getAnnouncementsHost(), options.getAnnouncementsPort(), this);
		notificationListener.start();
		
		notificationListener.subscribe(notificationChannel);
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
	public void pushNotificationReceived(String channel, String message) {
		if (channel.equals(notificationChannel) && !message.equals(notificationInstanceId)) {
			try {
				runSync();
			}
			catch (Exception e) {
				logger.log(Level.INFO, "Sync FAILED (event-triggered).");
			}
		}
	}	

	@Override
	public void watchEventsOccurred() {
		try {
			runSync();
		}
		catch (Exception e) {
			logger.log(Level.INFO, "Sync FAILED (event-triggered).");
		}
	}

	private void notifyChanges() {
		if (notificationListener != null) {
			notificationListener.announce(notificationChannel, notificationInstanceId);
		}
	}

	public static class WatchOperationOptions implements OperationOptions {
		private int interval = 120000;
		private boolean announcements = true;
		private String announcementsHost = "notify.syncany.org";
		private int announcementsPort = 8080;
		private int settleDelay = 3000;
		private boolean watcher = true;

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

		public int getSettleDelay() {
			return settleDelay;
		}

		public void setSettleDelay(int settleDelay) {
			this.settleDelay = settleDelay;
		}

		public boolean watcherEnabled() {
			return watcher;
		}

		public void setWatcher(boolean watcher) {
			this.watcher = watcher;
		}
	}

	public static class WatchOperationResult implements OperationResult {
		// Fressen
	}
}
