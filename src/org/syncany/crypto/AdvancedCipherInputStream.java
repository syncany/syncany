package org.syncany.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class AdvancedCipherInputStream extends InputStream {
	private static final byte[] STREAM_MAGIC = "Syncany".getBytes();
	private static final byte SUPPORTED_STREAM_VERSION = 1;
	
	private InputStream underlyingInputStream;
	private GcmCompatibleCipherInputStream cipherInputStream;
	private CipherSession cipherSession;
	
	public AdvancedCipherInputStream(InputStream in, CipherSession cipherSession) throws IOException {
		this.underlyingInputStream = in;		
		this.cipherSession = cipherSession;
		
		init();		
	}
	
	private void init() throws IOException {
		try {
			// Read magic
			byte[] streamMagic = new byte[STREAM_MAGIC.length];
			underlyingInputStream.read(streamMagic);
			
			if (!Arrays.equals(STREAM_MAGIC, streamMagic)) {
				throw new IOException("Not a Syncany-encrypted file, no magic!");
			}
			
			// Read version
			byte streamVersion = (byte) underlyingInputStream.read();
			
			if (streamVersion != SUPPORTED_STREAM_VERSION) {
				throw new IOException("Stream version not supported: "+streamVersion);
			}
			
			// Read cipher suite ID
			int cipherSuiteId = underlyingInputStream.read();
			
			if (cipherSession.getCipherSuite() == null) {
				CipherSuite cipherSuite = CipherSuites.getCipherSuite(cipherSuiteId);
				
				if (cipherSuite == null) {
					throw new IOException("Cannot find cipher suite with ID "+cipherSuiteId);
				}
				
				cipherSession.setCipherSuite(cipherSuite);
			}
						
	    	// Read salt from unencrypted stream
	    	byte[] streamSalt = new byte[cipherSession.getCipherSuite().getKeySize()]; 
	    	underlyingInputStream.read(streamSalt);
	    	
			// Read IV from unencrypted stream
			byte[] streamIV = null;
			
			if (cipherSession.getCipherSuite().isIv()) {
				streamIV = new byte[cipherSession.getCipherSuite().getIvSize()];		
				underlyingInputStream.read(streamIV);
			}
			
			// Create key
			SecretKey streamKey = null;
			
			if (cipherSession.getLastReadSalt() != null && Arrays.equals(cipherSession.getLastReadSalt(), streamSalt)) {
				streamKey = cipherSession.getLastReadSecretKey();
			}
			else {
				streamKey = cipherSession.createSecretKey(streamSalt);
				
				cipherSession.setLastReadSalt(streamSalt);
				cipherSession.setLastReadSecretKey(streamKey);
			}
			
			// Initialize cipher
			Cipher streamDecryptCipher = cipherSession.createDecCipher(streamKey, streamIV);
	
			// Now create cipher stream and write to encrypted stream
			cipherInputStream = new GcmCompatibleCipherInputStream(underlyingInputStream, streamDecryptCipher);		
    	}
    	catch (Exception e) {
    		throw new IOException(e);
    	}
	}

	@Override
	public int read() throws IOException {
		return cipherInputStream.read();
	}
	
	@Override
	public void close() throws IOException {
		cipherInputStream.close();
	}	
}
