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

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

/**
 *
 * @author pheckel
 */
public class CipherEncrypter extends Transformer {
    private Cipher encryptCipher;
    private Cipher decryptCipher;
    
    public CipherEncrypter(Cipher encryptCipher, Cipher decryptCipher) {
        this(encryptCipher, decryptCipher, null);
    }
    
    public CipherEncrypter(Cipher encryptCipher, Cipher decryptCipher, Transformer nextTransformer) {
        super(nextTransformer);
        
        this.encryptCipher = encryptCipher;
        this.decryptCipher = decryptCipher;
    }

    public Cipher getEncryptCipher() {
        return encryptCipher;
    }

    public void setEncryptCipher(Cipher cipher) {
        this.encryptCipher = cipher;
    }   
    
    public Cipher getDecryptCipher() {
		return decryptCipher;
	}

	public void setDecryptCipher(Cipher decryptCipher) {
		this.decryptCipher = decryptCipher;
	}

	@Override
    public OutputStream createOutputStream(OutputStream out) throws IOException {
        return new CipherOutputStream(out, encryptCipher);
    }

    @Override
    public InputStream createInputStream(InputStream in) throws IOException {
        return new CipherInputStream(in, decryptCipher);
    }
    
    @Override
    public String toString() {
        return (nextTransformer == null) ? "Cipher" : "Cipher-"+nextTransformer;
    }         
}
