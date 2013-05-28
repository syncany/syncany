package org.syncany.database;

import java.util.Date;

public class DatabaseVersionHeader {

    // DB Version and versions of other users (= DB basis)
    private Date uploaded;
    private VectorClock vectorClock; // vector clock, machine name to database version map
    private String uploadedFromClient;
    
    public DatabaseVersionHeader() {
    	uploaded = new Date();
    	vectorClock = new VectorClock();
    }
    
	public Date getUploadedDate() {
		return uploaded;
	}
	public void setUploadedDate(Date timestamp) {
		this.uploaded = timestamp;
	}
	public VectorClock getVectorClock() {
		return vectorClock;
	}
	public void setVectorClock(VectorClock vectorClock) {
		this.vectorClock = vectorClock;
	}
	
	public String getUploadedFromClient() {
		return uploadedFromClient;
	}

	public void setUploadedFromClient(String uploadedFromClient) {
		this.uploadedFromClient = uploadedFromClient;
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
		DatabaseVersionHeader other = (DatabaseVersionHeader) obj;
		if (vectorClock == null) {
			if (other.vectorClock != null)
				return false;
		} else if (!vectorClock.equals(other.vectorClock))
			return false;
		return true;
	}
  
}
