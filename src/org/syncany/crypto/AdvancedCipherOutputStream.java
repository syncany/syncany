package org.syncany.crypto;

import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

public class AdvancedCipherOutputStream extends OutputStream {
	private static final byte[] STREAM_MAGIC = "Syncany".getBytes();
	private static final byte STREAM_VERSION = 1;
	
	private OutputStream underlyingOutputStream;
	private CipherOutputStream cipherOutputStream;
	private CipherSession cipherSession;
	
	public AdvancedCipherOutputStream(OutputStream out, CipherSession cipherSession) throws IOException {
		this.underlyingOutputStream = out;		
		this.cipherSession = cipherSession;
		
		init();		
	}
		
	private void init() throws IOException {
		try {
			// Write output stream version
			underlyingOutputStream.write(STREAM_MAGIC);
			underlyingOutputStream.write(STREAM_VERSION);
			
			// Write cipher suite ID
			underlyingOutputStream.write(cipherSession.getCipherSuite().getId());
			
			// Create and write session salt to unencrypted stream
			if (cipherSession.getSessionWriteSecretKey() == null || cipherSession.getSessionWriteSalt() == null) {
				cipherSession.setSessionWriteSalt(cipherSession.createSalt());
				cipherSession.setSessionWriteSecretKey(cipherSession.createSecretKey(cipherSession.getSessionWriteSalt()));
			}
			
			underlyingOutputStream.write(cipherSession.getSessionWriteSalt());
			
			// Create and write random IV to unencrypted stream
			byte[] streamIV = null;
			
			if (cipherSession.getCipherSuite().isIv()) {
				streamIV = new byte[cipherSession.getCipherSuite().getIvSize()]; 
				new SecureRandom().nextBytes(streamIV);
			
				underlyingOutputStream.write(streamIV);
			}
			
			// Initialize cipher
			Cipher streamEncryptCipher = cipherSession.createEncCipher(cipherSession.getSessionWriteSecretKey(), streamIV);
	
			// Now create cipher stream and write to encrypted stream
	        cipherOutputStream = new CipherOutputStream(underlyingOutputStream, streamEncryptCipher);	        
    	}
    	catch (Exception e) {
    		throw new IOException(e);
    	}		
	}

	@Override
	public void write(int b) throws IOException {
		cipherOutputStream.write(b);		
	}
	
	@Override
	public void close() throws IOException {
		cipherOutputStream.close();
	}
}
