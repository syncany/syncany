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
package org.syncany.tests.config;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;
import org.syncany.config.Cache;
import org.syncany.tests.util.TestFileUtil;

public class CacheTest {
	@Test
	public void testCacheSizeOkay() throws Exception {
		File testCacheFolder = TestFileUtil.createTempDirectoryInSystemTemp();
		
		Cache cache = new Cache(testCacheFolder);
		cache.setKeepBytes(50*1024);
		
		TestFileUtil.createRandomFilesInDirectory(testCacheFolder, 10*1024, 4);

		assertEquals(4, testCacheFolder.listFiles().length);
		cache.clear();		
		assertEquals(4, testCacheFolder.listFiles().length);
		
		TestFileUtil.deleteDirectory(testCacheFolder);
	}
	
	@Test
	public void testCacheNeedsCleaning5Left() throws Exception {
		File testCacheFolder = TestFileUtil.createTempDirectoryInSystemTemp();
		
		Cache cache = new Cache(testCacheFolder);
		cache.setKeepBytes(50*1024);
		
		TestFileUtil.createRandomFilesInDirectory(testCacheFolder, 10*1024, 10);

		assertEquals(10, testCacheFolder.listFiles().length);
		cache.clear();		
		assertEquals(5, testCacheFolder.listFiles().length);
				
		TestFileUtil.deleteDirectory(testCacheFolder);
	}
	
	@Test
	public void testCacheNeedsCleaning1Left() throws Exception {
		File testCacheFolder = TestFileUtil.createTempDirectoryInSystemTemp();
		
		Cache cache = new Cache(testCacheFolder);
		cache.setKeepBytes(50*1024);
		
		TestFileUtil.createRandomFile(new File(testCacheFolder, "10"), 10*1024);
		Thread.sleep(1001);	// Linux/ext3 only has 1s accuracy
		TestFileUtil.createRandomFile(new File(testCacheFolder, "30"), 30*1024);
		Thread.sleep(1001);		
		TestFileUtil.createRandomFile(new File(testCacheFolder, "20"), 20*1024);
		Thread.sleep(1001);		
		TestFileUtil.createRandomFile(new File(testCacheFolder, "40"), 40*1024);

		assertEquals(4, testCacheFolder.listFiles().length);
		
		cache.clear();	
		
		assertEquals(1, testCacheFolder.listFiles().length);
		assertFalse(new File(testCacheFolder, "10").exists());
		assertFalse(new File(testCacheFolder, "30").exists());
		assertFalse(new File(testCacheFolder, "20").exists());
		assertTrue(new File(testCacheFolder, "40").exists());
		
		TestFileUtil.deleteDirectory(testCacheFolder);
	}
	
	@Test
	public void testCacheNeedsCleaning2Left() throws Exception {
		File testCacheFolder = TestFileUtil.createTempDirectoryInSystemTemp();
		
		Cache cache = new Cache(testCacheFolder);
		cache.setKeepBytes(50*1024);
		
		TestFileUtil.createRandomFile(new File(testCacheFolder, "40"), 40*1024);
		Thread.sleep(1001);	// Linux/ext3 only has 1s accuracy
		TestFileUtil.createRandomFile(new File(testCacheFolder, "30"), 30*1024);
		Thread.sleep(1001);		
		TestFileUtil.createRandomFile(new File(testCacheFolder, "20"), 20*1024);
		Thread.sleep(1001);		
		TestFileUtil.createRandomFile(new File(testCacheFolder, "10"), 10*1024);

		assertEquals(4, testCacheFolder.listFiles().length);
		
		cache.clear();	
		
		assertEquals(2, testCacheFolder.listFiles().length);
		assertFalse(new File(testCacheFolder, "40").exists());
		assertFalse(new File(testCacheFolder, "30").exists());
		assertTrue(new File(testCacheFolder, "20").exists());
		assertTrue(new File(testCacheFolder, "10").exists());
		
		TestFileUtil.deleteDirectory(testCacheFolder);
	}
}
