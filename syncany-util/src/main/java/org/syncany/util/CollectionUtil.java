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
package org.syncany.util;

import java.util.Arrays;
import java.util.Collection;

public abstract class CollectionUtil {	
	/**
	 * Checks if the collection contains only the given allowed items. <code>list</code>
	 * may contain zero to max(allowedItems) items from allowedItems.
	 * 
	 * <p><b>Examples:</b><br>
	 * <code>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }))        --&gt; true</code><br>
	 * <code>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), 1, 2)  --&gt; false, 3 missing</code><br>
	 * <code>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), 1, 99) --&gt; false, 3 missing</code>
	 */
	@SafeVarargs
	public static <T> boolean containsOnly(Collection<T> list, T... allowedItems) {
		return containsOnly(list, Arrays.asList(allowedItems));
	}
	
	/**
	 * Checks if the collection contains only the given allowed items. <code>list</code>
	 * may contain zero to max(allowedItems) items from allowedItems.
	 * 
	 * <p><b>Examples:</b><br>
	 * <code>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), Arrays.asList(new Integer[] { }))       --&gt; true</code><br>
	 * <code>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), Arrays.asList(new Integer[] { 1, 2 }))  --&gt; false, 3 missing</code><br>
	 * <code>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), Arrays.asList(new Integer[] { 1, 99 })) --&gt; false, 3 missing</code>
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
	 * Checks if the collection contains exactly the given <code>mustHaveItems</code>.
	 * 
	 * <p><b>Examples:</b><br>
	 * <code>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }))          --&gt; false</code><br>
	 * <code>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), 1, 99)   --&gt; false</code><br>
	 * <code>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), 1, 2, 3) --&gt; true</code>
	 */
	@SafeVarargs
	public static <T> boolean containsExactly(Collection<T> list, T... mustHaveItems) {
		return containsExactly(list, Arrays.asList(mustHaveItems));
	}
	
	/**
	 * Checks if the collection contains exactly the given <code>mustHaveItems</code>.
	 * 
	 * <p><b>Examples:</b><br>
	 * <code>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), Arrays.asList(new Integer[] { }))         --&gt; false</code><br>
	 * <code>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), Arrays.asList(new Integer[] { 1, 99 }))   --&gt; false</code><br>
	 * <code>containsOnly(Arrays.asList(new Integer[] { 1, 2, 3 }), Arrays.asList(new Integer[] { 1, 2, 3 })) --&gt; true</code>
	 */
	public static <T> boolean containsExactly(Collection<T> list, Collection<T> mustHaveItems) {
		return list.containsAll(mustHaveItems) && mustHaveItems.containsAll(list);
	}		
}
