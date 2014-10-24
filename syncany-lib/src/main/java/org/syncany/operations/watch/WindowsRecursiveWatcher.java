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
import java.nio.file.Paths;
import java.util.List;

import name.pachler.nio.file.FileSystems;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchEvent.Kind;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.WatchService;

/**
 * The Windows recursive file watcher monitors a folder (and its sub-folders)
 * by registering a <em>recursive</em> watch on the root folder. This class is used
 * only on Windows and uses jpathwatch-based {@link WatchService}.
 * 
 * <p>Via the jpathwatch library, the class uses the Windows-native recursive folder-
 * watching capabilities of Windows and thereby does not need to register watches on
 * all sub-folders. Instead, only one watch on the root folder is needed.
 * 
 * <p>When a file event occurs, a timer is started to wait for the file operations
 * to settle. It is reset whenever a new event occurs. When the timer times out,
 * an event is thrown through the {@link WatchListener}.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
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
		name.pachler.nio.file.Path extLibRootDir = name.pachler.nio.file.Paths.get(root.toString());

		watchService = FileSystems.getDefault().newWatchService();
		rootWatchKey = extLibRootDir.register(watchService,
				new Kind[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW }, FILE_TREE);
	}

	@Override
	protected void beforePollEventLoop() {
		// Nothing must happen before the event loop.		
	}

	@Override
	public boolean pollEvents() throws Exception {
		WatchKey watchKey = watchService.take();

		List<WatchEvent<?>> watchEvents = watchKey.pollEvents(); 
		boolean hasRelevantEvents = false;
		
		// Filter ignored events
		for (WatchEvent<?> watchEvent : watchEvents) {
			if (watchEvent.kind() == ENTRY_CREATE || watchEvent.kind() == ENTRY_MODIFY || watchEvent.kind() == ENTRY_DELETE) {				
				boolean ignoreEvent = false;		

				name.pachler.nio.file.Path extLibFilePath = (name.pachler.nio.file.Path) watchEvent.context();
				Path filePath = Paths.get(extLibFilePath.toString()).toAbsolutePath().normalize();
				
				for (Path ignorePath : ignorePaths) {
					if (filePath.startsWith(ignorePath.toAbsolutePath().normalize())) {
						ignoreEvent = true;
						break;
					}
				}
				
				if (!ignoreEvent) {
					hasRelevantEvents = true;
					break;
				}
			}
		}

		watchKey.reset();
		return hasRelevantEvents;
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
