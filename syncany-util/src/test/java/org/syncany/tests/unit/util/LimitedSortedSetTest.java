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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.syncany.util.LimitedSortedSet;

public class LimitedSortedSetTest {
	@Test
	public void testLimitedSortedSet1() {
		LimitedSortedSet<Integer> lss = new LimitedSortedSet<Integer>(3);
		
		lss.add(5);
		assertEquals("[5]", lss.toString());
		
		lss.add(4); 
		assertEquals("[4, 5]", lss.toString());
		
		lss.add(3);
		assertEquals("[3, 4, 5]", lss.toString());
		
		lss.add(2); 
		assertEquals("[2, 3, 4]", lss.toString());

		lss.add(1); 
		assertEquals("[1, 2, 3]", lss.toString());

		lss.add(6); 
		assertEquals("[1, 2, 3]", lss.toString());

		lss.add(0); 
		assertEquals("[0, 1, 2]", lss.toString());

		lss.remove(0); 
		assertEquals("[1, 2]", lss.toString());
		
		lss.remove(6); 
		assertEquals("[1, 2]", lss.toString());
		
		lss.addAll(Arrays.asList(new Integer[] { 8, 7, 1, -1 }));
		assertEquals("[-1, 1, 2]", lss.toString());
	}
}
