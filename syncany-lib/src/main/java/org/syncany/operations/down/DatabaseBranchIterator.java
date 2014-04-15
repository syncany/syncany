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
package org.syncany.operations.down;

import java.util.Iterator;
import java.util.List;

import org.syncany.database.DatabaseVersionHeader;

/**
 * The database branch iterator implements an {@link Iterator} and is used 
 * by the {@link DatabaseBranch} to walk through a branch. The class implements
 * forwards and backwards iteration.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class DatabaseBranchIterator implements Iterator<DatabaseVersionHeader> {
	private List<DatabaseVersionHeader> branch;
    private int current;
    
    public DatabaseBranchIterator(List<DatabaseVersionHeader> branch, int current) {
    	this.branch = branch;
    	this.current = current;
    }
    
	@Override
	public boolean hasNext() {
		return current < branch.size();
	}
	
	public boolean hasPrevious() {
		return current >= 0;
	}

	@Override
	public DatabaseVersionHeader next() {
		return branch.get(current++);
	}
	
	public DatabaseVersionHeader previous() {
		return branch.get(current--);
	}

	@Override
	public void remove() {
		throw new RuntimeException("Operation not supported, DatabaseBranchIterator.remove()");			
	}	
}