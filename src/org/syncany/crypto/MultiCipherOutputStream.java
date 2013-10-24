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
 * Format:
 * <pre>
 *    Length          Description
 *    ----------------------------------------------
 *    04              "Sy" 0x02 0x05 (4 bytes)
 *    01              Version (1 byte)
 *    01              Cipher count (=c, 1 byte)
 *    
 *    for i:= 0..c-1:
 *      01            Cipher IDs (c * 1 byte)
 *    
 *    for i:= 0..c-1:
 *      12            Salt (12 bytes)
 *      nn            IV (cipher suite specific length, 0..x)
 *      
 *    (ciphertext)
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
 * - Always MAC your IVs (TODO)
 * 
 * 
 */
public class MultiCipherOutputStream extends OutputStream {
	public static final byte[] STREAM_MAGIC = new byte[] {0x53, 0x79, 0x02, 0x05 };
	public static final byte STREAM_VERSION = 1;
	public static final int SALT_SIZE = 12;
	
	private OutputStream underlyingOutputStream;
	private List<CipherSpec> cipherSuites;
	private CipherSession cipherSession;

	private OutputStream cipherOutputStream;
	private boolean headerWritten;	
	
	public MultiCipherOutputStream(OutputStream out, List<CipherSpec> cipherSuites, CipherSession cipherSession) throws IOException {
		this.underlyingOutputStream = out;		
		this.cipherSuites = cipherSuites;		
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
			
			for (CipherSpec cipherSuite : cipherSuites) { 
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
	
	private byte[] createAndWriteIV(CipherSpec cipherSuite) throws IOException {
		byte[] streamIV = null;
		
		if (cipherSuite.hasIv()) {
			streamIV = new byte[cipherSuite.getIvSize()/8]; 
			new SecureRandom().nextBytes(streamIV);
		
			underlyingOutputStream.write(streamIV);
		}
		
		return streamIV;
	}

	private void doSanityChecks() throws IOException {
		for (CipherSpec cipherSuite : cipherSuites) {
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
		underlyingOutputStream.write(cipherSuites.size());
		
		for (CipherSpec cipherSuite : cipherSuites) {
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
