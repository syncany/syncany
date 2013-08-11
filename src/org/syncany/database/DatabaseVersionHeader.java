package org.syncany.database;

import java.util.Date;

public class DatabaseVersionHeader {

    // DB Version and versions of other users (= DB basis)
    private Date date;
    private VectorClock vectorClock; // vector clock, machine name to database version map
    private String client;
    private String previousClient;
    
    public DatabaseVersionHeader() {
    	this.date = new Date();
    	this.vectorClock = new VectorClock();
    	this.client = "UnknownMachine";
    	this.previousClient = "UnknownMachine";
    }    

	public Date getDate() {
		return date;
	}
	
	public void setDate(Date timestamp) {
		this.date = timestamp;
	}
	
	public VectorClock getVectorClock() {
		return vectorClock;
	}
	
	public VectorClock getPreviousVectorClock() {
		VectorClock previousVectorClock = vectorClock.clone();

		Long lastPreviousClientLocalClock = previousVectorClock.get(client);
		
		if (lastPreviousClientLocalClock == null) {
			throw new RuntimeException("Previous client '"+client+"' must be present in vector clock of database version header "+this.toString()+".");
		}
		
		if (lastPreviousClientLocalClock == 1) {
			previousVectorClock.remove(client);
			
			if (previousVectorClock.size() == 0) {
				return new VectorClock();
			}
			else {
				return previousVectorClock;
			}
		}
		else {
			previousVectorClock.setClock(client, lastPreviousClientLocalClock-1);
			return previousVectorClock;
		}		
	}
	
	public void setVectorClock(VectorClock vectorClock) {
		this.vectorClock = vectorClock;
	}
	
	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
	}

	public String getPreviousClient() {
		return previousClient;
	}

	public void setPreviousClient(String previousClient) {
		this.previousClient = previousClient;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((client == null) ? 0 : client.hashCode());
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
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (client == null) {
			if (other.client != null)
				return false;
		} else if (!client.equals(other.client))
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
				
		sb.append(client);
		sb.append("/");
		sb.append(vectorClock.toString());
		sb.append("/T=");
		sb.append(date.getTime());
		
		if (previousClient != null) {
			sb.append("/");
			sb.append(previousClient);
		}
		
		return sb.toString();
	}
  
}
