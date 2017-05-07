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
package org.syncany.tests.integration.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Date;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.FileVersionComparator;
import org.syncany.database.FileVersionComparator.FileChange;
import org.syncany.database.FileVersionComparator.FileProperties;
import org.syncany.database.FileVersionComparator.FileVersionComparison;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.util.CollectionUtil;
import org.syncany.util.EnvironmentUtil;
import org.syncany.util.FileUtil;

public class FileVersionComparatorTest {
	@Test
	public void testCaptureFilePropertiesFromFile() throws Exception {
		// Setup
		Config config = TestConfigUtil.createTestLocalConfig();
		FileVersionComparator versionComparator = new FileVersionComparator(config.getLocalDir(), config.getChunker().getChecksumAlgorithm());

		// Create file
		File somefile = new File(config.getLocalDir()+"/somefile");
		Path somefilePath = Paths.get(somefile.getAbsolutePath());
		
		TestFileUtil.createRandomFile(somefile, 100*1024);		
		somefile.setLastModified(1382196000);		
		
		if (EnvironmentUtil.isWindows()) {
			Files.setAttribute(somefilePath, "dos:archive", false);
			Files.setAttribute(somefilePath, "dos:hidden", false);
			Files.setAttribute(somefilePath, "dos:readonly", false);
			Files.setAttribute(somefilePath, "dos:system", false);
		}
		else if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			Files.setPosixFilePermissions(somefilePath, PosixFilePermissions.fromString("rw-rw-r-x"));
		}
		
		// Run
		FileProperties fileProperties = versionComparator.captureFileProperties(somefile, null, true);
		
		// Test
		assertNotNull(fileProperties.getChecksum());
		assertEquals(1382196000, fileProperties.getLastModified());
		assertEquals("somefile", fileProperties.getRelativePath());
		assertEquals(100*1024, fileProperties.getSize());
		assertNull(fileProperties.getLinkTarget());
		assertTrue(fileProperties.exists());
		assertEquals(FileType.FILE, fileProperties.getType());

		if (EnvironmentUtil.isWindows()) {
			DosFileAttributes dosFileAttributes = FileUtil.dosAttrsFromString(fileProperties.getDosAttributes());
			
			assertFalse(dosFileAttributes.isArchive());
			assertFalse(dosFileAttributes.isHidden());
			assertFalse(dosFileAttributes.isReadOnly());
			assertFalse(dosFileAttributes.isSystem());
			
			assertNull(fileProperties.getPosixPermissions());
		}
		else if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			assertEquals("rw-rw-r-x", fileProperties.getPosixPermissions());
			assertNull(fileProperties.getDosAttributes());
		}	
		
		// Tear down
		TestConfigUtil.deleteTestLocalConfigAndData(config);
	}
	
	@Test
	public void testCaptureFilePropertiesFromFileVersion() throws Exception {
		// Setup
		Config config = TestConfigUtil.createTestLocalConfig();
		FileVersionComparator versionComparator = new FileVersionComparator(config.getLocalDir(), config.getChunker().getChecksumAlgorithm());
		
		FileVersion fileVersion = new FileVersion();
		fileVersion.setVersion(3L);

		fileVersion.setChecksum(new FileChecksum(new byte[] { 0x11, 0x22, 0x33 }));
		fileVersion.setLastModified(new Date(123456789));
		fileVersion.setPath("folder/file");
		fileVersion.setSize(999*1024L);
		fileVersion.setLinkTarget(null);
		fileVersion.setStatus(FileStatus.CHANGED);
		fileVersion.setType(FileType.FILE);
		
		if (EnvironmentUtil.isWindows()) {
			fileVersion.setDosAttributes("rha-");
		}
		else if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			fileVersion.setPosixPermissions("rw-rw-r-x");
		}
		
		// Run
		FileProperties fileProperties = versionComparator.captureFileProperties(fileVersion);
		
		// Test
		assertEquals(new FileChecksum(new byte[] { 0x11, 0x22, 0x33 }), fileProperties.getChecksum());
		assertEquals(123456789, fileProperties.getLastModified());
		assertEquals("folder/file", fileProperties.getRelativePath());
		assertEquals(999*1024, fileProperties.getSize());
		assertNull(fileProperties.getLinkTarget());
		assertTrue(fileProperties.exists());
		assertEquals(FileType.FILE, fileProperties.getType());

		if (EnvironmentUtil.isWindows()) {
			DosFileAttributes dosFileAttributes = FileUtil.dosAttrsFromString(fileProperties.getDosAttributes());
			
			assertTrue(dosFileAttributes.isArchive());
			assertTrue(dosFileAttributes.isHidden());
			assertTrue(dosFileAttributes.isReadOnly());
			assertFalse(dosFileAttributes.isSystem());
			
			assertNull(fileProperties.getPosixPermissions());
		}
		else if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			assertEquals("rw-rw-r-x", fileProperties.getPosixPermissions());
			assertNull(fileProperties.getDosAttributes());
		}	
		
		// Tear down
		TestConfigUtil.deleteTestLocalConfigAndData(config);
	}
	

	@Test
	public void testCompareFileVersionToFile() throws Exception {
		// Setup
		Config config = TestConfigUtil.createTestLocalConfig();
		FileVersionComparator versionComparator = new FileVersionComparator(config.getLocalDir(), config.getChunker().getChecksumAlgorithm());
		
		// Create File
		File somefile = new File(config.getLocalDir()+"/file1");
		Path somefilePath = Paths.get(somefile.getAbsolutePath());
		
		TestFileUtil.createRandomFile(somefile, 130*1024);		
		somefile.setLastModified(1182196000);		
		
		if (EnvironmentUtil.isWindows()) {
			Files.setAttribute(somefilePath, "dos:readonly", true);
			Files.setAttribute(somefilePath, "dos:hidden", false);
			Files.setAttribute(somefilePath, "dos:archive", true);
			Files.setAttribute(somefilePath, "dos:system", false);
		}
		else if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			Files.setPosixFilePermissions(somefilePath, PosixFilePermissions.fromString("r--rwxrw-"));
		}
		
		// Create FileVersion
		FileVersion fileVersion = new FileVersion();
		fileVersion.setVersion(100L);

		fileVersion.setChecksum(new FileChecksum(new byte[] { 0x11, 0x22, 0x33 })); // << definitely differs
		fileVersion.setLastModified(new Date(1182196000));
		fileVersion.setPath("file1");
		fileVersion.setSize(130*1024L);
		fileVersion.setLinkTarget(null);
		fileVersion.setStatus(FileStatus.NEW);
		fileVersion.setType(FileType.FILE);
		
		if (EnvironmentUtil.isWindows()) {
			fileVersion.setDosAttributes("r-a-");
		}
		else if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			fileVersion.setPosixPermissions("r--rwxrw-");
		}
		
		// Run
		FileVersionComparison fileComparison = versionComparator.compare(fileVersion, somefile, null, true); 
		
		// Test
		assertFalse(fileComparison.areEqual());
		assertTrue(CollectionUtil.containsExactly(fileComparison.getFileChanges(), FileChange.CHANGED_CHECKSUM));
		
		// Tear down
		TestConfigUtil.deleteTestLocalConfigAndData(config);
	}
	
}
