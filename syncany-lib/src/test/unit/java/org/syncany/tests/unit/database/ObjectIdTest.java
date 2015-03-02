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
package org.syncany.tests.unit.database;

import org.junit.Assert;
import org.junit.Test;
import org.syncany.database.ObjectId;

public class ObjectIdTest {	
	@Test(expected = IllegalArgumentException.class)
	public void testObjectIdParsedInvalidHex1() {
		ObjectId.parseObjectId("1");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testObjectIdParsedInvalidHex2() {
		ObjectId.parseObjectId("X");
	}
	
	@Test
	public void testObjectIdParsedEmpty() {
		Assert.assertArrayEquals(new byte[] { }, ObjectId.parseObjectId(""));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testObjectIdParsedInvalidNull() {
		ObjectId.parseObjectId(null);
	}
	
	@Test
	public void testObjectIdParsedOk() {
		// TODO [low] ObjectId.parseObjectId() should return an ObjectId instead of a byte array
		// assertEquals("12", ObjectId.parseObjectId("12").toString());
	}
}
