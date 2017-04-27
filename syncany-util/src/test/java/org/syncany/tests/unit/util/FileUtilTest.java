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
package org.syncany.tests.unit.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.Test;
import org.syncany.util.EnvironmentUtil;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class FileUtilTest {	
	@Test
	public void testGetRelativePath() {
		if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			assertEquals("some/path", FileUtil.getRelativePath(new File("/home"), new File("/home/some/path")));
			assertEquals("some/path", FileUtil.getRelativePath(new File("/home/"), new File("/home/some/path")));
			assertEquals("path", FileUtil.getRelativePath(new File("/home/some"), new File("/home/some/path")));
			assertEquals("path", FileUtil.getRelativePath(new File("/home/some/"), new File("/home/some/path")));
		}
		else {
			assertEquals("some\\path", FileUtil.getRelativePath(new File("C:\\home"), new File("C:\\home\\some\\path")));
			assertEquals("some\\path", FileUtil.getRelativePath(new File("C:\\home"), new File("C:\\home\\some\\path")));
			assertEquals("path", FileUtil.getRelativePath(new File("C:\\home\\some"), new File("C:\\home\\some\\path")));
			assertEquals("path", FileUtil.getRelativePath(new File("C:\\homesome\\"), new File("C:\\home\\some\\path")));
		}
	}
	
	@Test
	public void testFileExistsNormal() throws Exception {
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		
		TestFileUtil.createRandomFile(new File(tempDir, "file1"), 1234);
		
		assertTrue(FileUtil.exists(new File(tempDir, "file1")));
		assertFalse(FileUtil.exists(new File(tempDir, "file2")));
		
		TestFileUtil.deleteDirectory(tempDir);
	}
	
	@Test
	public void testFileExistsSymlink() throws Exception {
		if (!EnvironmentUtil.symlinksSupported()) {
			return;
		}
		
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		
		TestFileUtil.createRandomFile(new File(tempDir, "file1"), 1234);

		Files.createSymbolicLink(new File(tempDir, "link-to-file1").toPath(), new File(tempDir, "file1").toPath());
		Files.createSymbolicLink(new File(tempDir, "non-existing-target").toPath(), Paths.get("/does/not/exist"));

		String linkTargetToFile1 = FileUtil.readSymlinkTarget(new File(tempDir, "link-to-file1"));
		String nonExistingSymlinkTarget = FileUtil.readSymlinkTarget(new File(tempDir, "non-existing-target"));
		
		assertEquals(new File(tempDir, "file1").getAbsolutePath(), linkTargetToFile1);
		assertEquals("/does/not/exist", nonExistingSymlinkTarget);		
		assertTrue(FileUtil.exists(new File(tempDir, "link-to-file1")));
		assertTrue(FileUtil.exists(new File(tempDir, "non-existing-target")));
		assertFalse(FileUtil.exists(new File(tempDir, "actually-non-existing-file-or-link")));
		
		TestFileUtil.deleteDirectory(tempDir);		
	}
	
	@Test
	public void testCreateSymlink() throws Exception {
		if (!EnvironmentUtil.symlinksSupported()) {
			return;
		}
		
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		
		TestFileUtil.createRandomFile(new File(tempDir, "file1"), 1234);

		FileUtil.createSymlink("/some/target", new File(tempDir, "link-to-non-existing-target"));
		FileUtil.createSymlink(new File(tempDir, "file1").getAbsolutePath(), new File(tempDir, "link-to-file1"));;

		String linkTargetToFile1 = FileUtil.readSymlinkTarget(new File(tempDir, "link-to-file1"));
		String nonExistingSymlinkTarget = FileUtil.readSymlinkTarget(new File(tempDir, "link-to-non-existing-target"));
		
		assertEquals(new File(tempDir, "file1").getAbsolutePath(), linkTargetToFile1);
		assertEquals("/some/target", nonExistingSymlinkTarget);		
		
		TestFileUtil.deleteDirectory(tempDir);		
	}

	@Test
	public void testGetCanonicalFile() throws Exception {
		if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			assertEquals(new File("/a"), FileUtil.getCanonicalFile(new File("/tmp/../a")));
		}
		else {
			assertEquals(new File("C:\\a"), FileUtil.getCanonicalFile(new File("C:\\Windows\\..\\a")));
		}
	}
	
	@Test
	public void testCreateChecksum() throws Exception {
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		
		TestFileUtil.createFileWithContent(new File(tempDir, "file"), "000");
		
		byte[] sha1sum = FileUtil.createChecksum(new File(tempDir, "file"), "SHA1");
		byte[] md5sum = FileUtil.createChecksum(new File(tempDir, "file"), "MD5");
		
		assertNotNull(sha1sum);
		assertNotNull(md5sum);
		assertEquals("8aefb06c426e07a0a671a1e2488b4858d694a730", StringUtil.toHex(sha1sum));
		assertEquals("c6f057b86584942e415435ffb1fa93d4", StringUtil.toHex(md5sum));
		
		TestFileUtil.deleteDirectory(tempDir);
	}

	@Test
	public void testIsDirectory() throws Exception {
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		
		new File(tempDir, "folder").mkdir();
		new File(tempDir, "file").createNewFile();
		assertTrue(FileUtil.isDirectory(new File(tempDir, "folder")));
		assertFalse(FileUtil.isDirectory(new File(tempDir, "file")));
		
		if (EnvironmentUtil.symlinksSupported()) {
			FileUtil.createSymlink(new File(tempDir, "folder").getAbsolutePath(), new File(tempDir, "symlink-to-folder"));
			assertFalse(FileUtil.isDirectory(new File(tempDir, "symlink-to-folder")));
		}
		
		TestFileUtil.deleteDirectory(tempDir);
		
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

		if (EnvironmentUtil.isWindows()) {
			Files.setAttribute(bFilePath, "dos:readonly", true);
		}
		else if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			Files.setPosixFilePermissions(bFilePath, PosixFilePermissions.fromString("r--r--r--"));
		}	
		
		assertFalse("File should not be locked if read-only: "+lockedFile, FileUtil.isFileLocked(lockedFile));
		
		// Tear down
		TestFileUtil.deleteDirectory(tempDir);
	}
}
