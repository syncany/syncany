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

/**
 * @author fabrice rossi
 *
 */
public class FileId {
	private long first;

	private long second;

	/**
	 * @param first
	 * @param second
	 */
	public FileId(long first, long second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (first ^ (first >>> 32));
		result = prime * result + (int) (second ^ (second >>> 32));
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
		if (!(obj instanceof FileId)) {
			return false;
		}
		FileId other = (FileId) obj;
		if (first != other.first) {
			return false;
		}
		if (second != other.second) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return String.format("%016x%016x", first, second);
	}

	private static long parseHexLong(String s) throws NumberFormatException {
		// this is used to induce the proper overflow needed to 2 complements coding of long
		long baseValue = Long.parseLong(s.substring(1), 16);
		long firstDigit = Character.digit(s.charAt(0), 16);
		return (firstDigit << 60) + baseValue;
	}

	public static FileId parseFileId(String s) throws NumberFormatException {
		if (s.length() != 32) {
			throw new NumberFormatException("Expecting 32 hexadecimal representation");
		}
		return new FileId(parseHexLong(s.substring(0, 16)), parseHexLong(s.substring(16)));
	}
}
