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
package org.syncany.util;

import java.math.BigInteger;

/**
 * base58 encoding
 * @author Vincent Wiencek <vwiencek@gmail.com>
 */
public class Base58 {

	private static final String Alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
	private static final BigInteger Base58 = new BigInteger("58", 10);

	public static String encode(byte[] indata) {
		byte[] data = new byte[indata.length];
		System.arraycopy(indata, 0, data, 0, data.length);
		// convert big endian data to little endian
		int len = data.length;
		int mid = len / 2;
		int i;
		for (i = 0; i < mid; i++) {
			byte tmp = data[i];
			data[i] = data[len - 1 - i];
			data[len - 1 - i] = tmp;
		}
		BigInteger value = new BigInteger(1, data);
		StringBuilder result = new StringBuilder();

		// divide until zero
		while (value.compareTo(BigInteger.ZERO) > 0) {
			BigInteger[] qr = value.divideAndRemainder(Base58);
			value = qr[0];
			BigInteger rem = qr[1];
			result.append(Alphabet.charAt(rem.intValue()));
		}
		// append leading zeros
		for (i = 0; i < len; i++) {
			if (data[i] == 0) {
				result.append(Alphabet.charAt(0));
			}
			else {
				break;
			}
		}
		// reverse to big endian
		len = result.length();
		mid = len / 2;
		for (i = 0; i < mid; i++) {
			char ch1 = result.charAt(i);
			char ch2 = result.charAt(len - 1 - i);
			result.setCharAt(len - 1 - i, ch1);
			result.setCharAt(i, ch2);
		}
		return result.toString();
	}

	public static byte[] decode(String data) {
		BigInteger value = BigInteger.ZERO;
		BigInteger character;
		// eat spaces
		data = data.trim();
		// encode to big number
		int i;
		int len = data.length();
		for (i = 0; i < len; i++) {
			int pos = Alphabet.indexOf(data.charAt(i));
			if (pos < 0) { // not a valid char
				return null;
			}
			character = new BigInteger(String.valueOf(pos), 10);
			value = value.multiply(Base58);
			value = value.add(character);
		}

		byte[] dec = value.toByteArray();
		int declen = dec.length;
		int decoff = 0;
		// strip sign byte
		if (dec.length > 2 && dec[0] == 0 && (dec[1] & 0x80) == 0x80) {
			declen--;
			decoff++;
		}
		// count leading zeros
		int n = 0;
		for (i = 0; i < len; i++) {
			if (data.charAt(i) == Alphabet.charAt(0)) {
				n++;
			}
			else {
				break;
			}
		}
		// reverse result at the right place
		byte[] result = new byte[declen + n];
		for (i = 0; i < declen; i++) {
			result[i] = dec[(declen - 1 - i) + decoff];
		}
		return result;
	}
}