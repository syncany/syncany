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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author pheckel
 *
 */
public class RecursiveWatcher {
	private static final Logger logger = Logger.getLogger(RecursiveWatcher.class.getSimpleName());

	private Path root;
	private List<Path> ignorePaths;
	private int settleDelay;
	private WatchListener listener;

	private boolean running;

	private WatchService watchService;
	private Map<WatchKey, Path> watchKeyPathMap;
	private Map<Path, WatchKey> watchPathKeyMap;

	private Timer timer;

	public RecursiveWatcher(Path root, List<Path> ignorePaths, int settleDelay, WatchListener listener) {
		this.root = root;
		this.ignorePaths = ignorePaths;
		this.settleDelay = settleDelay;
		this.listener = listener;

		this.running = false;

		this.watchService = null;
		this.watchKeyPathMap = new HashMap<WatchKey, Path>();
		this.watchPathKeyMap = new HashMap<Path, WatchKey>();

		this.timer = null;
	}

	public void start() throws IOException {
		watchService = FileSystems.getDefault().newWatchService();

		new Thread(new Runnable() {
			@Override
			public void run() {
				running = true;
				walkTreeAndSetWatches();

				while (running) {
					try {
						WatchKey watchKey = watchService.take();
						watchKey.pollEvents(); // Take events, but don't care what they are!

						watchKey.reset();
						resetWaitSettlementTimer();
					}
					catch (InterruptedException e) {
						running = false;
					}
				}
			}
		}, "Watcher").start();
	}

	private synchronized void resetWaitSettlementTimer() {
		logger.log(Level.INFO, "File system events registered. Waiting " + settleDelay + "ms for settlement ....");

		if (timer != null) {
			timer.cancel();
			timer = null;
		}

		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
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
		logger.log(Level.INFO, "Cancelling stale watchers ...");

		Set<Path> paths = new HashSet<Path>(watchPathKeyMap.keySet());

		for (Path dir : paths) {
			if (!Files.exists(dir, LinkOption.NOFOLLOW_LINKS)) {
				unregisterWatch(dir);
			}
		}
	}

	private synchronized void fireListenerEvents() {
		if (listener != null) {
			listener.watchEventsOccurred();
		}
	}

	private synchronized void registerWatch(Path dir) {
		if (!watchPathKeyMap.containsKey(dir)) {
			logger.log(Level.INFO, "- Registering " + dir);

			try {
				WatchKey watchKey = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW);

				watchKeyPathMap.put(watchKey, dir);
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

			watchKeyPathMap.remove(watchKey);
			watchPathKeyMap.remove(dir);
		}
	}

	public interface WatchListener {
		public void watchEventsOccurred();
	}
}
