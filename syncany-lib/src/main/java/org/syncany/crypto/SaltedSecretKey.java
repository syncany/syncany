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
package org.syncany.crypto;

import java.util.Arrays;

import javax.crypto.SecretKey;

import org.syncany.util.StringUtil;

/**
 * A salted secret key is a convenience class to bundle a {@link SecretKey} with
 * its corresponding salt. It is mainly used to represent the master key and the
 * master key salt. 
 *  
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class SaltedSecretKey implements SecretKey {
	private static final long serialVersionUID = 1216126055360327470L;
	
	private SecretKey secretKey;
	private byte[] salt;
	
	public SaltedSecretKey(SecretKey secretKey, byte[] salt) {
		this.secretKey = secretKey;
		this.salt = salt;
	}

	public byte[] getSalt() {
		return salt;
	}

	@Override
	public String getAlgorithm() {
		return secretKey.getAlgorithm();
	}

	@Override
	public String getFormat() {
		return secretKey.getFormat();
	}

	@Override
	public byte[] getEncoded() {
		return secretKey.getEncoded();
	}
	
	@Override
	public String toString() {
		return "[secretKey={algorithm=" + getAlgorithm() + ", format=" + getFormat() + ", encoded=" + StringUtil.toHex(getEncoded()) + "}, salt="
				+ StringUtil.toHex(getSalt()) + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(salt);
		result = prime * result + ((secretKey == null) ? 0 : secretKey.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SaltedSecretKey other = (SaltedSecretKey) obj;
		if (!Arrays.equals(salt, other.salt))
			return false;
		if (secretKey == null) {
			if (other.secretKey != null)
				return false;
		} else if (!secretKey.equals(other.secretKey))
			return false;
		return true;
	}		
}
