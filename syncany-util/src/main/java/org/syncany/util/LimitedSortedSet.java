package org.syncany.util;

import java.util.Collection;
import java.util.TreeSet;

/**
 * A limited sorted set is a {@link TreeSet} with limited entries.
 * Entries that exceed the maximum size of the set (<em>maxSize</em>) 
 * will be removed from the set. 
 * 
 * @see <a href="http://stackoverflow.com/questions/8382529/limited-sortedset">Original code on stackoverflow.com</a>
 * @author Thomas ?, see http://stackoverflow.com/users/637853/thomas
 */
@SuppressWarnings("unchecked")
public class LimitedSortedSet<E> extends TreeSet<E> {
	private static final long serialVersionUID = -4876601911765911284L;
	private int maxSize;

	public LimitedSortedSet(int maxSize) {
		this.maxSize = maxSize;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		boolean added = super.addAll(c);
		
		if (size() > maxSize) {			
			E firstToRemove = (E) toArray()[maxSize];
			removeAll(tailSet(firstToRemove));
		}
		
		return added;
	}

	@Override
	public boolean add(E o) {
		boolean added = super.add(o);
		
		if (size() > maxSize) {
			E firstToRemove = (E) toArray()[maxSize];
			removeAll(tailSet(firstToRemove));
		}
		
		return added;
	}
}