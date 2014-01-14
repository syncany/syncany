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
package org.syncany.chunk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import org.syncany.crypto.CipherSession;
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherSpecs;
import org.syncany.crypto.MultiCipherInputStream;
import org.syncany.crypto.MultiCipherOutputStream;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.util.StringUtil;

/**
 * The CipherTransformer can be used to encrypt/decrypt files (typically 
 * {@link MultiChunk}s) using the {@link MultiCipherOutputStream} and
 * {@link MultiCipherInputStream}. 
 * 
 * A CipherTransformer requires a list of {@link CipherSpec}s and the master 
 * key. It can be instantiated using a property list (from a config file) or
 * by passing the dependencies to the constructor.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class CipherTransformer extends Transformer {
	public static final String TYPE = "cipher";
	public static final String PROPERTY_CIPHER_SPECS = "cipherspecs";
	public static final String PROPERTY_MASTER_KEY = "masterkey";
	public static final String PROPERTY_MASTER_KEY_SALT = "mastersalt";
	
	private List<CipherSpec> cipherSpecs;
	private CipherSession cipherSession;
	
	public CipherTransformer() {
		this.cipherSpecs = new ArrayList<CipherSpec>();
		this.cipherSession = null;
	}
	
    public CipherTransformer(List<CipherSpec> cipherSpecs, SaltedSecretKey masterKey) {
    	this.cipherSpecs = cipherSpecs;
    	this.cipherSession = new CipherSession(masterKey);
    }    
    
    /**
     * Initializes the cipher transformer using a settings map. Required settings
     * are: {@link #PROPERTY_CIPHER_SPECS}, {@link #PROPERTY_MASTER_KEY} and 
     * {@link #PROPERTY_MASTER_KEY_SALT}.
     */
    @Override
    public void init(Map<String, String> settings) throws Exception {
    	String masterKeyStr = settings.get(PROPERTY_MASTER_KEY);
    	String masterKeySaltStr = settings.get(PROPERTY_MASTER_KEY);
    	String cipherSpecsListStr = settings.get(PROPERTY_CIPHER_SPECS);
    	
    	if (masterKeyStr == null || masterKeySaltStr == null || cipherSpecsListStr == null) {
    		throw new Exception("Settings '"+PROPERTY_CIPHER_SPECS+"', '"+PROPERTY_MASTER_KEY+"' and '"+PROPERTY_MASTER_KEY_SALT+"' must both be filled.");
    	}
    	
    	initCipherSpecs(cipherSpecsListStr);
    	initCipherSession(masterKeyStr, masterKeySaltStr);    	
    }
    
    private void initCipherSpecs(String cipherSpecListStr) throws Exception {
    	String[] cipherSpecIdStrs = cipherSpecListStr.split(",");
    	
    	for (String cipherSpecIdStr : cipherSpecIdStrs) {
    		int cipherSpecId = Integer.parseInt(cipherSpecIdStr);
    		CipherSpec cipherSpec = CipherSpecs.getCipherSpec(cipherSpecId);
    		
    		if (cipherSpec == null) {
    			throw new Exception("Cannot find cipher suite with ID '"+cipherSpecId+"'");
    		}
    		
    		cipherSpecs.add(cipherSpec);
    	}
	}

	private void initCipherSession(String masterKeyStr, String masterKeySaltStr) {
		byte[] masterKeySalt = StringUtil.fromHex(masterKeySaltStr);
		byte[] masterKeyBytes = StringUtil.fromHex(masterKeyStr);
		
		SaltedSecretKey masterKey = new SaltedSecretKey(new SecretKeySpec(masterKeyBytes, "RAW"), masterKeySalt);		
		cipherSession = new CipherSession(masterKey);
	}

	@Override
	public OutputStream createOutputStream(OutputStream out) throws IOException {
		if (cipherSession == null) {
			throw new RuntimeException("Cipher session is not initialized. Call init() before!");
		}
		
    	return new MultiCipherOutputStream(out, cipherSpecs, cipherSession);    	
    }

    @Override
    public InputStream createInputStream(InputStream in) throws IOException {
		if (cipherSession == null) {
			throw new RuntimeException("Cipher session is not initialized. Call init() before!");
		}
		
    	return new MultiCipherInputStream(in, cipherSession);    	
    }    

    @Override
    public String toString() {
        return (nextTransformer == null) ? "Cipher" : "Cipher-"+nextTransformer;
    }     
}
