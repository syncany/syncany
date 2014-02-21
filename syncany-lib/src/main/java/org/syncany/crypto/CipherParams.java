/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.crypto;

import java.security.Provider;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.syncany.util.StringUtil;

/**
 * Defines important crypto constants used in the application.
 * 
 * <p><b>Warning</b>: The class defines constants that (if changed) can lead to 
 * invalidated ciphertext data. Do <b>not change</b> any of these parameters unless 
 * you know what you are doing!
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class CipherParams {
	/**
	 * Defines the name of the cryptography provider. The constant is used
	 * during crypto provider registration, as well as to instantiate cipher
	 * algorithms.
	 * 
	 * @see #CRYPTO_PROVIDER
	 */
	public static final String CRYPTO_PROVIDER_ID = "BC";
	
	/**
	 * Defines the cryptography provider used in the application. The provider
	 * registration is done in the cipher utility class.
	 * 
	 * @see #CRYPTO_PROVIDER_ID
	 */
	public static final Provider CRYPTO_PROVIDER = new BouncyCastleProvider();
	
	/**
	 * Password-based key derivation function used to generate the master key
	 * from the user's password. 
	 * 
	 * <p><b>Warning:</b> Changing this constant may lead to unrecoverable ciphertext data
	 * Do not change this constant unless you know what you are doing! 
	 * 
	 * @see #MASTER_KEY_DERIVATION_ROUNDS
	 * @see #MASTER_KEY_SIZE
	 * @see #MASTER_KEY_SALT_SIZE
	 */
    public static final String MASTER_KEY_DERIVATION_FUNCTION = "PBKDF2WithHmacSHA1";
    
    /**
     * Number of rounds the password-based key derivation function is applied during the
     * master key generation process.
     * 
	 * <p><b>Warning:</b> Changing this constant may lead to unrecoverable ciphertext data
	 * Do not change this constant unless you know what you are doing! 
	 * 
	 * @see #MASTER_KEY_DERIVATION_FUNCTION
	 * @see #MASTER_KEY_SIZE
	 * @see #MASTER_KEY_SALT_SIZE
     */
    public static final int MASTER_KEY_DERIVATION_ROUNDS = 1000000;
    
    /**
     * Size of a generated master key (in bits). This value is used during the key
     * generation by the password-based key derivation function.
     * 
	 * <p><b>Warning:</b> Changing this constant may lead to unrecoverable ciphertext data
	 * Do not change this constant unless you know what you are doing! 
	 * 
	 * @see #MASTER_KEY_DERIVATION_FUNCTION
	 * @see #MASTER_KEY_DERIVATION_ROUNDS
	 * @see #MASTER_KEY_SALT_SIZE
     */
    public static final int MASTER_KEY_SIZE = 512; 	
    
    /**
     * Size of the salt used to generate the master key. This value is used during
     * the key generation by the password-based key derivation function.
     * 
	 * <p><b>Warning:</b> Changing this constant may lead to unrecoverable ciphertext data
	 * Do not change this constant unless you know what you are doing! 
	 * 
	 * @see #MASTER_KEY_DERIVATION_FUNCTION
	 * @see #MASTER_KEY_DERIVATION_ROUNDS
	 * @see #MASTER_KEY_SIZE
     */    
    public static final int MASTER_KEY_SALT_SIZE = 512;
    
    /**
     * Hash function used in the HKDF key derivation algorithm for deriving
     * keys from a master key.
     * 
	 * <p><b>Warning:</b> Changing this constant may lead to unrecoverable ciphertext data
	 * Do not change this constant unless you know what you are doing! 
	 * 
     * @see #KEY_DERIVATION_INFO
     */
    public static final Digest KEY_DERIVATION_DIGEST = new SHA256Digest(); 
    
    /**
     * Additional info used in the HKDF key derivation algorithm.
     *  
	 * <p><b>Warning:</b> Changing this constant may lead to unrecoverable ciphertext data
	 * Do not change this constant unless you know what you are doing! 
	 * 
	 * @see #KEY_DERIVATION_DIGEST
     */
    public static final byte[] KEY_DERIVATION_INFO = StringUtil.toBytesUTF8("Syncany_SHA256_Derivated_Key");        
}
