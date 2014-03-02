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
package org.syncany.crypto.specs;

import java.io.InputStream;
import java.io.OutputStream;

import org.bouncycastle.crypto.engines.TwofishEngine;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.syncany.crypto.BcFixedCipherInputStream;
import org.syncany.crypto.CipherException;
import org.syncany.crypto.CipherSpec;

/**
 * @author pheckel
 *
 */
public abstract class TwofishGcmCipherSpec extends CipherSpec {
	private static final int MAC_SIZE = 128;		

	public TwofishGcmCipherSpec(int id, String algorithm, int keySize, int ivSize, boolean needsUnlimitedStrength) {
		super(id, algorithm, keySize, ivSize, needsUnlimitedStrength);
	}
		
	@Override
	public OutputStream newCipherOutputStream(OutputStream underlyingOutputStream, byte[] secretKey, byte[] iv) throws CipherException {
		AEADBlockCipher cipher = new GCMBlockCipher(new TwofishEngine()); 
		cipher.init(true, new AEADParameters(new KeyParameter(secretKey), MAC_SIZE, iv));
		
		return new org.bouncycastle.crypto.io.CipherOutputStream(underlyingOutputStream, cipher);
	}

	@Override
	public InputStream newCipherInputStream(InputStream underlyingInputStream, byte[] secretKey, byte[] iv) throws CipherException {
		AEADBlockCipher cipher = new GCMBlockCipher(new TwofishEngine()); 
		cipher.init(false, new AEADParameters(new KeyParameter(secretKey), MAC_SIZE, iv));
		
		return new BcFixedCipherInputStream(underlyingInputStream, cipher);
	}
}
