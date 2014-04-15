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
package org.syncany.operations.watch;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.operations.Operation;
import org.syncany.operations.OperationOptions;
import org.syncany.operations.OperationResult;
import org.syncany.operations.down.DownOperation;
import org.syncany.operations.down.DownOperationListener;
import org.syncany.operations.down.DownOperationResult;
import org.syncany.operations.down.DownOperationResult.DownResultCode;
import org.syncany.operations.up.UpOperation;
import org.syncany.operations.up.UpOperationListener;
import org.syncany.operations.up.UpOperationResult;
import org.syncany.operations.up.UpOperationResult.UpResultCode;
import org.syncany.operations.watch.NotificationListener.NotificationListenerListener;
import org.syncany.operations.watch.RecursiveWatcher.WatchListener;
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
	private WatchOperationListener listener;

	private AtomicBoolean syncRunning;
	private AtomicBoolean stopRequired;
	private AtomicBoolean pauseRequired;

	private RecursiveWatcher recursiveWatcher;
	private NotificationListener notificationListener;

	private String notificationChannel;
	private String notificationInstanceId;

	public WatchOperation(Config config, WatchOperationOptions options, WatchOperationListener listener) {
		super(config);

		this.options = options;
		this.listener = listener;

		this.syncRunning = new AtomicBoolean(false);
		this.stopRequired = new AtomicBoolean(false);
		this.pauseRequired = new AtomicBoolean(false);

		this.recursiveWatcher = null;
		this.notificationListener = null;

		this.notificationChannel = StringUtil.toHex(config.getRepoId());
		this.notificationInstanceId = "" + Math.abs(new Random().nextLong());
	}

	@Override
	public WatchOperationResult execute() throws Exception {
		if (options.announcementsEnabled()) {
			startNotificationListener();
		}

		if (options.watcherEnabled()) {
			startRecursiveWatcher();
		}

		while (!stopRequired.get()) {
			while (pauseRequired.get()) {
				try {
					Thread.sleep(1000);
				}
				catch (Exception e) {
					// Don't care
				}
			}

			try {
				runSync();

				logger.log(Level.INFO, "Sync done, waiting {0} seconds ...", options.getInterval() / 1000);
				Thread.sleep(options.getInterval());
			}
			catch (Exception e) {
				logger.log(Level.INFO, String.format("Sync FAILED, waiting %d seconds ...", options.getInterval() / 1000), e);
				Thread.sleep(options.getInterval());
			}
		}

		return new WatchOperationResult();
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

	private void runSync() throws Exception {
		if (!syncRunning.get()) {
			syncRunning.set(true);

			logger.log(Level.INFO, "Running sync ...");

			try {
				// Run down
				DownOperationResult downResult = new DownOperation(config, listener).execute();
				
				if (downResult.getResultCode() == DownResultCode.OK_WITH_REMOTE_CHANGES) {
					// TODO [low] Do something?
				}
				
				// Run up
				UpOperationResult upOperationResult = new UpOperation(config, listener).execute();

				if (upOperationResult.getResultCode() == UpResultCode.OK_APPLIED_CHANGES && upOperationResult.getChangeSet().hasChanges()) {
					notifyChanges();
				}
			}
			finally {
				syncRunning.set(false);
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

	public void pause() {
		pauseRequired.set(true);
	}

	public void resume() {
		pauseRequired.set(false);
	}

	public void stop() {
		stopRequired.set(true);
	}

	/**
	 * @author Vincent Wiencek
	 */
	public interface WatchOperationListener extends UpOperationListener, DownOperationListener {
		// Nothing
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
