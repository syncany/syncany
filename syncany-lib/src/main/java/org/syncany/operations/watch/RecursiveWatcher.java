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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The recursive file watcher monitors a folder (and its sub-folders). 
 * 
 * <p>The class walks through the file tree and registers to a watch to every sub-folder.
 * For new folders, a new watch is registered, and stale watches are removed.
 * 
 * <p>When a file event occurs, a timer is started to wait for the file operations
 * to settle. It is reset whenever a new event occurs. When the timer times out,
 * an event is thrown through the {@link WatchListener}.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class RecursiveWatcher {
	private static final Logger logger = Logger.getLogger(RecursiveWatcher.class.getSimpleName());

	private Path root;
	private List<Path> ignorePaths;
	private int settleDelay;
	private WatchListener listener;

	private AtomicBoolean running;
	
	private WatchService watchService;
	private Thread watchThread;
	private Map<Path, WatchKey> watchPathKeyMap;

	private Timer timer;

	public RecursiveWatcher(Path root, List<Path> ignorePaths, int settleDelay, WatchListener listener) {
		this.root = root;
		this.ignorePaths = ignorePaths;
		this.settleDelay = settleDelay;
		this.listener = listener;

		this.running = new AtomicBoolean(false);
		
		this.watchService = null;
		this.watchThread = null;
		this.watchPathKeyMap = new HashMap<Path, WatchKey>();

		this.timer = null;
	}

	/**
	 * Starts the watcher service and registers watches in all of the sub-folders of
	 * the given root folder.
	 * 
	 * <p><b>Important:</b> This method returns immediately, even though the watches
	 * might not be in place yet. For large file trees, it might take several seconds
	 * until all directories are being monitored. For normal cases (1-100 folders), this
	 * should not take longer than a few milliseconds. 
	 */
	public void start() throws IOException {
		watchService = FileSystems.getDefault().newWatchService();

		watchThread = new Thread(new Runnable() {
			@Override
			public void run() {
				running.set(true);
				walkTreeAndSetWatches();

				while (running.get()) {
					try {
						WatchKey watchKey = watchService.take();
						watchKey.pollEvents(); // Take events, but don't care what they are!

						watchKey.reset();
						resetWaitSettlementTimer();
					}
					catch (InterruptedException | ClosedWatchServiceException e) {
						running.set(false);
					}
				}
			}
		}, "Watcher");
		
		watchThread.start();
	}
	
	public synchronized void stop() {
		if (watchThread != null) {
			try {
				watchService.close();
				running.set(false);
				watchThread.interrupt();
			}
			catch (IOException e) {
				// Don't care
			}			
		}		
	}

	private synchronized void resetWaitSettlementTimer() {
		logger.log(Level.FINE, "File system events registered. Waiting " + settleDelay + "ms for settlement ....");

		if (timer != null) {
			timer.cancel();
			timer = null;
		}

		timer = new Timer("WatchTimer");
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				logger.log(Level.INFO, "File system actions (on watched folders) settled. Updating watches ...");
				walkTreeAndSetWatches();
				unregisterStaleWatches();

				fireListenerEvents();
			}
		}, settleDelay);
	}

	private synchronized void walkTreeAndSetWatches() {
		logger.log(Level.INFO, "Registering new folders at watch service ...");

		try {
			Files.walkFileTree(root, new FileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					if (ignorePaths.contains(dir)) {
						return FileVisitResult.SKIP_SUBTREE;
					}
					else {
						registerWatch(dir);
						return FileVisitResult.CONTINUE;
					}
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		}
		catch (IOException e) {
			// Don't care
		}
	}

	private synchronized void unregisterStaleWatches() {
		Set<Path> paths = new HashSet<Path>(watchPathKeyMap.keySet());
		Set<Path> stalePaths = new HashSet<Path>();
		
		for (Path path : paths) {
			if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
				stalePaths.add(path);
			}
		}
		
		if (stalePaths.size() > 0) {
			logger.log(Level.INFO, "Cancelling stale path watches ...");
			
			for (Path stalePath : stalePaths) {
				unregisterWatch(stalePath);
			}
		}
	}

	private synchronized void fireListenerEvents() {
		if (listener != null) {
			logger.log(Level.INFO, "- Firing watch event (watchEventsOccurred) ...");
			listener.watchEventsOccurred();
		}
	}

	private synchronized void registerWatch(Path dir) {
		if (!watchPathKeyMap.containsKey(dir)) {
			logger.log(Level.INFO, "- Registering " + dir);

			try {
				WatchKey watchKey = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW);
				watchPathKeyMap.put(dir, watchKey);
			}
			catch (IOException e) {
				// Don't care!
			}
		}
	}

	private synchronized void unregisterWatch(Path dir) {
		WatchKey watchKey = watchPathKeyMap.get(dir);

		if (watchKey != null) {
			logger.log(Level.INFO, "- Cancelling " + dir);
			
			watchKey.cancel();
			watchPathKeyMap.remove(dir);
		}
	}

	public interface WatchListener {
		public void watchEventsOccurred();
	}
}
