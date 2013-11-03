package org.syncany.crypto;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

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
 * It tries to follow a few Do's and Don'ts, as described at
 * http://blog.cryptographyengineering.com/2011/11/how-not-to-use-symmetric-encryption.html
 * 
 * Encryption and cipher rules
 * - Don't encrypt with ECB mode (throws exception if ECB is used)
 * - Don't re-use your IVs (IVs are never reused)
 * - Don't encrypt your IVs (IVs are prepended)
 * - Authenticate cipher configuration (algorithm, salts and IVs)
 * - Only use authenticated ciphers
 */
public class MultiCipherOutputStream extends OutputStream {
	public static final byte[] STREAM_MAGIC = new byte[] {0x53, 0x79, 0x02, 0x05 };
	public static final byte STREAM_VERSION = 1;
	public static final int SALT_SIZE = 12;
	
	static final String HMAC_ALGORITHM = "HmacSHA1";
	static final int HMAC_KEY_SIZE = 256;	
	
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
		
		doSanityChecks();
	}
	
	@Override
	public void write(int b) throws IOException {
		if (!headerWritten) {
			writeHeader();
			headerWritten = true;
		}
		
		cipherOutputStream.write(b);		
	}
	
	@Override
	public void close() throws IOException {
		cipherOutputStream.close();
	}
	
	private void doSanityChecks() throws IOException {
		for (CipherSpec cipherSpec : cipherSpecs) {
			if (cipherSpec.getCipherStr().matches("/(ECB|CBC|DES|DESde)/")) {
				throw new IOException("Cipher algorithm or mode not allowed: "+cipherSpec.getCipherStr()+". This mode is not considered secure.");
			}
		}	 
	}
		
	private void writeHeader() throws IOException {
		try {
			// Initialize header HMAC
			byte[] hmacSalt = CipherUtil.createRandomArray(SALT_SIZE);
			SecretKey hmacSecretKey = CipherUtil.createSecretKey(HMAC_ALGORITHM, HMAC_KEY_SIZE, cipherSession.getPassword(), hmacSalt);
			
			headerHmac = Mac.getInstance(HMAC_ALGORITHM, CipherUtil.PROVIDER);
			headerHmac.init(hmacSecretKey);
			
			// Write header
			writeNoHmac(underlyingOutputStream, STREAM_MAGIC);
			writeNoHmac(underlyingOutputStream, STREAM_VERSION);
			writeNoHmac(underlyingOutputStream, hmacSalt);			
			writeAndUpdateHmac(underlyingOutputStream, cipherSpecs.size());
			
			cipherOutputStream = underlyingOutputStream;
			
			for (CipherSpec cipherSpec : cipherSpecs) { 
				byte[] salt = CipherUtil.createRandomArray(SALT_SIZE);		
				byte[] iv = CipherUtil.createRandomArray(cipherSpec.getIvSize()/8);

				writeAndUpdateHmac(underlyingOutputStream, cipherSpec.getId());
				writeAndUpdateHmac(underlyingOutputStream, salt);
				writeAndUpdateHmac(underlyingOutputStream, iv);
				
				SecretKey secretKey = CipherUtil.createSecretKey(cipherSpec, cipherSession.getPassword(), salt); 
				Cipher encryptCipher = CipherUtil.createEncCipher(cipherSpec, secretKey, iv);
				
				cipherOutputStream = new CipherOutputStream(cipherOutputStream, encryptCipher);	        
			}	
			
			writeNoHmac(underlyingOutputStream, headerHmac.doFinal());
    	}
    	catch (Exception e) {
    		throw new IOException(e);
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