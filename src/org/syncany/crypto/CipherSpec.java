package org.syncany.crypto;


public class CipherSpec {
	private int id;
	private String algorithm;
	private int keySize; // in bits
	private int ivSize; // in bits
	private boolean needsUnlimitedStrength;
	
	public CipherSpec(int id, String algorithm, int keySize, int ivSize, boolean needsUnlimitedStrength) {
		this.id = id;
		this.algorithm = algorithm;
		this.keySize = keySize;
		this.ivSize = ivSize;
		this.needsUnlimitedStrength = needsUnlimitedStrength;
		
		doSanityChecks();
	}
	
	public int getId() {
		return id;
	}

	public boolean needsUnlimitedStrength() {
		return needsUnlimitedStrength;
	}

	public String getAlgorithm() {
		return algorithm;
	}	

	public int getKeySize() {
		return keySize;
	}

	public int getIvSize() {
		return ivSize;
	}		
	
	@Override
	public String toString() {
		return algorithm+", "+keySize+" bit";
	}

	private void doSanityChecks() {
		if (algorithm.matches("(^DES/|/ECB/|/CBC/)")) {
			throw new RuntimeException("Cipher algorithm or mode not allowed: "+algorithm+". This mode is not considered secure.");
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((algorithm == null) ? 0 : algorithm.hashCode());
		result = prime * result + id;
		result = prime * result + ivSize;
		result = prime * result + keySize;
		result = prime * result + (needsUnlimitedStrength ? 1231 : 1237);
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
		CipherSpec other = (CipherSpec) obj;
		if (algorithm == null) {
			if (other.algorithm != null)
				return false;
		} else if (!algorithm.equals(other.algorithm))
			return false;
		if (id != other.id)
			return false;
		if (ivSize != other.ivSize)
			return false;
		if (keySize != other.keySize)
			return false;
		if (needsUnlimitedStrength != other.needsUnlimitedStrength)
			return false;
		return true;
	}	
}
