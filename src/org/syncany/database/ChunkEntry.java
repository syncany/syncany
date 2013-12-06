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

import java.util.Arrays;

import org.syncany.util.ObjectId;
import org.syncany.util.StringUtil;

/**
 *
 * @author pheckel
 */
public class ChunkEntry {
	private byte[] checksum;
	private int size;

	public ChunkEntry(byte[] checksum, int size) {
		this.checksum = checksum;
		this.size = size;
	}

	public void setSize(int chunksize) {
		this.size = chunksize;
	}

	public int getSize() {
		return size;
	}

	public byte[] getChecksum() {
		return checksum;
	}

	public void setChecksum(byte[] checksum) {
		this.checksum = checksum;
	}

	@Override
	public String toString() {
		return "ChunkEntry [checksum=" + StringUtil.toHex(checksum) + ", size=" + size + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(checksum);
		result = prime * result + size;
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
		ChunkEntry other = (ChunkEntry) obj;
		if (!Arrays.equals(checksum, other.checksum))
			return false;
		if (size != other.size)
			return false;
		return true;
	}

	/**
	 * Identifies a chunk entry (= chunk checksum)
	 * TODO [low] Cleanup chunk entry id usage in application. What about a MultiChunkEntryId, FileContentId, ...
	 */
	public static class ChunkEntryId extends ObjectId {
		public ChunkEntryId(byte[] array) {
			super(array);
		}

		public static ChunkEntryId parseChunkEntryId(String s) {
			return new ChunkEntryId(ObjectId.parseBytes(s));
		}
		
		public byte[] getChecksum() {
			return array;
		}
	}
}
