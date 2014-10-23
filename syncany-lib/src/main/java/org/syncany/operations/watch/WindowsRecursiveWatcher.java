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

import static name.pachler.nio.file.StandardWatchEventKind.ENTRY_CREATE;
import static name.pachler.nio.file.StandardWatchEventKind.ENTRY_DELETE;
import static name.pachler.nio.file.StandardWatchEventKind.ENTRY_MODIFY;
import static name.pachler.nio.file.StandardWatchEventKind.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import name.pachler.nio.file.ClosedWatchServiceException;
import name.pachler.nio.file.FileSystems;
import name.pachler.nio.file.Paths;
import name.pachler.nio.file.StandardWatchEventKind;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchEvent.Kind;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.WatchService;
import name.pachler.nio.file.ext.ExtendedWatchEventKind;
import name.pachler.nio.file.impl.PathWatchEventKind;
import static name.pachler.nio.file.ext.ExtendedWatchEventModifier.FILE_TREE;

import org.syncany.operations.watch.DefaultRecursiveWatcher.WatchListener;

/**
 * @author Philipp
 *
 */
public class WindowsRecursiveWatcher extends RecursiveWatcher {
	private AtomicBoolean running;
	
	private WatchService watchService;
	private Thread watchThread;
	private WatchKey rootWatchKey;

	private Timer timer;

	public WindowsRecursiveWatcher(Path root, List<Path> ignorePaths, int settleDelay, WatchListener listener) {
		super(root, ignorePaths, settleDelay, listener);
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
		}, "Watcher/" + root.toFile().getName());
		
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

		timer = new Timer("FsSettleTim/" + root.toFile().getName());
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				logger.log(Level.INFO, "File system actions (on watched folders) settled. Updating watches ...");
				//walkTreeAndSetWatches();
				//unregisterStaleWatches();

				fireListenerEvents();
			}
		}, settleDelay);
	}

	private synchronized void walkTreeAndSetWatches() {
		logger.log(Level.INFO, "Registering new folders at watch service ...");

		registerWatch(root);
	}

	private synchronized void unregisterStaleWatches() {
		// Nothing
	}

	private synchronized void fireListenerEvents() {
		if (listener != null) {
			logger.log(Level.INFO, "- Firing watch event (watchEventsOccurred) ...");
			listener.watchEventsOccurred();
		}
	}

	private synchronized void registerWatch(Path dir) {
		logger.log(Level.INFO, "- Registering " + dir);

		try {
			name.pachler.nio.file.Path translatedDir = Paths.get(dir.toString());

			rootWatchKey = translatedDir.register(watchService,
					new Kind[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW }, FILE_TREE);
		}
		catch (IOException e) {
			// Don't care!
		}
	}

	private synchronized void unregisterWatch(Path dir) {
		logger.log(Level.INFO, "- Cancelling " + dir);
			
		rootWatchKey.cancel();
	}

}
