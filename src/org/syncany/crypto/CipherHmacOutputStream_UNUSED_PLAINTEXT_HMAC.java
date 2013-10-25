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
package org.syncany.crypto;

import java.io.IOException;
import java.io.OutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.Mac;

/**
 *
 * @author pheckel
 */
public class CipherHmacOutputStream_UNUSED_PLAINTEXT_HMAC extends CipherOutputStream {
	private Cipher cipher;
	private Mac mac;
	private byte[] iv;
	
	public CipherHmacOutputStream_UNUSED_PLAINTEXT_HMAC(OutputStream os, Cipher c, Mac m, byte[] iv) {
		super(os, c);
		cipher = c;
		mac = m;
	}

	@Override
	public void write(int b) throws IOException {
		mac.update((byte) b);
		super.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		mac.update(b);
		super.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		mac.update(b, off, len);
		super.write(b, off, len);
	}

	@Override
	public void close() throws IOException {
		writeHmac();		
		super.close();
	}
	
	private void writeHmac() throws IOException {
		// Add IV & algorithm details
		try {
			mac.update(iv);
			mac.update(cipher.getAlgorithm().getBytes("UTF8"));
			mac.update(cipher.getParameters().getEncoded());
			
			byte[] macBytes = mac.doFinal();
			super.write(macBytes);
		}
		catch (Exception e) {
			throw new IOException(e);
		}		
	}
}
