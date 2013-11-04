package org.syncany.crypto;

import javax.crypto.SecretKey;

public class SaltedSecretKey implements SecretKey {
	private static final long serialVersionUID = 1216126055360327470L;
	
	private SecretKey secretKey;
	private byte[] salt;
	
	public SaltedSecretKey(SecretKey secretKey, byte[] salt) {
		this.secretKey = secretKey;
		this.salt = salt;
	}

	public byte[] getSalt() {
		return salt;
	}

	@Override
	public String getAlgorithm() {
		return secretKey.getAlgorithm();
	}

	@Override
	public String getFormat() {
		return secretKey.getFormat();
	}

	@Override
	public byte[] getEncoded() {
		return secretKey.getEncoded();
	}		
}
