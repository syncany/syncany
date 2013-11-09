/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import javax.crypto.SecretKey;

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
}
