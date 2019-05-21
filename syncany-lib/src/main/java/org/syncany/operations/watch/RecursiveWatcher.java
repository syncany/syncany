/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com>
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
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.util.EnvironmentUtil;

/**
 * The recursive file watcher monitors a folder (and its sub-folders).
 *
 * <p>When a file event occurs, a timer is started to wait for the file operations
 * to settle. It is reset whenever a new event occurs. When the timer times out,
 * an event is thrown through the {@link WatchListener}.
 *
 * <p>This is an abstract class, using several template methods that are called
 * in different lifecycle states: {@link #beforeStart()}, {@link #beforePollEventLoop()},
 * {@link #pollEvents()}, and {@link #afterStop()}.
 *
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
public abstract class RecursiveWatcher {
	protected static final Logger logger = Logger.getLogger(RecursiveWatcher.class.getSimpleName());

	protected Path root;
	protected List<Path> ignorePaths;
	private int settleDelay;
	private WatchListener listener;

	private AtomicBoolean running;

	private Thread watchThread;
	private Timer timer;

	public RecursiveWatcher(Path root, List<Path> ignorePaths, int settleDelay, WatchListener listener) {
		this.root = root;
		this.ignorePaths = ignorePaths;
		this.settleDelay = settleDelay;
		this.listener = listener;

		this.running = new AtomicBoolean(false);
	}

	/**
	 * Creates a recursive watcher for the given root path. The returned watcher
	 * will ignore the ignore paths and fire an event through the {@link WatchListener}
	 * as soon as the settle delay (in ms) has passed.
	 *
	 * <p>The method returns a platform-specific recursive watcher: {@link WindowsRecursiveWatcher}
	 * for Windows and {@link DefaultRecursiveWatcher} for other operating systems.
	 */
	public static RecursiveWatcher createRecursiveWatcher(Path root, List<Path> ignorePaths, int settleDelay, WatchListener listener) {
		if (EnvironmentUtil.isWindows()) {
			return new WindowsRecursiveWatcher(root, ignorePaths, settleDelay, listener);
		}
		else {
			return new DefaultRecursiveWatcher(root, ignorePaths, settleDelay, listener);
		}
	}

	/**
	 * Starts the watcher service and registers watches in all of the sub-folders of
	 * the given root folder.
	 *
	 * <p>This method calls the {@link #beforeStart()} method before everything else.
	 * Subclasses may execute their own commands there. Before the watch thread is started,
	 * {@link #beforePollEventLoop()} is called. And in the watch thread loop,
	 * {@link #pollEvents()} is called.
	 *
	 * <p><b>Important:</b> This method returns immediately, even though the watches
	 * might not be in place yet. For large file trees, it might take several seconds
	 * until all directories are being monitored. For normal cases (1-100 folders), this
	 * should not take longer than a few milliseconds.
	 */
	public void start() throws Exception {
		// Call before-start hook
		beforeStart();

		// Start watcher thread
		watchThread = new Thread(new Runnable() {
			@Override
			public void run() {
				running.set(true);
				beforePollEventLoop(); // Call before-loop hook

				while (running.get()) {
					try {
						boolean relevantEvents = pollEvents();

						if (relevantEvents) {
							restartWaitSettlementTimer();
						}
					}
					catch (InterruptedException e) {
						logger.log(Level.FINE, "Could not poll the events. EXITING watcher.", e);
						running.set(false);
					}
					catch (ClosedWatchServiceException e) {
						logger.log(Level.FINE, "Watch closed or polling failed. EXITING watcher.", e);
						running.set(false);
					}
				}
			}
		}, "Watcher/" + root.toFile().getName());

		watchThread.start();
	}

	/**
	 * Stops the watch thread by interrupting it and subsequently
	 * calls the {@link #afterStop()} template method (to be implemented
	 * by subclasses.
	 */
	public synchronized void stop() {
		if (watchThread != null) {
			try {
				running.set(false);
				watchThread.interrupt();

				// Call after-stop hook
				afterStop();
			}
			catch (IOException e) {
				logger.log(Level.FINE, "Could not close watcher", e);
			}
		}
	}

	private synchronized void restartWaitSettlementTimer() {
		logger.log(Level.FINE, "File system events registered. Waiting " + settleDelay + "ms for settlement ....");

		if (timer != null) {
			timer.cancel();
			timer = null;
		}

		timer = new Timer("FsSettleTim/" + root.toFile().getName());
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				logger.log(Level.INFO, "File system actions (on watched folders) settled. Updating watches ...");

				watchEventsOccurred();
				fireListenerEvents();
			}
		}, settleDelay);
	}

	private synchronized void fireListenerEvents() {
		if (listener != null) {
			logger.log(Level.INFO, "- Firing watch event (watchEventsOccurred) ...");
			listener.watchEventsOccurred();
		}
	}

	/**
	 * Called before the {@link #start()} method. This method is
	 * only called once.
	 */
	protected abstract void beforeStart() throws Exception;

	/**
	 * Called in the watch service polling thread, right
	 * before the {@link #pollEvents()} loop. This method is
	 * only called once.
	 */
	protected abstract void beforePollEventLoop();

	/**
	 * Called in the watch service polling thread, inside
	 * of the {@link #pollEvents()} loop. This method is called
	 * multiple times.
	 */
	protected abstract boolean pollEvents() throws InterruptedException;

	/**
	 * Called in the watch service polling thread, whenever
	 * a file system event occurs. This may be used by subclasses
	 * to (re-)set watches on folders. This method is called
	 * multiple times.
	 */
	protected abstract void watchEventsOccurred();

	/**
	 * Called after the {@link #stop()} method. This method is
	 * only called once.
	 */
	protected abstract void afterStop() throws IOException;

	public interface WatchListener {
		public void watchEventsOccurred();
	}
}
