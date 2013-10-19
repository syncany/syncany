package org.syncany.util;

import java.util.Arrays;
import java.util.Collection;

public abstract class CollectionUtil<T> {	
	/**
	 * Checks if the collection contains only the given allowed items. <tt>list</tt> 
	 * may contain zero to max(allowedItems) items from allowedItems.
	 * 
	 * <p><b>Examples:</b><br>
	 * <tt>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }))        --> true</tt><br>
	 * <tt>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), 1, 2)  --> false, 3 missing</tt><br>
	 * <tt>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), 1, 99) --> false, 3 missing</tt>
	 */
	@SafeVarargs
	public static <T> boolean containsOnly(Collection<T> list, T... allowedItems) {
		return containsOnly(list, Arrays.asList(allowedItems));
	}
	
	/**
	 * Checks if the collection contains only the given allowed items. <tt>list</tt> 
	 * may contain zero to max(allowedItems) items from allowedItems.
	 * 
	 * <p><b>Examples:</b><br>
	 * <tt>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), Arrays.asList(new Integer[] { }))       --> true</tt><br>
	 * <tt>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), Arrays.asList(new Integer[] { 1, 2 }))  --> false, 3 missing</tt><br>
	 * <tt>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), Arrays.asList(new Integer[] { 1, 99 })) --> false, 3 missing</tt>
	 */
	public static <T> boolean containsOnly(Collection<T> list, Collection<T> allowedItems) {
		for (T item : list) {
			if (!allowedItems.contains(item)) {
				return false;
			}
		}
		
		return true;		
	}	

	/**
	 * Checks if the collection contains exactly the given <tt>mustHaveItems</tt>. 
	 * 
	 * <p><b>Examples:</b><br>
	 * <tt>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }))          --> false</tt><br>
	 * <tt>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), 1, 99)   --> false</tt><br>
	 * <tt>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), 1, 2, 3) --> true</tt>
	 */
	@SafeVarargs
	public static <T> boolean containsExactly(Collection<T> list, T... mustHaveItems) {
		return containsExactly(list, Arrays.asList(mustHaveItems));
	}
	
	/**
	 * Checks if the collection contains exactly the given <tt>mustHaveItems</tt>.
	 * 
	 * <p><b>Examples:</b><br>
	 * <tt>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), Arrays.asList(new Integer[] { }))         --> false</tt><br>
	 * <tt>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), Arrays.asList(new Integer[] { 1, 99 }))   --> false</tt><br>
	 * <tt>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), Arrays.asList(new Integer[] { 1, 2, 3 })) --> true</tt>
	 */
	public static <T> boolean containsExactly(Collection<T> list, Collection<T> mustHaveItems) {
		return list.containsAll(mustHaveItems) && mustHaveItems.containsAll(list);
	}		
}
