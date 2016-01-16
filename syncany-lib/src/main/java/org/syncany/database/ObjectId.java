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
package org.syncany.database;

import java.security.SecureRandom;
import java.util.Arrays;

import org.syncany.util.StringUtil;

/**
 * The object ID is a generic identifier used by the database entities.
 * As of now, it uses a byte array internally, but could also use different
 * more memory-preserving methods (such as two longs).  
 * 
 * @author Fabrice Rossi <fabrice.rossi@apiacoa.org>
 */
public abstract class ObjectId {
	private static SecureRandom secureRng = new SecureRandom();
	protected byte[] identifier;
	
	public ObjectId(byte[] identifier) {
		if (identifier == null) {
			throw new IllegalArgumentException("Argument 'identifier' cannot be null.");
		}
		
		this.identifier = identifier;
	}
	
	/**
	 * Returns the raw representation of the object identifier in the
	 * form of a byte array.  
	 */
	public byte[] getBytes() {
		return Arrays.copyOf(identifier, identifier.length);
	}

	/**
	 * Converts the byte-array based identifier to a lower 
	 * case hex string and returns this string.
	 */
	@Override
	public String toString() {
		return StringUtil.toHex(identifier);
	}

	public static byte[] parseObjectId(String s) {
		if (s == null) {
			throw new IllegalArgumentException("Argument 's' cannot be null.");
		}
		
		return StringUtil.fromHex(s);
	}

	public static byte[] secureRandomBytes(int size) {
		byte[] newRandomBytes = new byte[size];	
		
		synchronized (secureRng) {
			secureRng.nextBytes(newRandomBytes);			
		}
		
		return newRandomBytes;
	}		
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(identifier);
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
		if (!(obj instanceof ObjectId)) {
			return false;
		}
		ObjectId other = (ObjectId) obj;
		if (!Arrays.equals(identifier, other.identifier)) {
			return false;
		}
		return true;
	}
}
