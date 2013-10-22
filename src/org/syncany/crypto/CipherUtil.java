package org.syncany.crypto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.output.ByteArrayOutputStream;

public class CipherUtil {
	public static String decryptToString(InputStream inputStream, String password) throws IOException {
		String plaintextString = null;
		
		while (plaintextString == null) {
			CipherSession cipherSession = new CipherSession(null, password);
			AdvancedCipherInputStream cipherInputStream = new AdvancedCipherInputStream(inputStream, cipherSession);
			
			ByteArrayOutputStream plaintextOutputStream = new ByteArrayOutputStream();
						
			int read = -1;
			byte[] buffer = new byte[1024];
			
			while (-1 != (read = cipherInputStream.read(buffer))) {
				plaintextOutputStream.write(buffer, 0, read);
			}
			
			cipherInputStream.close();
			plaintextOutputStream.close();
			
			byte[] plaintextByteArray = plaintextOutputStream.toByteArray();
			
			if (plaintextByteArray.length >= AdvancedCipherInputStream.STREAM_MAGIC.length) { // TODO [medium] Workaround, this should be done with Adv(Adv(Adv(in))).read()
				byte[] plaintextPotentialMagic = new byte[AdvancedCipherInputStream.STREAM_MAGIC.length];
				System.arraycopy(plaintextByteArray, 0, plaintextPotentialMagic, 0, plaintextPotentialMagic.length);
				
				if (!Arrays.equals(AdvancedCipherInputStream.STREAM_MAGIC, plaintextPotentialMagic)) {
					plaintextString = new String(plaintextByteArray);
				}
				else {
					inputStream = new ByteArrayInputStream(plaintextByteArray);
				}
			}
			else {
				plaintextString = new String(plaintextByteArray);
			}
		}
		
		return plaintextString;		
	}
}
