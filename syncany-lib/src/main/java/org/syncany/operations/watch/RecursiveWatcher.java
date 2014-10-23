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
import java.util.logging.Logger;

import org.syncany.operations.watch.DefaultRecursiveWatcher.WatchListener;
import org.syncany.util.EnvironmentUtil;

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
public abstract class RecursiveWatcher {
	protected static final Logger logger = Logger.getLogger(DefaultRecursiveWatcher.class.getSimpleName());

	protected Path root;
	protected List<Path> ignorePaths;
	protected int settleDelay;
	protected WatchListener listener;
	
	public RecursiveWatcher(Path root, List<Path> ignorePaths, int settleDelay, WatchListener listener) {
		this.root = root;
		this.ignorePaths = ignorePaths;
		this.settleDelay = settleDelay;
		this.listener = listener;		
	}
	
	public static RecursiveWatcher createRecursiveWatcher(Path root, List<Path> ignorePaths, int settleDelay, WatchListener listener) {
		if (EnvironmentUtil.isWindows()) {
			return new WindowsRecursiveWatcher(root, ignorePaths, settleDelay, listener);
		}
		else {
			return new DefaultRecursiveWatcher(root, ignorePaths, settleDelay, listener);
		}
	}
}
