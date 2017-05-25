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

import static org.junit.Assert.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.syncany.operations.watch.RecursiveWatcher.WatchListener;
import org.syncany.tests.util.TestFileUtil;

public class RecursiveWatcherTest {
	@Test
	public void testGetFolder() throws Exception {
		final AtomicInteger eventCount = new AtomicInteger(0);
		final Path tempDir = TestFileUtil.createTempDirectoryInSystemTemp("RecursiveWatcherTest").toPath();
		
		RecursiveWatcher watcher = RecursiveWatcher.createRecursiveWatcher(tempDir, new ArrayList<Path>(), 300, new WatchListener() {			
			@Override
			public void watchEventsOccurred() {
				eventCount.incrementAndGet();
			}
		});
		
		watcher.start();
		Thread.sleep(100);
		
		TestFileUtil.createRandomFileInDirectory(tempDir.toFile(), 111);		
		Thread.sleep(350);
		
		TestFileUtil.createRandomFileInDirectory(tempDir.toFile(), 222);		
		Thread.sleep(350);
		
		TestFileUtil.createRandomFileInDirectory(tempDir.toFile(), 333);		
		Thread.sleep(350);
		
		assertEquals(3, eventCount.get());
		
		TestFileUtil.deleteDirectory(tempDir.toFile());
	}

}
