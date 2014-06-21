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
package org.syncany.tests.util;

import static org.junit.Assert.*;

import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;

import org.junit.Test;
import org.syncany.util.FileUtil;
import org.syncany.util.LimitedDosFileAttributes;

public class LimitedDosFileAttributesTest {		
	@Test
	public void testFileDosAttrs() throws Exception {
		DosFileAttributes dosAttrsRHAS = FileUtil.dosAttrsFromString("rhas");
		assertTrue(dosAttrsRHAS.isReadOnly());
		assertTrue(dosAttrsRHAS.isHidden());
		assertTrue(dosAttrsRHAS.isArchive());
		assertTrue(dosAttrsRHAS.isSystem());
		
		DosFileAttributes dosAttrsRHA = FileUtil.dosAttrsFromString("rha-");
		assertTrue(dosAttrsRHA.isReadOnly());
		assertTrue(dosAttrsRHA.isHidden());
		assertTrue(dosAttrsRHA.isArchive());
		assertFalse(dosAttrsRHA.isSystem());
		
		DosFileAttributes dosAttrsRH = FileUtil.dosAttrsFromString("rh--");
		assertTrue(dosAttrsRH.isReadOnly());
		assertTrue(dosAttrsRH.isHidden());
		assertFalse(dosAttrsRH.isArchive());
		assertFalse(dosAttrsRH.isSystem());
		
		DosFileAttributes dosAttrsR = FileUtil.dosAttrsFromString("r---");
		assertTrue(dosAttrsR.isReadOnly());
		assertFalse(dosAttrsR.isHidden());
		assertFalse(dosAttrsR.isArchive());
		assertFalse(dosAttrsR.isSystem());
		
		DosFileAttributes dosAttrsNone = FileUtil.dosAttrsFromString("----");
		assertFalse(dosAttrsNone.isReadOnly());
		assertFalse(dosAttrsNone.isHidden());
		assertFalse(dosAttrsNone.isArchive());
		assertFalse(dosAttrsNone.isSystem());
		
		DosFileAttributes dosAttrsH = FileUtil.dosAttrsFromString("-h--"); 
		assertFalse(dosAttrsH.isReadOnly());
		assertTrue(dosAttrsH.isHidden());
		assertFalse(dosAttrsH.isArchive());
		assertFalse(dosAttrsH.isSystem());
		
		DosFileAttributes dosAttrsCorrect = FileUtil.dosAttrsFromString("NONO"); 
		assertFalse(dosAttrsCorrect.isReadOnly());
		assertFalse(dosAttrsCorrect.isHidden());
		assertFalse(dosAttrsCorrect.isArchive());
		assertFalse(dosAttrsCorrect.isSystem());
		
		// Can't do all ...
	}

	@Test
	public void testFileDosAttrsToString() throws Exception {
		assertEquals("rhas", LimitedDosFileAttributes.toString(FileUtil.dosAttrsFromString("rhas")));
		assertEquals("rh--", LimitedDosFileAttributes.toString(new DosFileAttributes() {			
			public long size() { return 0; }
			public FileTime lastModifiedTime() { return null; }
			public FileTime lastAccessTime() { return null; }
			public boolean isSymbolicLink() { return false; }
			public boolean isRegularFile() { return false; }
			public boolean isOther() { return false; }
			public boolean isDirectory() { return false; }
			public Object fileKey() { return null; }
			public FileTime creationTime() { return null; }
			
			public boolean isReadOnly() { return true; } // r
			public boolean isHidden() { return true; }   // h
			public boolean isArchive() { return false; } // -
			public boolean isSystem() { return false; }  // -
		}));		
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testFileDosAttrsInvalid1() throws Exception {
		FileUtil.dosAttrsFromString("illegal");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testFileDosAttrsInvalid2() throws Exception {
		FileUtil.dosAttrsFromString(null);
	}
	
	@Test(expected = RuntimeException.class)
	public void testFileDosAttrsIllegalSize() throws Exception {
		FileUtil.dosAttrsFromString("rhas").size();
	}
	
	@Test(expected = RuntimeException.class)
	public void testFileDosAttrsIllegalLastModified() throws Exception {
		FileUtil.dosAttrsFromString("rhas").lastModifiedTime();
	}
	
	@Test(expected = RuntimeException.class)
	public void testFileDosAttrsIllegalLastAccess() throws Exception {
		FileUtil.dosAttrsFromString("rhas").lastAccessTime();
	}
	
	@Test(expected = RuntimeException.class)
	public void testFileDosAttrsIllegalIsSymlink() throws Exception {
		FileUtil.dosAttrsFromString("rhas").isSymbolicLink();
	}
	
	@Test(expected = RuntimeException.class)
	public void testFileDosAttrsIllegalIsRegular() throws Exception {
		FileUtil.dosAttrsFromString("rhas").isRegularFile();
	}
	
	@Test(expected = RuntimeException.class)
	public void testFileDosAttrsIllegalIsDirectory() throws Exception {
		FileUtil.dosAttrsFromString("rhas").isDirectory();
	}
	
	@Test(expected = RuntimeException.class)
	public void testFileDosAttrsIllegalFileKey() throws Exception {
		FileUtil.dosAttrsFromString("rhas").fileKey();
	}	

	@Test(expected = RuntimeException.class)
	public void testFileDosAttrsIllegalCreationTime() throws Exception {
		FileUtil.dosAttrsFromString("rhas").creationTime();
	}	
}
