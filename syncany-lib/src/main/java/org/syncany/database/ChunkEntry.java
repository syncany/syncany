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
 *
 * @author pheckel
 */
public class ChunkEntry {
	private ChunkChecksum checksum;
	private int size;

	public ChunkEntry(ChunkChecksum checksum, int size) {
		this.checksum = checksum;
		this.size = size;
	}

	public void setSize(int chunksize) {
		this.size = chunksize;
	}

	public int getSize() {
		return size;
	}

	public ChunkChecksum getChecksum() {
		return checksum;
	}

	public void setChecksum(ChunkChecksum checksum) {
		this.checksum = checksum;
	}

	@Override
	public String toString() {
		return "ChunkEntry [checksum=" + checksum + ", size=" + size + "]";
	}	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((checksum == null) ? 0 : checksum.hashCode());
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
		if (checksum == null) {
			if (other.checksum != null)
				return false;
		}
		else if (!checksum.equals(other.checksum))
			return false;
		if (size != other.size)
			return false;
		return true;
	}

	public static class ChunkChecksum extends ObjectId {
		public ChunkChecksum(byte[] array) {
			super(array);
		}

		public static ChunkChecksum parseChunkChecksum(String s) {
			return new ChunkChecksum(ObjectId.parseBytes(s));
		}		
	}
}
