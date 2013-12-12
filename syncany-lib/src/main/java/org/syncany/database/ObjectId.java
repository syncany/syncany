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
	protected byte[] array;
	
	public ObjectId(byte[] array) {
		this.array = array;
	}
	
	@Deprecated
	public byte[] getRaw() {
		return array;
	}

	@Override
	public String toString() {
		return StringUtil.toHex(array);
	}

	public static byte[] parseBytes(String s) {
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
		result = prime * result + Arrays.hashCode(array);
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
		if (!Arrays.equals(array, other.array)) {
			return false;
		}
		return true;
	}
}
