package org.syncany.database;

import java.util.Date;

public class DatabaseVersionHeader {

    // DB Version and versions of other users (= DB basis)
    private Date uploaded;
    private VectorClock vectorClock; // vector clock, machine name to database version map
    private String uploadedByClient;
    
    public DatabaseVersionHeader() {
    	uploaded = new Date();
    	vectorClock = new VectorClock();
    	uploadedByClient = "UnknownMachine";
    }
    
	public DatabaseVersionHeader(Date uploaded, VectorClock vectorClock, String uploadedByClient) {
		this.uploaded = uploaded;
		this.vectorClock = vectorClock;
		this.uploadedByClient = uploadedByClient;
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
	
	public String getUploadedByClient() {
		return uploadedByClient;
	}

	public void setUploadedByClient(String uploadedByClient) {
		this.uploadedByClient = uploadedByClient;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uploaded == null) ? 0 : uploaded.hashCode());
		result = prime * result + ((uploadedByClient == null) ? 0 : uploadedByClient.hashCode());
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
		if (uploaded == null) {
			if (other.uploaded != null)
				return false;
		} else if (!uploaded.equals(other.uploaded))
			return false;
		if (uploadedByClient == null) {
			if (other.uploadedByClient != null)
				return false;
		} else if (!uploadedByClient.equals(other.uploadedByClient))
			return false;
		if (vectorClock == null) {
			if (other.vectorClock != null)
				return false;
		} else if (!vectorClock.equals(other.vectorClock))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
				
		sb.append(uploadedByClient);
		sb.append("/");
		sb.append(vectorClock.toString());
		sb.append("/T=");
		sb.append(uploaded.getTime());
		
		return sb.toString();
	}
  
}
