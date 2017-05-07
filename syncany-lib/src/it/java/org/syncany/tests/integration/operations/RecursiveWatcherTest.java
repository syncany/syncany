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
package org.syncany.tests.integration.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.syncany.config.Logging;
import org.syncany.operations.watch.DefaultRecursiveWatcher;
import org.syncany.operations.watch.RecursiveWatcher.WatchListener;
import org.syncany.tests.util.TestFileUtil;

public class RecursiveWatcherTest {	
	static {
		Logging.init();
	}
	
	@Test
	public void testWatchOneFile() throws Exception {
		// Setup
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		TestFileUtil.createRandomFilesInDirectory(tempDir, 50*1024, 20);		
		
		// Test
		final AtomicBoolean eventOccurred = new AtomicBoolean(false);
		
		DefaultRecursiveWatcher watcher = new DefaultRecursiveWatcher(Paths.get(tempDir.getAbsolutePath()), new ArrayList<Path>(), 300, new WatchListener() {
			@Override
			public void watchEventsOccurred() {
				eventOccurred.set(true);
			}			
		});
		
		// Start watcher and wait for watch service to set watch on all folders
		watcher.start();		
		Thread.sleep(100);
		
		// Event 1 
		TestFileUtil.createRandomFileInDirectory(tempDir, 10);
		assertFalse(eventOccurred.get());

		Thread.sleep(550);
		assertTrue(eventOccurred.get());

		// Stop watcher (close watches)
		watcher.stop();
		
		// Tear down
		TestFileUtil.deleteDirectory(tempDir);
	}
	
	@Test
	public void testWatchExistingSubfolderEvent() throws Exception {
		// Setup
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();		
		File subDir1 = new File(tempDir+"/subdir1");		
		
		// Test
		final AtomicBoolean eventOccurred = new AtomicBoolean(false);
		
		DefaultRecursiveWatcher watcher = new DefaultRecursiveWatcher(Paths.get(tempDir.getAbsolutePath()), new ArrayList<Path>(), 300, new WatchListener() {
			@Override
			public void watchEventsOccurred() {
				eventOccurred.set(true);
			}			
		});
		
		// Create sub folder BEFORE watcher starts
		subDir1.mkdir();
		
		// Start watcher and wait for watch service to set watch on all folders
		watcher.start();		
		Thread.sleep(100);
		
		// Event 1 
		TestFileUtil.createRandomFileInDirectory(subDir1, 10);
		assertFalse(eventOccurred.get());

		Thread.sleep(350);
		assertTrue(eventOccurred.get());

		// Stop watcher (close watches)
		watcher.stop();
		
		// Tear down
		TestFileUtil.deleteDirectory(tempDir);
	}
	
	@Test
	public void testWatchNewSubfolderEvent() throws Exception {
		// Setup
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();		
		File subDir1 = new File(tempDir+"/subdir1");		
		
		// Test
		final AtomicBoolean eventOccurred = new AtomicBoolean(false);
		
		DefaultRecursiveWatcher watcher = new DefaultRecursiveWatcher(Paths.get(tempDir.getAbsolutePath()), new ArrayList<Path>(), 300, new WatchListener() {
			@Override
			public void watchEventsOccurred() {
				eventOccurred.set(true);
			}			
		});
		
		// Start watcher and wait for watch service to set watch on all folders
		watcher.start();		
		Thread.sleep(150);
		
		// Create sub folder AFTER watcher starts
		subDir1.mkdir();
		Thread.sleep(370);
		assertTrue(eventOccurred.get());
		
		eventOccurred.set(false);
		
		// Event on NEW subfolder
		TestFileUtil.createRandomFileInDirectory(subDir1, 10);
		assertFalse(eventOccurred.get());

		Thread.sleep(350);
		assertTrue(eventOccurred.get());

		// Stop watcher (close watches)
		watcher.stop();
		
		// Tear down
		TestFileUtil.deleteDirectory(tempDir);
	}
	
	@Test
	public void testWatch100LevelsOfNewSubfolders() throws Exception {
		// Setup
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();		
		
		// Test
		final AtomicBoolean eventOccurred = new AtomicBoolean(false);
		
		DefaultRecursiveWatcher watcher = new DefaultRecursiveWatcher(Paths.get(tempDir.getAbsolutePath()), new ArrayList<Path>(), 300, new WatchListener() {
			@Override
			public void watchEventsOccurred() {
				eventOccurred.set(true);
			}			
		});
		
		// Start watcher and wait for watch service to set watch on all folders
		watcher.start();		
		Thread.sleep(100);

		// Create sub folder hierarchy
		File lastSubFolder = tempDir;
		
		for (int i=0; i<100; i++) {
			lastSubFolder.mkdir();
			lastSubFolder = new File(lastSubFolder+"/"+i);
		}
		
		lastSubFolder.mkdir();
		
		// Wait for settlement
		Thread.sleep(350);
		eventOccurred.set(false);
		
		// Event on DEEPEST new subfolder
		TestFileUtil.createRandomFileInDirectory(lastSubFolder, 10*1024);
		assertFalse(eventOccurred.get());
		
		Thread.sleep(500);
		assertTrue(eventOccurred.get());
		
		// Stop watcher (close watches)
		watcher.stop();
		
		// Tear down	
		TestFileUtil.deleteDirectory(tempDir);
	}
	
	@Test
	public void testWatchFireOnceWhenLargeFileFinishes() throws Exception {
		// Setup
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();		
		
		// Test
		final AtomicLong watcherEventOccurredTime = new AtomicLong(0L);
		final AtomicInteger watcherEventOccurredCount = new AtomicInteger(0);
		final AtomicLong fileWrittenTime = new AtomicLong(0L);
		
		DefaultRecursiveWatcher watcher = new DefaultRecursiveWatcher(Paths.get(tempDir.getAbsolutePath()), new ArrayList<Path>(), 300, new WatchListener() {
			@Override
			public void watchEventsOccurred() { 
				watcherEventOccurredCount.addAndGet(1);
				watcherEventOccurredTime.set(System.currentTimeMillis());
			}			
		});
		
		// Start watcher 
		watcher.start();		
		Thread.sleep(400); // Wait for watch service to set watch on all folders
		
		File largeFile = new File(tempDir+"/largefile");
				
		TestFileUtil.createRandomFile(largeFile, 50*1024*1024);
		fileWrittenTime.set(System.currentTimeMillis());
		
		// Stop watcher (close watches)
		Thread.sleep(800); // Wait for the watcher to fire an event
		watcher.stop();
		
		// Test
		assertNotSame(0L, watcherEventOccurredTime.get());
		assertEquals(1, watcherEventOccurredCount.get());
		assertNotSame(0L, fileWrittenTime.get());
		assertTrue(watcherEventOccurredTime.get() >= fileWrittenTime.get());
		
		// Tear down	
		TestFileUtil.deleteDirectory(tempDir);
	}
	
	@Test
	public void testWatchFireEventCount() throws Exception {
		// Setup
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();		
		
		// Test
		final AtomicInteger watcherEventOccurredCount = new AtomicInteger(0);
		
		DefaultRecursiveWatcher watcher = new DefaultRecursiveWatcher(Paths.get(tempDir.getAbsolutePath()), new ArrayList<Path>(), 300, new WatchListener() {
			@Override
			public void watchEventsOccurred() { 
				watcherEventOccurredCount.addAndGet(1);
			}			
		});
		
		// Start watcher 
		watcher.start();		
		Thread.sleep(200); // Wait for watch service to set watch on all folders
		
		for (int i=0; i<20; i++) {
			TestFileUtil.createRandomFile(new File(tempDir+"/"+i), 50*1024);
		}				

		// Stop watcher (close watches)
		Thread.sleep(400); // Wait for the watcher to fire an event
		watcher.stop();
		
		// Test
		assertEquals(1, watcherEventOccurredCount.get()); // not 20!
		
		// Tear down	
		TestFileUtil.deleteDirectory(tempDir);
	}	
}
