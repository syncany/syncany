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
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
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
	
	public static RecursiveWatcher createRecursiveWatcher(Path root, List<Path> ignorePaths, int settleDelay, WatchListener listener) {
		if (EnvironmentUtil.isWindows()) {
			return new WindowsRecursiveWatcher(root, ignorePaths, settleDelay, listener);
		}
		else {
			return new DefaultRecursiveWatcher(root, ignorePaths, settleDelay, listener);
		}
	}
	
	public void start() throws Exception {
		beforeStart();

		watchThread = new Thread(new Runnable() {
			@Override
			public void run() {
				running.set(true);
				beforePollEventLoop();
				
				while (running.get()) {
					try {
						pollEvents();
						resetWaitSettlementTimer();
					}
					catch (Exception e) {
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
				running.set(false);
				watchThread.interrupt();
				
				afterStop();
			}
			catch (Exception e) {
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
	
	protected abstract void beforeStart() throws Exception;
	protected abstract void beforePollEventLoop();
	protected abstract void pollEvents() throws Exception;	
	protected abstract void watchEventsOccurred();
	protected abstract void afterStop() throws Exception;	

	public interface WatchListener {
		public void watchEventsOccurred();
	}
}
