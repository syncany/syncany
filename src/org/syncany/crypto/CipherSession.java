package org.syncany.crypto;

public class CipherSession {
	private String password;
	
	public CipherSession(String password) {
		this.password = password;
	}
	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
