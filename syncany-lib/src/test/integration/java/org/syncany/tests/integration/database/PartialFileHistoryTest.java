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
package org.syncany.tests.integration.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;

public class PartialFileHistoryTest {
	@Test
	public void testFileHistoryInitEmpty() {
		PartialFileHistory fileHistory = new PartialFileHistory(FileHistoryId.parseFileId("1234"));
		
		assertEquals(FileHistoryId.parseFileId("1234"), fileHistory.getFileHistoryId());
		assertNotSame(FileHistoryId.parseFileId("9999"), fileHistory.getFileHistoryId());
		assertNull(fileHistory.getLastVersion());
		assertNotNull(fileHistory.toString());
		assertTrue(fileHistory.toString().contains("1234"));
		assertNotNull(fileHistory.getFileVersions());
		assertEquals(0, fileHistory.getFileVersions().size());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testFileHistoryInitInvalid() {
		new PartialFileHistory(null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testFileHistoryAddFileVersionInvalidNull() {
		PartialFileHistory fileHistory = new PartialFileHistory(FileHistoryId.parseFileId("1234"));
		fileHistory.addFileVersion(null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testFileHistoryAddFileVersionInvalidVersionNull() {
		PartialFileHistory fileHistory = new PartialFileHistory(FileHistoryId.parseFileId("1234"));
		fileHistory.addFileVersion(new FileVersion());
	}
	
	@Test
	public void testFileHistoryGetVersionsNonEmpty() {
		FileVersion fileVersion = new FileVersion();
		fileVersion.setVersion(5L);
		fileVersion.setPath("/somepath");
		
		PartialFileHistory fileHistory = new PartialFileHistory(FileHistoryId.parseFileId("1234"));
		fileHistory.addFileVersion(fileVersion);
		
		assertNotNull(fileHistory.getLastVersion());
		assertNotNull(fileHistory.getFileVersions());
		assertEquals(1, fileHistory.getFileVersions().size());
		assertEquals(fileVersion, fileHistory.getLastVersion());
		assertNull(fileHistory.getFileVersions().get(1L));
		assertNull(fileHistory.getFileVersion(1L));
		assertEquals(fileVersion, fileHistory.getFileVersions().get(5L));
		assertEquals(fileVersion, fileHistory.getFileVersion(5L));
	}	
	
	@Test
	public void testFileHistoryClone() {
		FileVersion fileVersion1 = new FileVersion();
		fileVersion1.setVersion(1L);
		fileVersion1.setPath("/somepath");
		
		FileVersion fileVersion2 = new FileVersion();
		fileVersion2.setVersion(2L);
		fileVersion2.setPath("/somepath");

		PartialFileHistory fileHistory = new PartialFileHistory(FileHistoryId.parseFileId("1234"));
		fileHistory.addFileVersion(fileVersion1);
		fileHistory.addFileVersion(fileVersion2);
		
		PartialFileHistory fileHistoryClone = fileHistory.clone();
		
		assertEquals(fileHistory, fileHistoryClone);
		assertEquals(fileHistory.getFileVersions().size(), fileHistoryClone.getFileVersions().size());
		assertEquals(fileHistory.getFileVersions(), fileHistoryClone.getFileVersions());

		FileVersion fileVersion3 = new FileVersion();
		fileVersion3.setVersion(3L);
		fileVersion3.setPath("/somepath");
		
		fileHistoryClone.addFileVersion(fileVersion3);
		assertEquals(fileHistory.getFileVersions().size()+1, fileHistoryClone.getFileVersions().size());
	}	
	
	@Test
	public void testFileHistoryIdParsedOk() {
		assertEquals("12", FileHistoryId.parseFileId("12").toString());
	}
	
	@Test
	public void testFileHistoryIdRandom() {
		FileHistoryId secureRandomFileId1 = FileHistoryId.secureRandomFileId();
		FileHistoryId secureRandomFileId2 = FileHistoryId.secureRandomFileId();
		
		assertNotNull(secureRandomFileId1);
		assertEquals(20*2, secureRandomFileId1.toString().length());
		
		assertNotSame(secureRandomFileId1, secureRandomFileId2);
		assertNotNull(secureRandomFileId2);
		assertEquals(20*2, secureRandomFileId2.toString().length());		
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testFileHistoryIdInvalidNull() {
		FileHistoryId.parseFileId(null);
	}
}
