/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.operations.up;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.syncany.chunk.Deduper;
import org.syncany.config.Config;
import org.syncany.database.DatabaseVersion;

/**
 * @author Jesse Donkervliet
 *
 */
public class DatabaseVersionIterator implements Iterator<DatabaseVersion> {

	private AsyncIndexer asyncIndexer;
	private Queue<DatabaseVersion> queue;

	public DatabaseVersionIterator(final Config config, Deduper deduper, List<File> files) {
		queue = new LinkedList<>();
		asyncIndexer = new AsyncIndexer(config, deduper, files, queue);
		new Thread(asyncIndexer).start();
	}

	@Override
	public boolean hasNext() {
		while (!asyncIndexer.isDone() && queue.size() == 0) {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				// We don't care.
			}
		}
		return queue.size() > 0;
	}

	@Override
	public DatabaseVersion next() {
		return queue.poll();
	}

}
