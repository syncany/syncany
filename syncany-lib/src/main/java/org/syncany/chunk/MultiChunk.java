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
package org.syncany.chunk;

import java.io.IOException;
import java.io.InputStream;

import org.syncany.database.MultiChunkEntry.MultiChunkId;

/**
 * A multichunk represents the container format that stores one to many {@link Chunk}s.
 * Multichunks are created during the chunking/deduplication process by a {@link MultiChunker}.
 *
 * <p>There are two modes to handle multichunks:
 *
 * <ul>
 *  <li>When a new multichunk is <i>written</i> and filled up with chunks, the {@link Deduper} makes sure that
 *      chunks are only added until a multichunk's minimum size has been reached, and closes the
 *      multichunk afterwards. During that process, the {@link #write(Chunk) write()} method is called
 *      for each chunk, and {@link #isFull()} is checked for the size.
 *
 *  <li>When a multichunk is <i>read</i> from a file or an input stream, it can be processed sequentially using
 *      the {@link #read()} method (not used in current code!), or in a random order using the
 *      {@link #getChunkInputStream(byte[]) getChunkInputStream()} method. Because of the latter method,
 *      <b>it is essential that random read access on a multichunk is possible</b>.
 * </ul>
 *
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
public abstract class MultiChunk {
	protected MultiChunkId id;
	protected long size;
	protected int minSize; // in KB

	/**
	 * Creates a new multichunk.
	 *
	 * <p>This method should be used if the multichunk identifier is known to the
	 * calling method. This is typically the case if a new multichunk is written.
	 *
	 * @param id Unique multichunk identifier (can be randomly chosen)
	 * @param minSize Minimum multichunk size, used to determine if chunks can still be added
	 */
	public MultiChunk(MultiChunkId id, int minSize) {
		this.id = id;
		this.minSize = minSize;
		this.size = 0;
	}

	/**
	 * Creates a new multichunk.
	 *
	 * <p>This method should be used if the multichunk identifier is <i>not</i> known to the
	 * calling method. This is typically the case if a multichunk is read from a file.
	 *
	 * @param minSize Minimum multichunk size, used to determine if chunks can still be added
	 */
	public MultiChunk(int minSize) {
		this(null, minSize);
	}

	/**
	 * In write mode, this method can be used to write {@link Chunk}s to a multichunk.
	 *
	 * <p>Implementations must increase the {@link #size} by the amount written to the multichunk
	 * (input size sufficient) and make sure that (if required) a header is written for the first
	 * chunk.
	 *
	 * <p>Implementations do not have to check whether or not a multichunk is full. This should be
	 * done outside the multichunker/multichunk as part of the deduplication algorithm in the {@link Deduper}.
	 *
	 * @param chunk Chunk to be written to the multichunk container
	 * @throws IOException If an exception occurs when writing to the multichunk
	 */
	public abstract void write(Chunk chunk) throws IOException;

	/**
	 * In read mode, this method can be used to <b>sequentially</b> read {@link Chunk}s from a multichunk.
	 * The method returns a chunk until no more chunks are available, at which point it will return
	 * <code>null</code>.
	 *
	 * <p>If random read access on a multichunk is desired, the
	 * {@link #getChunkInputStream(byte[]) getChunkInputStream()} method should be used instead.
	 *
	 * @return Returns the next chunk in the opened multichunk, or <code>null</code> if no chunk is available (anymore)
	 * @throws IOException If an exception occurs when reading from the multichunk
	 */
	// TODO [low] Method is only used by tests, not necessary anymore? Required for 'cleanup'?
	public abstract Chunk read() throws IOException;

	/**
	 * In read mode, this method can be used to read {@link Chunk}s in <b>random access mode</b>, using a chunk
	 * checksum as identifier. The method returns a chunk input stream (the chunk's data) if the chunk is
	 * found, and <code>null</code> otherwise.
	 *
	 * <p>If all chunks are read from a multichunk sequentially, the {@link #read()} method should be used instead.
	 *
	 * @param checksum The checksum identifying a chunk instance
	 * @return Returns a chunk input stream (chunk data) if the chunk can be found in the multichunk, or <code>null</code> otherwise
	 * @throws IOException If an exception occurs when reading from the multichunk
	 */
	// TODO [low] Method should be named 'read(checksum)' and return a Chunk object, not an input stream, right?!
	public abstract InputStream getChunkInputStream(byte[] checksum) throws IOException;

	/**
	 * Closes a multichunk after writing/reading.
	 *
	 * <p>Implementations should close the underlying input/output stream (depending on
	 * whether the chunk was opened in read or write mode.
	 *
	 * @throws IOException If an exception occurs when closing the multichunk
	 */
	public abstract void close() throws IOException;

	/**
	 * In write mode, this method determines the fill state of the multichunk and
	 * returns whether or not a new chunk can still be added. It is used by the
	 * {@link Deduper}.
	 *
	 * @return Returns <code>true</code> if no more chunks should be added and the chunk should be closed, <code>false</code> otherwise
	 */
	public boolean isFull() {
		return size >= minSize;
	}

	public long getSize() {
		return size;
	}

	public MultiChunkId getId() {
		return id;
	}

	public void setId(MultiChunkId id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + minSize;
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
		if (!(obj instanceof MultiChunk)) {
			return false;
		}
		MultiChunk other = (MultiChunk) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		}
		else if (!id.equals(other.id)) {
			return false;
		}
		if (minSize != other.minSize) {
			return false;
		}
		if (size != other.size) {
			return false;
		}
		return true;
	}
}
