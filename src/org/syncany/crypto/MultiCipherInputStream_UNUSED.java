package org.syncany.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.syncany.util.StringUtil;

/**
 * header: underlyingInputStream 
 * 
 * read: 
 * 1. enc bytes = read underlyingInputStream
 * 2. hmacUpdate(enc bytes)
 * 3. write enc bytes to pipedOutputStream
 * 4. read  
 * 
 * 
 */
public class MultiCipherInputStream_UNUSED extends InputStream {
	private static final int BUFFER_SIZE = 8192; // Must be larger than any cipher block size!
	
	private InputStream underlyingInputStream;
	private InputStream cipherInputStream;
	private CipherSession cipherSession;
	
	private byte[] readBuffer;
	private int readBufferPosition;
	private int readBufferSize;
	private int readBufferRemaining;
	private boolean readBufferEnded;
	
	private byte[] readHmacBytes;
	
	private boolean headerRead;
	private List<CipherSpec> cipherSpecs;
	
	private Mac hmac;
	private int hmacLength;

	private PipedOutputStream pipedOutputStream;
	private PipedInputStream pipedInputStream;
		
	public MultiCipherInputStream_UNUSED(InputStream in, CipherSession cipherSession) throws IOException {
		this.underlyingInputStream = in;		
		this.cipherSession = cipherSession;
		
		this.readBuffer = new byte[BUFFER_SIZE];
		this.readBufferPosition = 0;
		this.readBufferSize = 0;
		this.readBufferRemaining = 0;
		this.readHmacBytes = null;
		this.readBufferEnded = false;
		
		this.headerRead = false;		
		this.cipherSpecs = new ArrayList<CipherSpec>();
		
		this.hmac = null;
		this.hmacLength = -1;
		
		this.pipedOutputStream = null;
		this.pipedInputStream = null;
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (!headerRead) {
			readHeader();		
			headerRead = true;
		}		
		
		if (!readBufferEnded && readBufferRemaining < len) {
			fillReadBuffer();
		}
		
		if (len > readBufferRemaining) {
			len = readBufferRemaining;			
		}		
		
		if (readBufferRemaining == 0) {
			return -1;
		}
		
		System.out.println("r "+len);
		int read = cipherInputStream.read(b, off, len);
		
		readBufferPosition += read;
		readBufferRemaining -= read;
		
		System.out.println("   -> read from cipher IS = "+read+", remain = "+readBufferRemaining+", pos = "+readBufferPosition+", b = "+StringUtil.toHex(b));
		
		if (!readBufferEnded && read < len) {
			fillReadBuffer();
		}
		
		return read;
	}
	
	private void fillReadBuffer() throws IOException {
		int encryptedBytesWantToRead = BUFFER_SIZE - readBufferRemaining;
		
		byte[] encryptedBytes = new byte[encryptedBytesWantToRead];
		int encryptedBytesActuallyRead = underlyingInputStream.read(encryptedBytes);
		System.out.println("fill read buffer, wanted to read = "+encryptedBytesWantToRead+", actually read = "+encryptedBytesActuallyRead);		
		
		if (encryptedBytesActuallyRead < encryptedBytesWantToRead) {
			readBufferEnded = true;				
			readHmacBytes = new byte[hmacLength];
					
			// Read buffer    | Encrypted bytes
			// aa bb cc dd ee | ff 11 22 33 44 55
			// <-------- hmacLength ------------>
			//                  <-encBytesRead-->

			if (encryptedBytesActuallyRead >= hmacLength) {
				int positionOfHmacBytesInEncryptedBytes = encryptedBytesActuallyRead-hmacLength;

				System.arraycopy(encryptedBytes, positionOfHmacBytesInEncryptedBytes, readHmacBytes, 0, hmacLength);
			}
			else {
				int numberOfHmacBytesInEncryptedBytes = encryptedBytesActuallyRead;
				int positionOfHmacBytesInEncryptedBytes = 0;

				int numberOfHmacBytesInReadBuffer = hmacLength-numberOfHmacBytesInEncryptedBytes;
				int positionOfHmacBytesInReadBuffer= readBufferSize-numberOfHmacBytesInReadBuffer;
				
				System.arraycopy(readBuffer, positionOfHmacBytesInReadBuffer, readHmacBytes, 0, numberOfHmacBytesInReadBuffer);
				System.arraycopy(encryptedBytes, positionOfHmacBytesInEncryptedBytes, readHmacBytes, numberOfHmacBytesInReadBuffer+1, numberOfHmacBytesInEncryptedBytes);
			}	
			
			System.out.println("read hmac ===> "+StringUtil.toHex(readHmacBytes));
		}
		else {
			
		}
		
		hmac.update(encryptedBytes, 0, encryptedBytesActuallyRead);
				
		byte[] newReadBuffer = new byte[BUFFER_SIZE];		
		System.arraycopy(readBuffer, readBufferPosition, newReadBuffer, 0, readBufferRemaining);
		System.arraycopy(encryptedBytes, 0, newReadBuffer, readBufferPosition, encryptedBytesActuallyRead);
		
		readBufferSize = BUFFER_SIZE;
		readBufferPosition = 0;
		readBufferRemaining = readBufferRemaining + encryptedBytesActuallyRead;
		readBuffer = newReadBuffer;
		
		pipedOutputStream.write(encryptedBytes, 0, encryptedBytesActuallyRead);				
				
		System.out.println("encrypted bytes = "+StringUtil.toHex(encryptedBytes));		
		System.out.println("new read buffer = "+StringUtil.toHex(readBuffer));			
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read() throws IOException {
		byte[] singleByte = new byte[1];
		int read = read(singleByte);
		
		return (read > 0) ? singleByte[0] : read;
	}
	
	private void readHeader() throws IOException {
		try {
			readAndVerifyMagicNoHmac();
			readAndVerifyVersionNoHmac();			
			
			byte[] hmacSalt = readNoHmac(underlyingInputStream, MultiCipherOutputStream.SALT_SIZE);
			SecretKey hmacSecretKey = CipherUtil.createSecretKey(MultiCipherOutputStream_UNUSED.HMAC_ALGORITHM, MultiCipherOutputStream_UNUSED.HMAC_KEY_SIZE, cipherSession.getPassword(), hmacSalt);
			
			hmac = Mac.getInstance(MultiCipherOutputStream_UNUSED.HMAC_ALGORITHM, CipherUtil.PROVIDER);
	        hmac.init(hmacSecretKey);
	        hmacLength = hmac.getMacLength();
			
			int cipherSpecCount = readByteAndUpdateHmac(underlyingInputStream);
			
			pipedOutputStream = new PipedOutputStream();
			pipedInputStream = new PipedInputStream(pipedOutputStream, BUFFER_SIZE);
			
			cipherInputStream = pipedInputStream;
			
			for (int i=0; i<cipherSpecCount; i++) {
				int cipherSpecId = readByteAndUpdateHmac(underlyingInputStream);
				
				CipherSpec cipherSpec = CipherSpecs.getCipherSpec(cipherSpecId);
				
				if (cipherSpec == null) {
					throw new IOException("Cannot find cipher suite with ID "+cipherSpecId);
				}
				
				cipherSpecs.add(cipherSpec);
				
				byte[] salt = readAndUpdateHmac(underlyingInputStream, MultiCipherOutputStream.SALT_SIZE);
				byte[] iv = readAndUpdateHmac(underlyingInputStream, cipherSpec.getIvSize()/8);
				
				SecretKey secretKey = CipherUtil.createSecretKey(cipherSpec, cipherSession.getPassword(), salt); 
				Cipher decryptCipher = CipherUtil.createDecCipher(cipherSpec, secretKey, iv);
				
				cipherInputStream = new GcmCompatibleCipherInputStream(cipherInputStream, decryptCipher);		
			}	    	
    	}
    	catch (Exception e) {
    		throw new IOException(e);
    	}
	}

	private int readByteAndUpdateHmac(InputStream inputStream) throws IOException {
		int readByte = inputStream.read();
		hmac.update((byte) readByte);
		
		return readByte;
	}

	private byte[] readNoHmac(InputStream inputStream, int readCount) throws IOException {
		byte[] buffer = new byte[readCount];
		int actuallyReadCount = inputStream.read(buffer);
		
		if (actuallyReadCount < readCount) {
			throw new IOException("Cannot read "+readCount+" bytes");
		}
		
		return buffer;
	}

	private void readAndVerifyMagicNoHmac() throws IOException {
		byte[] streamMagic = new byte[MultiCipherOutputStream.STREAM_MAGIC.length];
		underlyingInputStream.read(streamMagic);
		
		if (!Arrays.equals(MultiCipherOutputStream.STREAM_MAGIC, streamMagic)) {
			throw new IOException("Not a Syncany-encrypted file, no magic!");
		}
	}

	private void readAndVerifyVersionNoHmac() throws IOException {
		byte streamVersion = (byte) underlyingInputStream.read();
		
		if (streamVersion != MultiCipherOutputStream.STREAM_VERSION) {
			throw new IOException("Stream version not supported: "+streamVersion);
		}		
	}
	
	private byte[] readAndUpdateHmac(InputStream inputStream, int readCount) throws IOException {
		byte[] salt = new byte[readCount]; 
    	inputStream.read(salt);
    	
    	return salt;
	}
	
	@Override
	public void close() throws IOException {
		cipherInputStream.close();
	}	
}
