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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.Operation;
import org.syncany.operations.cleanup.CleanupOperation;
import org.syncany.operations.cleanup.CleanupOperationResult;
import org.syncany.operations.cleanup.CleanupOperationResult.CleanupResultCode;
import org.syncany.operations.down.DownOperation;
import org.syncany.operations.down.DownOperationResult;
import org.syncany.operations.down.DownOperationResult.DownResultCode;
import org.syncany.operations.up.UpOperation;
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
	private static final int STOP_GRACE_PERIOD = 15*1000; 
	
	private WatchOperationOptions options;
	
	private SqlDatabase localDatabase;

	private Thread watchThread;
	private AtomicBoolean syncRunning;
	private AtomicBoolean syncRequested;
	private AtomicBoolean stopRequested;
	private AtomicBoolean pauseRequested;
	private AtomicInteger upCount;

	private RecursiveWatcher recursiveWatcher;
	private NotificationListener notificationListener;

	private String notificationChannel;
	private String notificationInstanceId;

	public WatchOperation(Config config, WatchOperationOptions options) {
		super(config);

		this.options = options;

		this.localDatabase = new SqlDatabase(config);
		
		this.watchThread = null;
		this.syncRunning = new AtomicBoolean(false);
		this.syncRequested = new AtomicBoolean(false);
		this.stopRequested = new AtomicBoolean(false);
		this.pauseRequested = new AtomicBoolean(false);
		this.upCount = new AtomicInteger(0);

		this.recursiveWatcher = null;
		this.notificationListener = null;

		this.notificationChannel = StringUtil.toHex(config.getRepoId());
		this.notificationInstanceId = "" + Math.abs(new Random().nextLong());
	}

	@Override
	public WatchOperationResult execute() {
		watchThread = Thread.currentThread();
		
		if (options.announcementsEnabled()) {
			startNotificationListener();
		}

		if (options.watcherEnabled()) {
			startRecursiveWatcher();
		}

		syncLoop: while (!stopRequested.get()) {
			try {
				waitWhilePaused();
			}
			catch (InterruptedException e) {
				logger.log(Level.INFO, "Sleep INTERRUPTED during PAUSE. STOPPING.", e);
				break syncLoop;
			}

			try {
				runSync();

				if (!syncRequested.get() && !pauseRequested.get() && !stopRequested.get()) {
					logger.log(Level.INFO, "Sync done, waiting {0} seconds ...", options.getInterval() / 1000);
					Thread.sleep(options.getInterval());
				}
			}
			catch (InterruptedException e) {
				logger.log(Level.INFO, "Sync loop INTERRUPTED. STOPPING.", e);
				break syncLoop;
			}
			catch (Exception e) {
				if (pauseRequested.get()) {
					logger.log(Level.INFO, "Sync FAILED, but PAUSE requested. Normally we would wait a bit and try again, but in this case we don't.", e);					
				}
				else if (stopRequested.get()) {
					logger.log(Level.INFO, "Sync FAILED, but STOP requested.", e);
					break syncLoop;
				}
				else {
					logger.log(Level.INFO, String.format("Sync FAILED, waiting %d seconds ...", options.getInterval() / 1000), e);

					try {
						Thread.sleep(options.getInterval());
					}
					catch (InterruptedException e2) {
						logger.log(Level.INFO, "Sleep INTERRUPTED during retry-wait. STOPPING.", e2);
						break syncLoop;
					}
				}
			}
		}
		
		if (options.announcementsEnabled()) {
			stopNotificationListener();
		}

		if (options.watcherEnabled()) {
			stopRecursiveWatcher();
		}
		
		localDatabase.shutdown();
		
		return new WatchOperationResult();
	}

	private void startRecursiveWatcher() {
		logger.log(Level.INFO, "Starting recursive watcher for " + config.getLocalDir() + " ...");
		
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
		logger.log(Level.INFO, "Starting notification listener for " + config.getLocalDir() + " ...");
		
		notificationListener = new NotificationListener(options.getAnnouncementsHost(), options.getAnnouncementsPort(), this);
		notificationListener.start();

		notificationListener.subscribe(notificationChannel);
	}
	
	private void stopRecursiveWatcher() {
		logger.log(Level.INFO, "Stopping recursive watcher for " + config.getLocalDir() + " ...");
		recursiveWatcher.stop();
	}

	private void stopNotificationListener() {
		logger.log(Level.INFO, "Stopping notification listener for " + config.getLocalDir() + " ...");
		notificationListener.stop();
	}

	/**
	 * Runs one iteration of the main synchronization loop, containing a {@link DownOperation},
	 * an {@link UpOperation} and (if required), a {@link CleanupOperation}. 
	 */
	private void runSync() throws Exception {
		if (!syncRunning.get()) {
			syncRunning.set(true);
			syncRequested.set(false);

			logger.log(Level.INFO, "RUNNING SYNC ...");
			
			try {
				boolean notifyChanges = false;
				
				// Run down
				DownOperationResult downResult = new DownOperation(config, options.getDownOptions()).execute();
				
				if (downResult.getResultCode() == DownResultCode.OK_WITH_REMOTE_CHANGES) {
					// TODO [low] Do something?
				}
				
				// Run up
				UpOperationResult upOperationResult = new UpOperation(config, options.getUpOptions()).execute();

				if (upOperationResult.getResultCode() == UpResultCode.OK_CHANGES_UPLOADED && upOperationResult.getChangeSet().hasChanges()) {
					upCount.incrementAndGet();
					notifyChanges = true;
				}		

				CleanupOperationResult cleanupOperationResult = new CleanupOperation(config, options.getCleanupOptions()).execute();
				
				if (cleanupOperationResult.getResultCode() == CleanupResultCode.OK) {
					notifyChanges = true;
				}
				
				// Fire change event if up and/or cleanup  
				if (notifyChanges) {
					notifyChanges();
				}
			}
			finally {
				logger.log(Level.INFO, "SYNC DONE.");
				syncRunning.set(false);
			}
		}
		else {
			// Can't do a log message here, because this bit is called thousand 
			// of times when file system events occur.
			
			syncRequested.set(true);
		}
	}

	@Override
	public void pushNotificationReceived(String channel, String message) {
		if (channel.equals(notificationChannel) && !message.equals(notificationInstanceId)) {
			try {
				waitWhilePaused();
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
			waitWhilePaused();
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
		pauseRequested.set(true);
	}

	public void resume() {
		pauseRequested.set(false);
	}

	public void stop() {
		if (!stopRequested.get()) {
			stopRequested.set(true);

			if (syncRunning.get()) {
				logger.log(Level.INFO, "Stop requested, but sync process currently running. Waiting max. " + STOP_GRACE_PERIOD + "ms for sync to finish.");
				scheduleForceKill();
			}
			else {
				logger.log(Level.INFO, "Stop requested, but sync is NOT running. Immediately stopping thread.");
				forceKillWatchThread();
			}
		}
		else {
			logger.log(Level.INFO, "Stop requested AGAIN, but was requested before. IGNORING.");
		}
	}
	
	public boolean isSyncRunning() {
		return syncRunning.get();
	}

	public boolean isSyncRequested() {
		return syncRequested.get();
	}
	
	private void waitWhilePaused() throws InterruptedException {
		while (pauseRequested.get()) {
			Thread.sleep(1000);
		}
	}

	private void scheduleForceKill() {
		String killTimerName = "Kill/" + config.getLocalDir().getName();
		
		new Timer(killTimerName).schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					logger.log(Level.INFO, "STOP GRACE PERIOD OVER. STOPPING WATCH " + config.getLocalDir() + " ...");
					
					if (watchThread != null && !watchThread.isInterrupted()) {
						watchThread.interrupt();
					}
				}
				catch (Exception e) {
					logger.log(Level.INFO, "Forcefully stopping watch thread FAILED at " + config.getLocalDir() + ". Giving up.");
				}
			}
		}, STOP_GRACE_PERIOD);
	}

	private void forceKillWatchThread() {
		try {
			logger.log(Level.INFO, "STOPPING WATCH " + config.getLocalDir() + " ...");
			
			if (watchThread != null && !watchThread.isInterrupted()) {
				watchThread.interrupt();
			}
		}
		catch (Exception e) {
			logger.log(Level.INFO, "Forcefully stopping watch thread FAILED at " + config.getLocalDir() + ". Giving up.");
		}
	}
}
