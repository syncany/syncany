package org.syncany.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public abstract class CollectionUtil<T> {
	public static <T> boolean containsOnly(Collection<T> list, T... allowedItems) {
		return containsOnly(list, Arrays.asList(allowedItems));
	}
	
	public static <T> boolean containsOnly(Collection<T> list, Collection<T> allowedItems) {
		return intersection(list, allowedItems).size() > 0;
	}	

    public static <T> Collection<T> intersection(Collection<T> list1, Collection<T> list2) {
    	Collection<T> list = new ArrayList<T>();

        for (T listEntry : list1) {
            if(list2.contains(listEntry)) {
                list.add(listEntry);
            }
        }

        return list;
    }
}
