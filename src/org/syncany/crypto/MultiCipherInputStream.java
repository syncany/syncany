package org.syncany.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class MultiCipherInputStream extends InputStream {
	private InputStream underlyingInputStream;
	private InputStream cipherInputStream;
	private CipherSession cipherSession;
	
	private boolean headerRead;
	private List<CipherSuite> cipherSuites;
	
	public MultiCipherInputStream(InputStream in, CipherSession cipherSession) throws IOException {
		this.underlyingInputStream = in;		
		this.cipherSession = cipherSession;
		
		this.headerRead = false;		
		this.cipherSuites = new ArrayList<CipherSuite>();
	}
	
	private void readHeader() throws IOException {
		try {
			readMagic();
			readVersion();			
			readCipherSuites();
			
			cipherInputStream = underlyingInputStream;
			
			for (CipherSuite cipherSuite : cipherSuites) { 
				byte[] salt = readSalt();
				byte[] iv = readIV(cipherSuite);
				
				SecretKey secretKey = CipherUtil.createSecretKey(cipherSuite, cipherSession.getPassword(), salt); 
				Cipher decryptCipher = CipherUtil.createDecCipher(cipherSuite, secretKey, iv);
				
				cipherInputStream = new GcmCompatibleCipherInputStream(cipherInputStream, decryptCipher);		
			}	    	
    	}
    	catch (Exception e) {
    		throw new IOException(e);
    	}
	}

	private byte[] readIV(CipherSuite cipherSuite) throws IOException {
		byte[] iv = null;
		
		if (cipherSuite.hasIv()) {
			iv = new byte[cipherSuite.getIvSize()/8];		
			underlyingInputStream.read(iv);
		}
		
		return iv;		
	}

	private void readMagic() throws IOException {
		byte[] streamMagic = new byte[MultiCipherOutputStream.STREAM_MAGIC.length];
		underlyingInputStream.read(streamMagic);
		
		if (!Arrays.equals(MultiCipherOutputStream.STREAM_MAGIC, streamMagic)) {
			throw new IOException("Not a Syncany-encrypted file, no magic!");
		}
	}

	private void readVersion() throws IOException {
		byte streamVersion = (byte) underlyingInputStream.read();
		
		if (streamVersion != MultiCipherOutputStream.STREAM_VERSION) {
			throw new IOException("Stream version not supported: "+streamVersion);
		}		
	}

	private void readCipherSuites() throws IOException {
		int cipherSuiteCount = underlyingInputStream.read();
		
		for (int i=0; i<cipherSuiteCount; i++) {
			int cipherSuiteId = underlyingInputStream.read();
			
			CipherSuite cipherSuite = CipherSuites.getCipherSuite(cipherSuiteId);
			
			if (cipherSuite == null) {
				throw new IOException("Cannot find cipher suite with ID "+cipherSuiteId);
			}
			
			cipherSuites.add(cipherSuite);
		}					
	}
	
	private byte[] readSalt() throws IOException {
		byte[] salt = new byte[MultiCipherOutputStream.SALT_SIZE]; 
    	underlyingInputStream.read(salt);
    	
    	return salt;
	}
	
	@Override
	public int read() throws IOException {
		if (!headerRead) {
			readHeader();		
			headerRead = true;
		}
		
		return cipherInputStream.read();
	}
	
	@Override
	public void close() throws IOException {
		cipherInputStream.close();
	}	
}
