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

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.syncany.Constants;
import org.syncany.exceptions.EncryptionException;

public class Encryption {	
	
	private String password;
    private String cipherStr;
    private String salt;
    private Integer keylength;

    private Cipher encCipher;
    private Cipher decCipher;

    public Encryption() {
        this("", Constants.DEFAULT_ENCRYPTION_CIPHER, Constants.DEFAULT_ENCRYPTION_KEYLENGTH); // default.
    }

    public Encryption(String password, String cipherStr, int keylength) {
        // All set by init()
        this.password = password;
        this.cipherStr = cipherStr;
        this.keylength = keylength;

        encCipher = null;
        decCipher = null;
    }
 
    public Cipher createEncCipher(byte[] salt) throws EncryptionException {
        try {        
            SecretKeySpec keySpec = createKeySpec(salt);

            Cipher cipher = Cipher.getInstance(cipherStr);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);        

            return cipher;
        }
        catch (Exception e) {
            throw new EncryptionException(e);
        }
    }    
    
    public Cipher createEncCipher(String salt) throws EncryptionException {
        try {
            return createEncCipher(salt.getBytes("UTF-8"));
        } 
        catch (UnsupportedEncodingException ex) {
            throw new EncryptionException(ex);
        }
    }    
    
    public Cipher createDecCipher(byte[] salt) throws EncryptionException {
        try {
            SecretKeySpec keySpec = createKeySpec(salt);

            Cipher cipher = Cipher.getInstance(cipherStr);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);        

            return cipher;
        }
        catch (Exception e) {
            throw new EncryptionException(e);
        }
    }    
    
    public Cipher createDecCipher(String salt) throws EncryptionException {
        try {
            return createDecCipher(salt.getBytes("UTF-8"));
        } 
        catch (UnsupportedEncodingException ex) {
            throw new EncryptionException(ex);
        }
    }    

    /**
     * unsalted.
     * @return 
     */
    public Cipher getEncCipher() {
        return encCipher;
    }
    
    /**
     * unsalted.
     * @return 
     */
    public Cipher getDecCipher() {
        return decCipher;
    }                

    public String getCipherStr() {
        return cipherStr;
    }

    public void setCipherStr(String cipherStr) {
        this.cipherStr = cipherStr;
    }

    public Integer getKeylength() {
        return keylength;
    }

    public void setKeylength(Integer keylength) {
        this.keylength = keylength;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) throws EncryptionException {
    	encCipher = createEncCipher(salt);
    	decCipher = createDecCipher(salt);
        this.salt = salt;
    }

    /**
     * unsalted.
     */
    public synchronized byte[] encrypt(byte[] data) throws EncryptionException {
        if (encCipher == null) {
            encCipher = createEncCipher((byte[]) null); // unsalted.
        }
        try {
            return encCipher.doFinal(data);
        }
        catch (Exception ex) {
            throw new EncryptionException(ex);
        }
    }

    /**
     * unsalted.
     */
    public synchronized byte[] decrypt(byte[] data) throws EncryptionException {
        if (decCipher == null) {
            decCipher = createDecCipher((byte[]) null); // unsalted.
        }
        try {
            return decCipher.doFinal(data);
        }
        catch (Exception ex) {
            throw new EncryptionException(ex);
        }
    }
    
    private SecretKeySpec createKeySpec(byte[] salt) 
            throws NoSuchAlgorithmException, UnsupportedEncodingException, EncryptionException {
        
        if (keylength % 8 != 0) {
            throw new EncryptionException("Invalid keylength. Must be divisible by 8.");
        }
        
        // Created salted password        
        byte[] saltedpass;
        
        if (salt == null) {
            saltedpass =  password.getBytes("UTF-8");
        }
        else {
            byte[] pass = password.getBytes("UTF-8");
            saltedpass = new byte[pass.length+salt.length];
            
            System.arraycopy(salt, 0, saltedpass, 0, salt.length);
            System.arraycopy(pass, 0, saltedpass, salt.length, pass.length);
        }
                
        // Create key by hashing the password (+ the salt)
        byte[] key = new byte[keylength/8];

        MessageDigest msgDigest = MessageDigest.getInstance("SHA-256");
        msgDigest.reset();

        byte[] longkey = msgDigest.digest(saltedpass);

        if (longkey.length == key.length) {
            key = longkey;
        }

        else if (longkey.length > key.length) {
            System.arraycopy(longkey, 0, key, 0, key.length);
        }

        else if (longkey.length < key.length) {
            throw new RuntimeException("Invalid key length '"+keylength+"' bit; max 256 bit supported.");
        }

        //System.out.println("key = "+Arrays.toString(key));
        return new SecretKeySpec(key, cipherStr); // AES -> 128/192 bit
        //return new SecretKeySpec(key, "AES"); // AES -> 128/192 bit
    }    
}
