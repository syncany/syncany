package org.syncany.crypto;

public class CipherSpec {
	private int id;
	private boolean unlimitedStrength;
	private String cipherStr;
	private int keySize;
	private boolean iv;		
	private int ivSize;
	private boolean authenticated;
	
	public CipherSpec(int id, String cipherStr, int keySize, boolean unlimitedStrength, boolean iv, int ivSize, boolean authenticated) {
		this.id = id;
		this.unlimitedStrength = unlimitedStrength;
		this.cipherStr = cipherStr;
		this.keySize = keySize;
		this.iv = iv;
		this.ivSize = ivSize;
		this.authenticated = authenticated;
	}
	
	public int getId() {
		return id;
	}

	public boolean needsUnlimitedStrength() {
		return unlimitedStrength;
	}

	public String getCipherStr() {
		return cipherStr;
	}	

	public int getKeySize() {
		return keySize;
	}

	public boolean hasIv() {
		return iv;
	}

	public int getIvSize() {
		return ivSize;
	}		
	
	public boolean isAuthenticated() {
		return authenticated;
	}

	@Override
	public String toString() {
		return cipherStr+", "+keySize+" bit";
	}
}
