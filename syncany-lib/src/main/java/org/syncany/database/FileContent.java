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
import java.util.Collections;
import java.util.List;

import org.syncany.database.ChunkEntry.ChunkChecksum;

/**
 * A file content represents the content of a file. It contains a list of
 * references to {@link ChunkEntry}s, and identifies a content by its checksum.
 *
 * <p>A file content is implicitly referenced by one or many {@link FileVersion}s
 * through the checksum attribute. A file content always contains the full list of
 * chunks it resembles. There are no deltas!
 *
 * <p>Unlike the chunk list in a {@link MultiChunkEntry}, the order of the chunks
 * is very important, because a file can only be reconstructed if the order of
 * its chunks are followed.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FileContent {
	private FileChecksum checksum;
	private long size;

	private List<ChunkChecksum> chunkChecksums;

	public FileContent() {
		this.chunkChecksums = new ArrayList<ChunkChecksum>();
	}

	public void addChunk(ChunkChecksum chunk) {
		chunkChecksums.add(chunk);
	}

	public FileChecksum getChecksum() {
		return checksum;
	}

	public void setChecksum(FileChecksum checksum) {
		this.checksum = checksum;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long contentSize) {
		size = contentSize;
	}

	public List<ChunkChecksum> getChunks() {
		return Collections.unmodifiableList(chunkChecksums);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((checksum == null) ? 0 : checksum.hashCode());
		result = prime * result + ((chunkChecksums == null) ? 0 : chunkChecksums.hashCode());
		result = prime * result + (int) (size ^ (size >>> 32));
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
		if (!(obj instanceof FileContent)) {
			return false;
		}
		FileContent other = (FileContent) obj;
		if (checksum == null) {
			if (other.checksum != null) {
				return false;
			}
		}
		else if (!checksum.equals(other.checksum)) {
			return false;
		}
		if (chunkChecksums == null) {
			if (other.chunkChecksums != null) {
				return false;
			}
		}
		else if (!chunkChecksums.equals(other.chunkChecksums)) {
			return false;
		}
		if (size != other.size) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "FileContent [checksum=" + checksum + ", contentSize=" + size + ", chunks=" + chunkChecksums + "]";
	}

	public static class FileChecksum extends ObjectId {
		public FileChecksum(byte[] array) {
			super(array);
		}

		public static FileChecksum parseFileChecksum(String s) {
			return new FileChecksum(ObjectId.parseObjectId(s));
		}

		public static boolean fileChecksumEquals(FileChecksum checksum1, FileChecksum checksum2) {
			if (checksum1 != null && checksum2 != null) {
				return checksum1.equals(checksum2);
			}
			else {
				return checksum1 == null && checksum2 == null;
			}
		}
	}
}
