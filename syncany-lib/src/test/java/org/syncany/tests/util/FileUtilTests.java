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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.util.EnvUtil;
import org.syncany.util.EnvUtil.OperatingSystem;
import org.syncany.util.FileUtil;
import org.syncany.util.FileUtil.NormalizedPath;

public class FileUtilTests {
	private OperatingSystem originalOperatingSystem;
	
	@Before
	public void storeOperatingSystem() {
		originalOperatingSystem = EnvUtil.getOperatingSystem();
	}
	
	@After
	public void resetOperatingSystem() {
		// Important: Restore the actual operating systems, 
		//            or other tests might fail.
		
		EnvUtil.setOperatingSystem(originalOperatingSystem);
	}
	
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
	
	@Test
	public void testBackslashPaths() {
		EnvUtil.setOperatingSystem(OperatingSystem.WINDOWS);
		String someWindowsFile = "C:\\Users\\Philipp\\März.jpg";
		
		assertEquals("C:/Users/Philipp/März.jpg", FileUtil.toDatabasePath(someWindowsFile));
		assertEquals("C:/Users/Philipp", FileUtil.getDatabaseParentDirectory(someWindowsFile));
		assertEquals("März.jpg", FileUtil.getDatabaseBasename(someWindowsFile));
		
		EnvUtil.setOperatingSystem(OperatingSystem.UNIX_LIKE);
		String someLinuxFile = "/home/philipp/A \"black\\white\" ☎ telephone.jpg";
		assertEquals("/home/philipp", FileUtil.getDatabaseParentDirectory(someLinuxFile));
		assertEquals("A \"black\\white\" ☎ telephone.jpg", FileUtil.getDatabaseBasename(someLinuxFile));		
	}
	
	@Test
	public void testNameAndParentPathForNormalizedPathsOnWindows() {		
		testNameAndParentPathForNormalizedPaths(OperatingSystem.WINDOWS);
	}
	
	@Test
	public void testNameAndParentPathForNormalizedPathsOnUnixLikeSystems() {		
		testNameAndParentPathForNormalizedPaths(OperatingSystem.UNIX_LIKE);
	}

	private void testNameAndParentPathForNormalizedPaths(OperatingSystem operatingSystem) {		
		EnvUtil.setOperatingSystem(operatingSystem);
		
		// Test 1: For a file called 'A black\white telephone ☎.jpg' 
		//         Note: "A black" is NOT a directory, it's part of the filename (invalid on Windows!)		
		String alreadyNormalizedRelativePathFileStr = "Pictures/A black\\white telephone ☎.jpg";
		NormalizedPath normalizedPathFile = new NormalizedPath(alreadyNormalizedRelativePathFileStr);
		
		assertEquals("Pictures/A black\\white telephone ☎.jpg", normalizedPathFile.toString());
		assertEquals("A black\\white telephone ☎.jpg", normalizedPathFile.getName());
		assertEquals("Pictures", normalizedPathFile.getParent());

		// Test 2: For directory called 'black\\white telephones ☎' 		
		String alreadyNormalizedRelativePathDirStr = "Pictures/black\\white telephones ☎";
		NormalizedPath normalizedPathDir = new NormalizedPath(alreadyNormalizedRelativePathDirStr);
		
		assertEquals("Pictures/black\\white telephones ☎", normalizedPathDir.toString());
		assertEquals("black\\white telephones ☎", normalizedPathDir.getName());
		assertEquals("Pictures", normalizedPathDir.getParent());
		
		// Test 3: For directory called 'black\\white telephones ☎' 		
		String alreadyNormalizedRelativePathFileWithBackslashesDirStr = "Pictures/Black\\White Pictures/Mostly\\Black Pictures/blacky.jpg";
		NormalizedPath normalizedPathWithBackslashesDir = new NormalizedPath(alreadyNormalizedRelativePathFileWithBackslashesDirStr);
		
		assertEquals("Pictures/Black\\White Pictures/Mostly\\Black Pictures/blacky.jpg", normalizedPathWithBackslashesDir.toString());
		assertEquals("blacky.jpg", normalizedPathWithBackslashesDir.getName());
		assertEquals("Pictures/Black\\White Pictures/Mostly\\Black Pictures", normalizedPathWithBackslashesDir.getParent());		
	}
	
	@Test
	public void testNameAndParentPathForNormalizedPathsMoreTests() {		
		// Does not depend on OS
		
		assertEquals("Philipp", new NormalizedPath("Philipp").getName());
		assertEquals("", new NormalizedPath("Philipp").getParent()); 
	}
	
	@Test
	public void testNormalizationOnWindows() {		
		EnvUtil.setOperatingSystem(OperatingSystem.WINDOWS);
		
		assertEquals("C:/Philipp", NormalizedPath.get("C:\\Philipp\\").toString());
		assertEquals("C:/Philipp", NormalizedPath.get("C:\\Philipp").toString());
		assertEquals("C:/Philipp/image.jpg", NormalizedPath.get("C:\\Philipp\\image.jpg").toString());
		assertEquals("C:/Philipp/image", NormalizedPath.get("C:\\Philipp\\image").toString());
		assertEquals("C:/Philipp/file:with:colons.txt", NormalizedPath.get("C:\\Philipp\\file:with:colons.txt").toString()); // Cannot happen on Windows 
		assertEquals("C:/Philipp/file/with/backslashes.txt", NormalizedPath.get("C:\\Philipp\\file\\with\\backslashes.txt").toString()); 
		assertEquals("C:/Philipp/folder/with/backslashes", NormalizedPath.get("C:\\Philipp\\folder\\with\\backslashes\\").toString()); 
	}
	
	@Test
	public void testNormalizationOnUnixLikeSystems() {
		EnvUtil.setOperatingSystem(OperatingSystem.UNIX_LIKE);

		assertEquals("/home/philipp", NormalizedPath.get("/home/philipp/").toString());
		assertEquals("/home/philipp", NormalizedPath.get("/home/philipp").toString());
		assertEquals("/home/philipp/image.jpg", NormalizedPath.get("/home/philipp/image.jpg").toString());
		assertEquals("/home/philipp/image", NormalizedPath.get("/home/philipp/image").toString());
		assertEquals("/home/philipp/file:with:colons", NormalizedPath.get("/home/philipp/file:with:colons").toString());
		assertEquals("/home/philipp/file\\with\\backslashes.txt", NormalizedPath.get("/home/philipp/file\\with\\backslashes.txt").toString());
		assertEquals("/home/philipp/folder\\with\\backslashes", NormalizedPath.get("/home/philipp/folder\\with\\backslashes/").toString());		
	}	
}
