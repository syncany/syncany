package org.syncany.tests.util;

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
