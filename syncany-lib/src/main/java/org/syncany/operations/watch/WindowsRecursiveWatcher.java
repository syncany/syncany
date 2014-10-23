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
import static name.pachler.nio.file.ext.ExtendedWatchEventModifier.FILE_TREE;

import java.nio.file.Path;
import java.util.List;

import name.pachler.nio.file.FileSystems;
import name.pachler.nio.file.Paths;
import name.pachler.nio.file.WatchEvent.Kind;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.WatchService;

/**
 * @author Philipp
 *
 */
public class WindowsRecursiveWatcher extends RecursiveWatcher {	
	private WatchService watchService;
	private WatchKey rootWatchKey;

	public WindowsRecursiveWatcher(Path root, List<Path> ignorePaths, int settleDelay, WatchListener listener) {
		super(root, ignorePaths, settleDelay, listener);	
		
		this.watchService = null;
		this.rootWatchKey = null;
	}
	
	@Override
	public void beforeStart() throws Exception {
		name.pachler.nio.file.Path translatedRootDir = Paths.get(root.toString());

		watchService = FileSystems.getDefault().newWatchService();
		rootWatchKey = translatedRootDir.register(watchService,
				new Kind[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW }, FILE_TREE);
	}
	
	@Override
	protected void beforePollEventLoop() {
		// Nothing must happen before the event loop.		
	}

	@Override
	public void pollEvents() throws Exception {
		WatchKey watchKey = watchService.take();
		watchKey.pollEvents(); // Take events, but don't care what they are!

		watchKey.reset();
	}

	@Override
	protected void watchEventsOccurred() {
		// This watcher monitors recursively. No need to add more watches.
	}

	@Override
	public synchronized void afterStop() throws Exception {
		rootWatchKey.cancel();
		watchService.close();				
	}
}
