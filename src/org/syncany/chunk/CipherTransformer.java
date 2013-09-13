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
package org.syncany.chunk;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.syncany.config.Encryption;
import org.syncany.config.EncryptionException;

/**
 *
 * @author pheckel
 */
public class CipherTransformer extends Transformer {
	private Encryption encryption;
	
	private byte[] sessionWriteSalt;
	private SecretKey sessionWriteSecretKey;
	
	private byte[] lastReadSalt;
	private SecretKey lastReadSecretKey;	
    
    public CipherTransformer(Encryption encryption) {
    	this.encryption = encryption;
    }   
    
    @Override
    public String toString() {
        return (nextTransformer == null) ? "Cipher" : "Cipher-"+nextTransformer;
    }     
    
    @Override
	public OutputStream createOutputStream(OutputStream out) throws IOException {
    	try {
			// Create and write session salt to unencrypted stream
			if (sessionWriteSecretKey == null || sessionWriteSalt == null) {
				sessionWriteSalt = createSalt();
				sessionWriteSecretKey = createSecretKey(sessionWriteSalt);
			}
			
			out.write(sessionWriteSalt);
			
			// Create and write random IV to unencrypted stream
			byte[] streamIV = new byte[encryption.getKeySize()/8]; 
			new SecureRandom().nextBytes(streamIV);
			
			out.write(streamIV);
			
			// Initialize cipher
			Cipher streamEncryptCipher = createEncCipher(sessionWriteSecretKey, streamIV);
	
			// Now create cipher stream and write to encrypted stream
	        CipherOutputStream cipherOutputStream = new CipherOutputStream(out, streamEncryptCipher);
	        
	        return cipherOutputStream;
    	}
    	catch (Exception e) {
    		throw new IOException(e);
    	}
    }

    @Override
    public InputStream createInputStream(InputStream in) throws IOException {
    	try {
	    	// Read salt from unencrypted stream
	    	byte[] streamSalt = new byte[encryption.getKeySize()/8]; 
	    	in.read(streamSalt);
	    	
			// Read IV from unencrypted stream
			byte[] streamIV = new byte[encryption.getKeySize()/8];		
			in.read(streamIV);
			
			// Create key
			SecretKey streamKey = null;
			
			if (lastReadSalt != null && Arrays.equals(lastReadSalt, streamSalt)) {
				streamKey = lastReadSecretKey;
			}
			else {
				streamKey = createSecretKey(streamSalt);
				
				lastReadSalt = streamSalt;
				lastReadSecretKey = streamKey;
			}
			
			// Initialize cipher
			Cipher streamDecryptCipher = createDecCipher(streamKey, streamIV);
	
			// Now create cipher stream and write to encrypted stream
			CipherInputStream cipherInputStream = new CipherInputStream(in, streamDecryptCipher);		
	        
	        return cipherInputStream;
    	}
    	catch (Exception e) {
    		throw new IOException(e);
    	}
    }

    private byte[] createSalt() {
    	byte[] salt = new byte[encryption.getKeySize()/8];    	
    	new SecureRandom().nextBytes(salt);
    	
    	return salt;
    }
    
    private SecretKey createSecretKey(byte[] keySalt) throws InvalidKeySpecException, NoSuchAlgorithmException {
    	// Derive secret key from password 
    	SecretKeyFactory factory = SecretKeyFactory.getInstance(Encryption.KEY_DERIVATION_FUNCTION);
        KeySpec pbeKeySpec = new PBEKeySpec(encryption.getPassword().toCharArray(), keySalt, 1000, encryption.getKeySize());
        SecretKey secretKey = factory.generateSecret(pbeKeySpec);
        
        // The key name must be "AES" if cipherStr is "AES/...". This is really odd, but necessary
        String algorithm = (encryption.getCipherStr().indexOf('/') != -1) ? encryption.getCipherStr().substring(0, encryption.getCipherStr().indexOf('/')) : encryption.getCipherStr();
        SecretKey secretKeyAlgorithm = new SecretKeySpec(secretKey.getEncoded(), algorithm);  
        
        return secretKeyAlgorithm;
    }
    
	private Cipher createCipher(int cipherInitMode, SecretKey secretKey, byte[] iv) throws EncryptionException {
		try {
            Cipher cipher = Cipher.getInstance(encryption.getCipherStr(), Encryption.PROVIDER);
            cipher.init(cipherInitMode, secretKey, new IvParameterSpec(iv));        

            return cipher;
        }
        catch (Exception e) {
            throw new EncryptionException(e);
        }
	}

	private Cipher createEncCipher(SecretKey secretKey, byte[] iv) throws EncryptionException {
		return createCipher(Cipher.ENCRYPT_MODE, secretKey, iv);
	}   
	
	private Cipher createDecCipher(SecretKey secretKey, byte[] iv) throws EncryptionException {
		return createCipher(Cipher.DECRYPT_MODE, secretKey, iv);
	}      
}
