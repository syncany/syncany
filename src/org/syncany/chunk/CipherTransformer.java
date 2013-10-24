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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.syncany.crypto.CipherSession;
import org.syncany.crypto.CipherSuite;
import org.syncany.crypto.CipherSuites;
import org.syncany.crypto.MultiCipherInputStream;
import org.syncany.crypto.MultiCipherOutputStream;

/**
 *
 * @author pheckel
 */
public class CipherTransformer extends Transformer {
	private List<CipherSuite> cipherSuites;
	private CipherSession cipherSession;
	
	public CipherTransformer() {
		this.cipherSuites = new ArrayList<CipherSuite>();
		this.cipherSession = null;
	}
	
    public CipherTransformer(List<CipherSuite> cipherSuites, String password) {
    	this.cipherSuites = cipherSuites;
    	this.cipherSession = new CipherSession(password);
    }    
    
    @Override
    public void init(Map<String, String> settings) throws Exception {
    	String password = settings.get("password");
    	String cipherSuitesListStr = settings.get("ciphersuites");
    	
    	if (password == null || cipherSuitesListStr == null) {
    		throw new Exception("Settings 'ciphersuites' and 'password' must both be filled.");
    	}
    	
    	initCipherSuites(cipherSuitesListStr);
    	initPassword(password);    	
    }
    
    private void initCipherSuites(String cipherSuitesListStr) throws Exception {
    	String[] cipherSuiteIdStrs = cipherSuitesListStr.split(",");
    	
    	for (String cipherSuiteIdStr : cipherSuiteIdStrs) {
    		int cipherSuiteId = Integer.parseInt(cipherSuiteIdStr);
    		CipherSuite cipherSuite = CipherSuites.getCipherSuite(cipherSuiteId);
    		
    		if (cipherSuite == null) {
    			throw new Exception("Cannot find cipher suite with ID '"+cipherSuiteId+"'");
    		}
    		
    		cipherSuites.add(cipherSuite);
    	}
	}

	private void initPassword(String password) {
		cipherSession = new CipherSession(password);
	}

	@Override
	public OutputStream createOutputStream(OutputStream out) throws IOException {
    	return new MultiCipherOutputStream(out, cipherSuites, cipherSession);    	
    }

    @Override
    public InputStream createInputStream(InputStream in) throws IOException {
    	return new MultiCipherInputStream(in, cipherSession);    	
    }    

    @Override
    public String toString() {
        return (nextTransformer == null) ? "Cipher" : "Cipher-"+nextTransformer;
    }     
}
