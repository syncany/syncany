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

import static org.syncany.crypto.CipherParams.CRYPTO_PROVIDER_ID;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

public class MultiCipherInputStream extends InputStream {
	private InputStream underlyingInputStream;

	private InputStream cipherInputStream;
	private CipherSession cipherSession;
	
	private boolean headerRead;
	private Mac headerHmac;
		
	public MultiCipherInputStream(InputStream in, CipherSession cipherSession) throws IOException {
		this.underlyingInputStream = in;		

		this.cipherInputStream = null;
		this.cipherSession = cipherSession;
		
		this.headerRead = false;		
		this.headerHmac = null;		
	}

	@Override
	public int read() throws IOException {
		readHeader();
		return cipherInputStream.read();
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		readHeader();
		return cipherInputStream.read(b, 0, b.length);
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		readHeader();
		return cipherInputStream.read(b, off, len);
	}
	
	@Override
	public void close() throws IOException {
		cipherInputStream.close();
	}	
	
	private void readHeader() throws IOException {
		if (!headerRead) {
			try {
				readAndVerifyMagicNoHmac(underlyingInputStream);
				readAndVerifyVersionNoHmac(underlyingInputStream);

				headerHmac = readHmacSaltAndInitHmac(underlyingInputStream, cipherSession);				
				cipherInputStream = readCipherSpecsAndUpdateHmac(underlyingInputStream, headerHmac, cipherSession);

				readAndVerifyHmac(underlyingInputStream, headerHmac);			
			}
			catch (Exception e) {
				throw new IOException(e);
			}
			
			headerRead = true;
		}
	}

	private void readAndVerifyMagicNoHmac(InputStream inputStream) throws IOException {
		byte[] streamMagic = new byte[MultiCipherOutputStream.STREAM_MAGIC.length];
		inputStream.read(streamMagic);
		
		if (!Arrays.equals(MultiCipherOutputStream.STREAM_MAGIC, streamMagic)) {
			throw new IOException("Not a Syncany-encrypted file, no magic!");
		}
	}

	private void readAndVerifyVersionNoHmac(InputStream inputStream) throws IOException {
		byte streamVersion = (byte) inputStream.read();
		
		if (streamVersion != MultiCipherOutputStream.STREAM_VERSION) {
			throw new IOException("Stream version not supported: "+streamVersion);
		}		
	}
	
	private Mac readHmacSaltAndInitHmac(InputStream inputStream, CipherSession cipherSession) throws Exception {
		byte[] hmacSalt = readNoHmac(inputStream, MultiCipherOutputStream.SALT_SIZE);
		SecretKey hmacSecretKey = cipherSession.getReadSecretKey(MultiCipherOutputStream.HMAC_SPEC, hmacSalt);
		
		Mac hmac = Mac.getInstance(MultiCipherOutputStream.HMAC_SPEC.getAlgorithm(), CRYPTO_PROVIDER_ID);
		hmac.init(hmacSecretKey);	
		
		return hmac;
	}
	
	private InputStream readCipherSpecsAndUpdateHmac(InputStream underlyingInputStream, Mac hmac, CipherSession cipherSession) throws Exception {
		int cipherSpecCount = readByteAndUpdateHmac(underlyingInputStream, hmac);		
		InputStream nestedCipherInputStream = underlyingInputStream;
		
		for (int i=0; i<cipherSpecCount; i++) {
			int cipherSpecId = readByteAndUpdateHmac(underlyingInputStream, hmac);				
			CipherSpec cipherSpec = CipherSpecs.getCipherSpec(cipherSpecId);
			
			if (cipherSpec == null) {
				throw new IOException("Cannot find cipher spec with ID "+cipherSpecId);
			}

			byte[] salt = readAndUpdateHmac(underlyingInputStream, MultiCipherOutputStream.SALT_SIZE, hmac);
			byte[] iv = readAndUpdateHmac(underlyingInputStream, cipherSpec.getIvSize()/8, hmac);
			
			SecretKey secretKey = cipherSession.getReadSecretKey(cipherSpec, salt);			
			nestedCipherInputStream = cipherSpec.newCipherInputStream(nestedCipherInputStream, secretKey.getEncoded(), iv);		
		}	 
		
		return nestedCipherInputStream;
	}

	private void readAndVerifyHmac(InputStream inputStream, Mac hmac) throws Exception {
		byte[] calculatedHeaderHmac = hmac.doFinal();
		byte[] readHeaderHmac = readNoHmac(inputStream, calculatedHeaderHmac.length);
		
		if (!Arrays.equals(calculatedHeaderHmac, readHeaderHmac)) {
			throw new Exception("Integrity exception: Calculated HMAC and read HMAC do not match.");
		}			
	}

	private byte[] readNoHmac(InputStream inputStream, int size) throws IOException {
		byte[] bytes = new byte[size];		
		inputStream.read(bytes);	
		
		return bytes;
	}

	private byte[] readAndUpdateHmac(InputStream inputStream, int size, Mac hmac) throws IOException {
		byte[] bytes = readNoHmac(inputStream, size);		
		hmac.update(bytes);
		
		return bytes;
	}

	private int readByteAndUpdateHmac(InputStream inputStream, Mac hmac) throws IOException {
		int abyte = inputStream.read();
		hmac.update((byte) abyte);
		
		return abyte;
	}
}