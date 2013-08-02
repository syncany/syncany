package org.syncany.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Branch {
	private ArrayList<DatabaseVersionHeader> branch;
	
	public Branch() {
		this.branch = new ArrayList<DatabaseVersionHeader>();
	}
	
	public void add(DatabaseVersionHeader header) {
		branch.add(header);		
	}	
	
	public void addAll(List<DatabaseVersionHeader> headers) {
		branch.addAll(headers);
	}	
	
	public int size() {
		return branch.size();
	}
	
	public DatabaseVersionHeader get(int index) {
		return branch.get(index);
	}

	public List<DatabaseVersionHeader> getAll() {
		return Collections.unmodifiableList(branch);
	}	
	
	public DatabaseVersionHeader getFirst() {
		return branch.get(0);
	}	
	
	public DatabaseVersionHeader getLast() {
		return branch.get(branch.size()-1);
	}	
	
	public BranchIterator iteratorLast() {
        return new BranchIterator(branch.size()-1);
    }
	
	public BranchIterator iteratorFirst() {
        return new BranchIterator(0);
    }	
	
	@Override
	public String toString() {
		return branch.toString();
	}
	
	public class BranchIterator implements Iterator<DatabaseVersionHeader> {		
        private int current;
        
        public BranchIterator(int current) {
        	this.current = 0;
        }
        
		@Override
		public boolean hasNext() {
			return current+1 < branch.size();
		}
		
		public boolean hasPrevious() {
			return current-1 >= 0;
		}

		@Override
		public DatabaseVersionHeader next() {
			return branch.get(++current);
		}
		
		public DatabaseVersionHeader previous() {
			return branch.get(--current);
		}

		@Override
		public void remove() {
			throw new RuntimeException("Operation not supported, BranchIterator.remove()");			
		}
		
	}

}
