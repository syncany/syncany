/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import static org.syncany.crypto.CipherParams.CRYPTO_PROVIDER_ID;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.crypto.Mac;

import org.syncany.crypto.specs.HmacSha256CipherSpec;

/**
 * Implements an output stream that encrypts the underlying output
 * stream using one to many ciphers. 
 * 
 * Format:
 * <pre>
 *    Length           HMAC'd           Description
 *    ----------------------------------------------
 *    04               no               "Sy" 0x02 0x05 (4 bytes)
 *    01               no               Version (1 byte)
 *    12               no               HMAC salt             
 *    01               yes (in header)  Cipher count (=n, 1 byte)
 *    
 *    for i := 0..n-1:
 *      01             yes (in header)  Cipher spec ID (1 byte)
 *      12             yes (in header)  Salt for cipher i (12 bytes)
 *      aa             yes (in header)  IV for cipher i (cipher specific length, 0..x)
 *      
 *    20               no               Header HMAC (20 bytes, for "HmacSHA1")
 *    bb               yes (in mode)    Ciphertext (HMAC'd by mode, e.g. GCM)
 * </pre>
 * 
 * It follows a few Do's and Don'ts:
 * - http://blog.cryptographyengineering.com/2011/11/how-not-to-use-symmetric-encryption.html
 * - http://security.stackexchange.com/questions/30170/after-how-much-data-encryption-aes-256-we-should-change-key
 * 
 * Encryption and cipher rules
 * - Don't encrypt with ECB mode (throws exception if ECB is used)
 * - Don't re-use your IVs (IVs are never reused)
 * - Don't encrypt your IVs (IVs are prepended)
 * - Authenticate cipher configuration (algorithm, salts and IVs)
 * - Only use authenticated ciphers
 */
public class MultiCipherOutputStream extends OutputStream {
	public static final byte[] STREAM_MAGIC = new byte[] { 0x53, 0x79, 0x02, 0x05 };
	public static final byte STREAM_VERSION = 1;

	public static final int SALT_SIZE = 12;	
	public static final CipherSpec HMAC_SPEC = new HmacSha256CipherSpec();
	
	private OutputStream underlyingOutputStream;
	
	private List<CipherSpec> cipherSpecs;
	private CipherSession cipherSession;
	private OutputStream cipherOutputStream;

	private boolean headerWritten;	
	private Mac headerHmac;
	
	public MultiCipherOutputStream(OutputStream out, List<CipherSpec> cipherSpecs, CipherSession cipherSession) throws IOException {
		this.underlyingOutputStream = out;	
		
		this.cipherSpecs = cipherSpecs;		
		this.cipherSession = cipherSession;		
		this.cipherOutputStream = null;
		
		this.headerWritten = false;
		this.headerHmac = null;		
	}
	
	@Override
	public void write(int b) throws IOException {
		writeHeader();
		cipherOutputStream.write(b);		
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		writeHeader();
		cipherOutputStream.write(b, 0, b.length);
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		writeHeader();
		cipherOutputStream.write(b, off, len);
	}
	
	@Override
	public void close() throws IOException {
		cipherOutputStream.close();
	}
		
	private void writeHeader() throws IOException {
		if (!headerWritten) {
			try {
				// Initialize header HMAC
				SaltedSecretKey hmacSecretKey = cipherSession.getWriteSecretKey(HMAC_SPEC);

				headerHmac = Mac.getInstance(HMAC_SPEC.getAlgorithm(), CRYPTO_PROVIDER_ID);
				headerHmac.init(hmacSecretKey);

				// Write header
				writeNoHmac(underlyingOutputStream, STREAM_MAGIC);
				writeNoHmac(underlyingOutputStream, STREAM_VERSION);
				writeNoHmac(underlyingOutputStream, hmacSecretKey.getSalt());			
				writeAndUpdateHmac(underlyingOutputStream, cipherSpecs.size());

				cipherOutputStream = underlyingOutputStream;

				for (CipherSpec cipherSpec : cipherSpecs) { 
					SaltedSecretKey saltedSecretKey = cipherSession.getWriteSecretKey(cipherSpec);				
					byte[] iv = CipherUtil.createRandomArray(cipherSpec.getIvSize()/8);

					writeAndUpdateHmac(underlyingOutputStream, cipherSpec.getId());
					writeAndUpdateHmac(underlyingOutputStream, saltedSecretKey.getSalt());
					writeAndUpdateHmac(underlyingOutputStream, iv);

					cipherOutputStream = cipherSpec.newCipherOutputStream(cipherOutputStream, saltedSecretKey.getEncoded(), iv);	        
				}	

				writeNoHmac(underlyingOutputStream, headerHmac.doFinal());
			}
			catch (Exception e) {
				throw new IOException(e);
			}	
			headerWritten = true;
		}
	}	

	private void writeNoHmac(OutputStream outputStream, byte[] bytes) throws IOException {
		outputStream.write(bytes);
	}

	private void writeNoHmac(OutputStream outputStream, int abyte) throws IOException {
		outputStream.write(abyte);
	}	
	
	private void writeAndUpdateHmac(OutputStream outputStream, byte[] bytes) throws IOException {
		writeNoHmac(outputStream, bytes);
		headerHmac.update(bytes);
	}

	private void writeAndUpdateHmac(OutputStream outputStream, int abyte) throws IOException {
		writeNoHmac(outputStream, abyte);
		headerHmac.update((byte) abyte);
	}	
}