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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.syncany.util.CollectionUtil;

public class CollectionUtilTest {
	@Test
	public void testContainsOnlySuccessAll() {
		List<Integer> list = Arrays.asList(new Integer[] { 1, 2, 3, 4});		
		assertTrue(CollectionUtil.containsOnly(list, 2, 3, 4, 1));		
	}
	
	@Test
	public void testContainsOnlySuccessSome() {
		List<Integer> list = Arrays.asList(new Integer[] { 1, 2, 3, 4});		
		assertFalse(CollectionUtil.containsOnly(list, 1, 2, 3)); // 4 missing		
	}
	
	@Test
	public void testContainsOnlySuccessMore() {
		List<Integer> list = Arrays.asList(new Integer[] { 1, 2, 3, 4});		
		assertTrue(CollectionUtil.containsOnly(list, 1, 2, 3, 4, 5, 6, 7, 8));		
	}
	
	@Test
	public void testContainsOnlySuccessNone() {
		List<Integer> list = Arrays.asList(new Integer[] { 1, 2, 3, 4});		
		assertFalse(CollectionUtil.containsOnly(list)); // 1, 2, 3, 4 missing		
	}
	
	@Test
	public void testContainsOnlyFailure() {
		List<Integer> list = Arrays.asList(new Integer[] { 1, 2, 3, 4});		
		assertFalse(CollectionUtil.containsOnly(list, 100, 2, 3));		
	}

	@Test
	public void testContainsExactlySuccessAll() {
		List<Integer> list = Arrays.asList(new Integer[] { 1, 2, 3, 4});		
		assertTrue(CollectionUtil.containsExactly(list, 2, 3, 4, 1));	
	}	

	@Test
	public void testContainsExactlyFailureSome() {
		List<Integer> list = Arrays.asList(new Integer[] { 1, 2, 3, 4});		
		assertFalse(CollectionUtil.containsExactly(list, 1, 2));	
	}

	@Test
	public void testContainsExactlyFailureNone() {
		List<Integer> list = Arrays.asList(new Integer[] { 1, 2, 3, 4});		
		assertFalse(CollectionUtil.containsExactly(list));	
	}
	
	@Test
	public void testContainsExactlyFailureOthers() {
		List<Integer> list = Arrays.asList(new Integer[] { 1, 2, 3, 4});		
		assertFalse(CollectionUtil.containsExactly(list, 999));	
	}	
}
