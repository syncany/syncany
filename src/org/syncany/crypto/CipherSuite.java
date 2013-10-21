package org.syncany.crypto;

public class CipherSuite {
	private int id;
	private boolean unlimitedStrength;
	private String cipherStr;
	private int keySize;
	private boolean iv;		
	private int ivSize;
	
	public CipherSuite(int id, String cipherStr, int keySize, boolean unlimitedStrength, boolean iv, int ivSize) {
		this.id = id;
		this.unlimitedStrength = unlimitedStrength;
		this.cipherStr = cipherStr;
		this.keySize = keySize;
		this.iv = iv;
		this.ivSize = ivSize;
	}
	
	public int getId() {
		return id;
	}

	public boolean isUnlimitedStrength() {
		return unlimitedStrength;
	}

	public String getCipherStr() {
		return cipherStr;
	}

	public int getKeySize() {
		return keySize;
	}

	public boolean isIv() {
		return iv;
	}

	public int getIvSize() {
		return ivSize;
	}	
	
	@Override
	public String toString() {
		return cipherStr+", "+keySize+" bit";
	}
}
