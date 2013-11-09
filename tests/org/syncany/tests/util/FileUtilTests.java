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
package org.syncany.tests.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.Test;
import org.syncany.util.FileUtil;

public class FileUtilTests {
	@Test
	public void testGetRelativeFilePath() {
		String expectedResult = "somefile";
		
		File[] rootFolders = new File[] {
			new File("/home/user/Syncany"),
			new File("/home/user/Syncany/"),
			new File("/home/user/Syncany//"),
			new File("/home/user//Syncany"),
			new File("/home/user//Syncany/"),
			new File("/home/user//Syncany//")			
		};
		
		File[] files = new File[] {
			new File("/home/user/Syncany/somefile"),
			new File("/home/user/Syncany/somefile/"),
			new File("/home/user/Syncany/somefile//"),
			new File("/home/user/Syncany//somefile"),
			new File("/home/user/Syncany//somefile/"),
			new File("/home/user/Syncany//somefile//")
		};
		
		for (File rootFolder : rootFolders) {
			for (File file : files) {
				String actualResult = FileUtil.getRelativePath(rootFolder, file);
				assertEquals("Expected '"+expectedResult+"' for root folder '"+rootFolder+"' and file '"+file+"'", expectedResult, actualResult);
			}
		}
	}
	
	@Test
	public void testGetRelativeFilePathSpecialCases() {
		assertEquals("", FileUtil.getRelativePath(new File("/home/user/"), new File("/home/user")));
		assertEquals("", FileUtil.getRelativePath(new File("/home/user/"), new File("/home/user/")));
		assertEquals("", FileUtil.getRelativePath(new File("/home/user/"), new File("/home/user//")));		
	}
	
	@Test
	public void testFileLocked() throws Exception {
		// Setup
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		
		// Run
		File lockedFile = TestFileUtil.createRandomFileInDirectory(tempDir, 50*1024);
		
		// Test
		assertFalse("File should not be locked: "+lockedFile, FileUtil.isFileLocked(lockedFile));
		 
		RandomAccessFile fileLock = new RandomAccessFile(lockedFile, "rw");		
		FileLock lockedFileLock = fileLock.getChannel().lock();
		
		assertTrue("File should be locked: "+lockedFile, FileUtil.isFileLocked(lockedFile));
		 
		// Tear down
		lockedFileLock.release();
		fileLock.close();
		Path bFilePath = Paths.get(lockedFile.getAbsolutePath());

		if (FileUtil.isWindows()) {
			Files.setAttribute(bFilePath, "dos:readonly", true);
		}
		else if (FileUtil.isUnixLikeOperatingSystem()) {
			Files.setPosixFilePermissions(bFilePath, PosixFilePermissions.fromString("r--r--r--"));
		}	
		
		assertFalse("File should not be locked if read-only: "+lockedFile, FileUtil.isFileLocked(lockedFile));
		
		// Tear down
		TestFileUtil.deleteDirectory(tempDir);
	}
	
}
