package org.syncany.crypto;

import javax.crypto.SecretKey;

public class CipherSession {
	private String password;
	
	private byte[] sessionWriteSalt;
	private SecretKey sessionWriteSecretKey;
	
	private byte[] lastReadSalt;
	private SecretKey lastReadSecretKey;	
	
	public CipherSession(String password) {
		this.password = password;
	}
	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public byte[] getSessionWriteSalt() {
		return sessionWriteSalt;
	}

	public void setSessionWriteSalt(byte[] sessionWriteSalt) {
		this.sessionWriteSalt = sessionWriteSalt;
	}

	public SecretKey getSessionWriteSecretKey() {
		return sessionWriteSecretKey;
	}

	public void setSessionWriteSecretKey(SecretKey sessionWriteSecretKey) {
		this.sessionWriteSecretKey = sessionWriteSecretKey;
	}

	public byte[] getLastReadSalt() {
		return lastReadSalt;
	}

	public void setLastReadSalt(byte[] lastReadSalt) {
		this.lastReadSalt = lastReadSalt;
	}

	public SecretKey getLastReadSecretKey() {
		return lastReadSecretKey;
	}

	public void setLastReadSecretKey(SecretKey lastReadSecretKey) {
		this.lastReadSecretKey = lastReadSecretKey;
	}
}
