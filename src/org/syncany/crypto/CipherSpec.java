package org.syncany.crypto;

public class CipherSpec {
	private int id;
	private boolean unlimitedStrength;
	private String cipherStr;
	private int keySize;
	private int ivSize;
	
	public CipherSpec(int id, String cipherStr, int keySize, boolean unlimitedStrength, int ivSize) {
		this.id = id;
		this.unlimitedStrength = unlimitedStrength;
		this.cipherStr = cipherStr;
		this.keySize = keySize;
		this.ivSize = ivSize;
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

	public int getIvSize() {
		return ivSize;
	}		
	
	@Override
	public String toString() {
		return cipherStr+", "+keySize+" bit";
	}
}
