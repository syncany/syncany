package org.syncany.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;

import org.syncany.util.StringUtil;

/**
 * Implements an output stream that encrypts the underlying output
 * stream using one to many ciphers. 
 * 
 * Format:
 * <pre>
 *    Length           HMAC'd       Description
 *    ----------------------------------------------
 *    04               no          "Sy" 0x02 0x05 (4 bytes)
 *    01               no          Version (1 byte)
 *    12               no          HMAC salt             
 *    01               yes         Cipher count (=n, 1 byte)
 *    
 *    for i := 0..n-1:
 *      01             yes         Cipher spec ID (1 byte)
 *      12             yes         Salt for cipher i (12 bytes)
 *      aa             yes         IV for cipher i (cipher suite specific length, 0..x)
 *      
 *    bb               yes         Ciphertext
 *    cc               no          HMAC (algorithm-specific length, 20 bytes if "HmacSHA1")
 * </pre>
 * 
 * It tries to follow a few Do's and Don'ts, as described at
 * http://blog.cryptographyengineering.com/2011/11/how-not-to-use-symmetric-encryption.html
 * 
 * Encryption and cipher rules
 * - Don't encrypt with ECB mode (throws exception if ECB is used)
 * - Don't re-use your IVs (IVs are never reused)
 * - Don't encrypt your IVs (IVs are prepended)
 * - Authenticate/HMAC your ciphertexts
 */
public class MultiCipherOutputStream_UNUSED extends OutputStream {
	public static final byte[] STREAM_MAGIC = new byte[] {0x53, 0x79, 0x02, 0x05 };
	public static final byte STREAM_VERSION = 1;
	public static final int SALT_SIZE = 12;
	
	static final int BUFFER_SIZE = 4096; 
	static final String HMAC_ALGORITHM = "HmacSHA1";
	static final int HMAC_KEY_SIZE= 256;
	
	private SecureRandom secureRandom;
	
	private OutputStream underlyingOutputStream;
	private List<CipherSpec> cipherSpecs;
	private CipherSession cipherSession;

	private ByteArrayOutputStream unwrittenOutputStream;
	private OutputStream unwrittenCipherOutputStream;
	private boolean headerWritten;	
	
	private HmacAccumulator_UNUSED hmacAccumulator;
	
	public MultiCipherOutputStream_UNUSED(OutputStream out, List<CipherSpec> cipherSpecs, CipherSession cipherSession) throws IOException {
		this.secureRandom = new SecureRandom();
		
		this.underlyingOutputStream = out;		
		this.cipherSpecs = cipherSpecs;		
		this.cipherSession = cipherSession;
		
		this.unwrittenOutputStream = null;
		this.unwrittenCipherOutputStream = null;
		this.headerWritten = false;
		
		this.hmacAccumulator = null;
		
		doSanityChecks();
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (!headerWritten) {
			writeHeader();
			headerWritten = true;
		}
		
		unwrittenCipherOutputStream.write(b, off, len);
		System.out.println("written bytes to byte array OS:  "+StringUtil.toHex(b));
		
		if (unwrittenOutputStream.size() >= BUFFER_SIZE) {
			flushToUnderlyingOutputStream();
		}
	}
	
	@Override
	public void write(int b) throws IOException {
		write(new byte[] { (byte) b });
	}
			
	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}	
	
	@Override
	public void close() throws IOException {
		flushToUnderlyingOutputStream();
		
		byte[] calculatedHmac = hmacAccumulator.getCalculatedMac();
		underlyingOutputStream.write(calculatedHmac);
		System.out.println("written hmac to underlying OS:   "+StringUtil.toHex(calculatedHmac));
		
		unwrittenOutputStream.close();
		unwrittenCipherOutputStream.close();
	}
		
	private void writeHeader() throws IOException {
		try {
			byte[] hmacSalt = createSalt();
			SecretKey hmacSecretKey = CipherUtil.createSecretKey(HMAC_ALGORITHM, HMAC_KEY_SIZE, cipherSession.getPassword(), hmacSalt);
			
			hmacAccumulator = new HmacAccumulator_UNUSED(hmacSecretKey, HMAC_ALGORITHM, CipherUtil.PROVIDER, BUFFER_SIZE);
			unwrittenOutputStream = new ByteArrayOutputStream();
			unwrittenCipherOutputStream = unwrittenOutputStream;
			
			writeNoHmac(underlyingOutputStream, STREAM_MAGIC);
			writeNoHmac(underlyingOutputStream, STREAM_VERSION);
			writeNoHmac(underlyingOutputStream, hmacSalt);
			writeAndUpdateHmac(underlyingOutputStream, cipherSpecs.size());			
			
			for (CipherSpec cipherSpec : cipherSpecs) { 
				byte[] salt = createSalt();		
				byte[] iv = createIV(cipherSpec);

				writeAndUpdateHmac(underlyingOutputStream, cipherSpec.getId());
				writeAndUpdateHmac(underlyingOutputStream, salt);
				writeAndUpdateHmac(underlyingOutputStream, iv);
				
				SecretKey secretKey = CipherUtil.createSecretKey(cipherSpec, cipherSession.getPassword(), salt); 
				Cipher encryptCipher = CipherUtil.createEncCipher(cipherSpec, secretKey, iv);
		
				unwrittenCipherOutputStream = new CipherOutputStream(unwrittenCipherOutputStream, encryptCipher);	        
			}										
    	}
    	catch (Exception e) {
    		throw new IOException(e);
    	}		
	}	
	
	private void writeAndUpdateHmac(OutputStream outputStream, byte[] bytes) throws IOException {
		outputStream.write(bytes);
		hmacAccumulator.encryptUpdate(bytes, bytes.length);
	}

	private void writeAndUpdateHmac(OutputStream outputStream, int abyte) throws IOException {
		outputStream.write(abyte);
		hmacAccumulator.encryptUpdate(abyte);
	}
	
	private void writeNoHmac(OutputStream outputStream, byte[] bytes) throws IOException {
		outputStream.write(bytes);
	}

	private void writeNoHmac(OutputStream outputStream, int abyte) throws IOException {
		outputStream.write(abyte);
	}
	
	private byte[] createIV(CipherSpec cipherSpec) throws IOException {
		byte[] streamIV = new byte[cipherSpec.getIvSize()/8]; 
		secureRandom.nextBytes(streamIV);
		
		return streamIV;
	}		

	private byte[] createSalt() {
		byte[] salt = new byte[SALT_SIZE];    	
    	secureRandom.nextBytes(salt);
    	
    	return salt;
	}

	private void doSanityChecks() throws IOException {
		for (CipherSpec cipherSuite : cipherSpecs) {
			if (cipherSuite.getCipherStr().contains("/ECB/")) {
				throw new IOException("Cannot use ECB mode. This mode is not considered secure.");
			}
		}	 
	}
	
	private void flushToUnderlyingOutputStream() throws IOException {
		byte[] unwrittenEnryptedBytes = unwrittenOutputStream.toByteArray();
		
		if (unwrittenEnryptedBytes.length > 0) {
			System.out.println("flushing bytes to underlying OS: "+StringUtil.toHex(unwrittenEnryptedBytes));
			
			hmacAccumulator.encryptUpdate(unwrittenEnryptedBytes, unwrittenEnryptedBytes.length);
			underlyingOutputStream.write(unwrittenEnryptedBytes, 0, unwrittenEnryptedBytes.length);
			
			unwrittenOutputStream.reset();
		}
	}
}
