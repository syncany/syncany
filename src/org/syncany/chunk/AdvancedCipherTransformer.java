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

import org.syncany.crypto.AdvancedCipherInputStream;
import org.syncany.crypto.AdvancedCipherOutputStream;
import org.syncany.crypto.CipherSession;
import org.syncany.crypto.CipherSuite;

/**
 *
 * @author pheckel
 */
public class AdvancedCipherTransformer extends Transformer {
	private CipherSession cipherSession;
	
    public AdvancedCipherTransformer(CipherSuite cipherSuite, String password) {
    	this.cipherSession = new CipherSession(cipherSuite, password);
    }   
    
    @Override
    public String toString() {
        return (nextTransformer == null) ? "Cipher" : "Cipher-"+nextTransformer;
    }     
    
    @Override
	public OutputStream createOutputStream(OutputStream out) throws IOException {
    	return new AdvancedCipherOutputStream(out, cipherSession);    	
    }

    @Override
    public InputStream createInputStream(InputStream in) throws IOException {
    	return new AdvancedCipherInputStream(in, cipherSession);    	
    }
}
