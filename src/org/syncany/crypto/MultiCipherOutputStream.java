package org.syncany.crypto;

import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;

/**
 * Implements an output stream that encrypts the underlying output
 * stream using one to many ciphers. 
 * 
 * Format: // TODO [high] Implement this format
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
 *      aa             yes (in header)  IV for cipher i (cipher suite specific length, 0..x)
 *      
 *    20               no               Header HMAC (20 bytes, for "HmacSHA1")
 *    bb               yes (in mode)    Ciphertext (HMAC'd by mode, e.g. GCM)
 * </pre>
 * 
 * TODO [medium] HMAC over IV, Cipher Suites and Ciphertext
 * 
 * It tries to follow a few Do's and Don'ts, as described at
 * http://blog.cryptographyengineering.com/2011/11/how-not-to-use-symmetric-encryption.html
 * 
 * Encryption and cipher rules
 * - Don't encrypt with ECB mode (throws exception if ECB is used)
 * - Don't re-use your IVs (IVs are never reused)
 * - Don't encrypt your IVs (IVs are prepended)
 * - Authenticate/HMAC your ciphertexts (TODO [low] Not necessary if GCM, CCM, OCB or EAX mode is used)
 * -  TODO [medium] Always MAC your IVs 
 * 
 * 
 */
public class MultiCipherOutputStream extends OutputStream {
	public static final byte[] STREAM_MAGIC = new byte[] {0x53, 0x79, 0x02, 0x05 };
	public static final byte STREAM_VERSION = 1;
	public static final int SALT_SIZE = 12;
	
	static final String HMAC_ALGORITHM = "HmacSHA1";
	static final int HMAC_KEY_SIZE= 256;	
	
	private OutputStream underlyingOutputStream;
	private List<CipherSpec> cipherSpecs;
	private CipherSession cipherSession;

	private OutputStream cipherOutputStream;
	private boolean headerWritten;	
	
	public MultiCipherOutputStream(OutputStream out, List<CipherSpec> cipherSpecs, CipherSession cipherSession) throws IOException {
		this.underlyingOutputStream = out;		
		this.cipherSpecs = cipherSpecs;		
		this.cipherSession = cipherSession;
		
		this.cipherOutputStream = null;
		this.headerWritten = false;
		
		doSanityChecks();
	}
		
	private void writeHeader() throws IOException {
		try {
			writeMagic();
			writeVersion();
			writeCipherSuites();
			
			cipherOutputStream = underlyingOutputStream;
			
			for (CipherSpec cipherSuite : cipherSpecs) { 
				byte[] salt = createAndWriteSalt();		
				byte[] iv = createAndWriteIV(cipherSuite);
				
				SecretKey secretKey = CipherUtil.createSecretKey(cipherSuite, cipherSession.getPassword(), salt); 
				Cipher encryptCipher = CipherUtil.createEncCipher(cipherSuite, secretKey, iv);
		
				cipherOutputStream = new CipherOutputStream(cipherOutputStream, encryptCipher);	        
			}				       
    	}
    	catch (Exception e) {
    		throw new IOException(e);
    	}		
	}	
	
	private byte[] createAndWriteIV(CipherSpec cipherSpec) throws IOException {
		byte[] streamIV = null;
		
		if (cipherSpec.hasIv()) {
			streamIV = new byte[cipherSpec.getIvSize()/8]; 
			new SecureRandom().nextBytes(streamIV);
		
			underlyingOutputStream.write(streamIV);
		}
		
		return streamIV;
	}

	private void doSanityChecks() throws IOException {
		for (CipherSpec cipherSuite : cipherSpecs) {
			if (cipherSuite.getCipherStr().contains("/ECB/")) {
				throw new IOException("Cannot use ECB mode. This mode is not considered secure.");
			}
		}	 
	}

	private void writeMagic() throws IOException {
		underlyingOutputStream.write(STREAM_MAGIC);
	}
	
	private void writeVersion() throws IOException {
		underlyingOutputStream.write(STREAM_VERSION);		
	}

	private void writeCipherSuites() throws IOException {
		underlyingOutputStream.write(cipherSpecs.size());
		
		for (CipherSpec cipherSuite : cipherSpecs) {
			underlyingOutputStream.write(cipherSuite.getId());
		}		
	}

	private byte[] createAndWriteSalt() throws IOException {
		byte[] salt = new byte[SALT_SIZE];    	
    	new SecureRandom().nextBytes(salt);
    	
		underlyingOutputStream.write(salt);
		
		return salt;
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
}