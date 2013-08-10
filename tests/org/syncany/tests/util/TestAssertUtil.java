package org.syncany.tests.util;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Iterator;

public class TestAssertUtil {
	public static void assertCollectionEquals(String message, Collection<? extends Object> expected, Collection<? extends Object> actual) {
		assertEquals(message+": Different amount of objects.", expected.size(), actual.size());
		
		Iterator<? extends Object> expectedIt = expected.iterator();
		Iterator<? extends Object> actualIt = actual.iterator();
		
		int i = 0;
		while (expectedIt.hasNext()) {			
			Object expectedObj = expectedIt.next();			
			Object actualObj = actualIt.next();
			
			assertEquals(message+": actual["+i+"] differs from expected["+i+"]: ", expectedObj, actualObj);
			i++;
		}
	}
}
