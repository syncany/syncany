package org.syncany.operations;

import org.syncany.database.DatabaseVersionHeader;

public class DatabaseVersionHeaderPair {
	private DatabaseVersionHeader firstHeader;
	private DatabaseVersionHeader secondHeader;
	
	public DatabaseVersionHeaderPair(DatabaseVersionHeader firstHeader, DatabaseVersionHeader secondHeader) {
		this.firstHeader = firstHeader;
		this.secondHeader = secondHeader;
	}
	public DatabaseVersionHeader getFirstHeader() {
		return firstHeader;
	}
	public DatabaseVersionHeader getSecondHeader() {
		return secondHeader;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((firstHeader == null) ? 0 : firstHeader.hashCode());
		result = prime * result + ((secondHeader == null) ? 0 : secondHeader.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DatabaseVersionHeaderPair other = (DatabaseVersionHeaderPair) obj;
		if (firstHeader == null) {
			if (other.firstHeader != null)
				return false;
		} else if (!firstHeader.equals(other.firstHeader))
			return false;
		if (secondHeader == null) {
			if (other.secondHeader != null)
				return false;
		} else if (!secondHeader.equals(other.secondHeader))
			return false;
		return true;
	}			
}	