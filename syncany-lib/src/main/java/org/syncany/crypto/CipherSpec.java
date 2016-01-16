/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com>
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
package org.syncany.crypto;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

/**
 * A cipher spec represents the definition of a cipher/encryption algorithm and the
 * corresponding settings required to instantiate a new cipher object.
 *
 * <p>Cipher specs are identified by an identifier (<i>id</i>), which will (when the
 * cipher spec is used by the {@link MultiCipherOutputStream}) be written to the output
 * file format. When the file is read by {@link MultiCipherInputStream}, the identifier
 * is looked up using the {@link CipherSpecs} class.
 *
 * <p>While it would be technically possible to define any kind of cipher using this class,
 * this class restricts the allowed algorithms to a few ones that are considered secure.
 *
 * <p>Instantiating a cipher spec that does pass the sanity checks will result in a
 * RuntimeException.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class CipherSpec {
	public static final Pattern ALLOWED_CIPHER_ALGORITHMS = Pattern.compile("^HmacSHA256$|(^(AES|Twofish)/(GCM|EAX)/.+)");

	private int id;
	private String algorithm;
	private int keySize; // in bits
	private int ivSize; // in bits
	private boolean needsUnlimitedStrength;

	public CipherSpec(int id, String algorithm, int keySize, int ivSize, boolean needsUnlimitedStrength) {
		this.id = id;
		this.algorithm = algorithm;
		this.keySize = keySize;
		this.ivSize = ivSize;
		this.needsUnlimitedStrength = needsUnlimitedStrength;

		doSanityChecks();
	}

	public int getId() {
		return id;
	}

	public boolean needsUnlimitedStrength() {
		return needsUnlimitedStrength;
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public int getKeySize() {
		return keySize;
	}

	public int getIvSize() {
		return ivSize;
	}

	public abstract OutputStream newCipherOutputStream(OutputStream underlyingOutputStream, byte[] secretKey, byte[] iv) throws CipherException;

	public abstract InputStream newCipherInputStream(InputStream underlyingInputStream, byte[] secretKey, byte[] iv) throws CipherException;

	@Override
	public String toString() {
		return algorithm + ", " + keySize + " bit";
	}

	private void doSanityChecks() {
		if (!ALLOWED_CIPHER_ALGORITHMS.matcher(algorithm).matches()) {
			throw new RuntimeException("Cipher algorithm or mode not allowed: " + algorithm + ". This mode is not considered secure.");
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((algorithm == null) ? 0 : algorithm.hashCode());
		result = prime * result + id;
		result = prime * result + ivSize;
		result = prime * result + keySize;
		result = prime * result + (needsUnlimitedStrength ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof CipherSpec)) {
			return false;
		}
		CipherSpec other = (CipherSpec) obj;
		if (algorithm == null) {
			if (other.algorithm != null) {
				return false;
			}
		}
		else if (!algorithm.equals(other.algorithm)) {
			return false;
		}
		if (id != other.id) {
			return false;
		}
		if (ivSize != other.ivSize) {
			return false;
		}
		if (keySize != other.keySize) {
			return false;
		}
		if (needsUnlimitedStrength != other.needsUnlimitedStrength) {
			return false;
		}
		return true;
	}
}
