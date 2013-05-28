package org.syncany.database;

import java.util.Date;

public class DatabaseVersionIdentifier {

    // DB Version and versions of other users (= DB basis)
	// TODO weird
    private Date timestamp;
    private VectorClock vectorClock; // vector clock, machine name to database version map
    
    public DatabaseVersionIdentifier() {
    	timestamp = new Date();
    	vectorClock = new VectorClock();
    }
    
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public VectorClock getVectorClock() {
		return vectorClock;
	}
	public void setVectorClock(VectorClock vectorClock) {
		this.vectorClock = vectorClock;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((vectorClock == null) ? 0 : vectorClock.hashCode());
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
		DatabaseVersionIdentifier other = (DatabaseVersionIdentifier) obj;
		if (vectorClock == null) {
			if (other.vectorClock != null)
				return false;
		} else if (!vectorClock.equals(other.vectorClock))
			return false;
		return true;
	}
  
}
