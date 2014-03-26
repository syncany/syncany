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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.attribute.DosFileAttributes;

import org.junit.Test;
import org.syncany.util.FileUtil;

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
	
	@Test(expected = IllegalArgumentException.class)
	public void testFileDosAttrsInvalid1() throws Exception {
		FileUtil.dosAttrsFromString("illegal");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testFileDosAttrsInvalid2() throws Exception {
		FileUtil.dosAttrsFromString(null);
	}
}
