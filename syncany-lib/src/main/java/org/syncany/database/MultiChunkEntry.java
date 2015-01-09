/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
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

import java.util.ArrayList;
import java.util.List;

import org.syncany.database.ChunkEntry.ChunkChecksum;

/**
 * The multichunk entry represents the chunk container in which a set of
 * {@link ChunkEntry}s is stored. On a file, level, a multichunk is represented
 * by a file (container format) and chunks are added to this file.
 *
 * <p>A multichunk is identified by a unique identifier (random, not a checksum),
 * and contains references to {@link ChunkEntry}s.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class MultiChunkEntry {
	private static final byte MULTICHUNK_ID_LENGTH = 20;

	private MultiChunkId id;
	private long size;
	private List<ChunkChecksum> chunks;

	public MultiChunkEntry(MultiChunkId id, long size) {
		this.id = id;
		this.size = size;
		this.chunks = new ArrayList<ChunkChecksum>();
	}

	public void addChunk(ChunkChecksum chunk) {
		chunks.add(chunk);
	}

	public MultiChunkId getId() {
		return id;
	}

	public void setId(MultiChunkId id) {
		this.id = id;
	}

	public List<ChunkChecksum> getChunks() {
		return chunks;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((chunks == null) ? 0 : chunks.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		if (!(obj instanceof MultiChunkEntry)) {
			return false;
		}
		MultiChunkEntry other = (MultiChunkEntry) obj;
		if (chunks == null) {
			if (other.chunks != null) {
				return false;
			}
		}
		else if (!chunks.equals(other.chunks)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		}
		else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "MultiChunkEntry [id=" + id + ", chunks=" + chunks + "]";
	}

	public static class MultiChunkId extends ObjectId {
		public MultiChunkId(byte[] array) {
			super(array);
		}

		public static MultiChunkId secureRandomMultiChunkId() {
			return new MultiChunkId(ObjectId.secureRandomBytes(MULTICHUNK_ID_LENGTH));
		}

		public static MultiChunkId parseMultiChunkId(String s) {
			return new MultiChunkId(ObjectId.parseObjectId(s));
		}
	}

}
