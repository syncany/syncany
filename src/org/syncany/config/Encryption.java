/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.config;

import java.lang.reflect.Field;
import java.security.Security;

import javax.crypto.Cipher;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */ 
public class Encryption {	
    public static final String PROVIDER = "BC";
    public static final String KEY_DERIVATION_FUNCTION = "PBKDF2WithHmacSHA1";
	    
	public static final String DEFAULT_CIPHER_ALGORITHM = "AES";
	public static final String DEFAULT_CIPHER_STRING = "AES/GCM/NoPadding";
    public static final int DEFAULT_KEYLENGTH = 128;
    public static final boolean DEFAULT_IV_NEEDED = true;
    public static final boolean DEFAULT_UNLIMITED_CRYPTO_NEEDED = false;
    
	private String password;
    private String cipherStr;
    private int keySize;
    private boolean ivNeeded;
    private boolean unlimitedCryptoNeeded;
    
    static {
    	try {
			init();
		}
    	catch (EncryptionException e) {
			throw new RuntimeException(e);
		}
    }
    
    public static synchronized void init() throws EncryptionException {
    	if (Security.getProvider("BC") == null) {
    		Security.addProvider(new BouncyCastleProvider()); 
    	}
    }
    
    public static synchronized boolean isUnlimitedCrypto() {
    	try {
    		return Cipher.getMaxAllowedKeyLength("AES") > 128;
    	}
    	catch (Exception e) {
    		return false;
    	}
    }
    
    public static void enableUnlimitedCrypto() throws EncryptionException {
    	if (!isUnlimitedCrypto()) {
			try {
				Field field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
	
				field.setAccessible(true);
				field.set(null, false);		
			}
			catch (Exception e) {
				throw new EncryptionException(e);
			}
    	}
    }
    
    public Encryption() {
        this("", DEFAULT_CIPHER_STRING, DEFAULT_KEYLENGTH, DEFAULT_IV_NEEDED, DEFAULT_UNLIMITED_CRYPTO_NEEDED); 
    }

    public Encryption(String password, String cipherStr, int keySize, boolean ivNeeded, boolean unlimitedNeeded) {
        this.password = password;
        this.cipherStr = cipherStr;
        this.keySize = keySize;
        this.ivNeeded = ivNeeded;
        this.unlimitedCryptoNeeded = unlimitedNeeded;
    } 

    public String getCipherStr() {
        return cipherStr;
    }

    public void setCipherStr(String cipherStr) {
        this.cipherStr = cipherStr;
    }

    public Integer getKeySize() {
        return keySize;
    }

    public void setKeySize(Integer keySize) {
        this.keySize = keySize;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }    
    
    public boolean isIvNeeded() {
		return ivNeeded;
	}
    
    public void setIvNeeded(boolean ivNeeded) {
		this.ivNeeded = ivNeeded;
	}
    
    public boolean isUnlimitedNeeded() {
		return unlimitedCryptoNeeded;
	}
    
    public void setUnlimitedCryptoNeeded(boolean unlimitedNeeded) {
		this.unlimitedCryptoNeeded = unlimitedNeeded;
	}
}
