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
package org.syncany.chunk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.syncany.database.MultiChunkEntry.MultiChunkId;

/**
 *
 * @author pheckel
 */
public class ZipMultiChunker extends MultiChunker {
	public static final String TYPE = "zip";

	public ZipMultiChunker() {
		// Nothing
	}

	public ZipMultiChunker(int minMultiChunkSize) {
		super(minMultiChunkSize);
	}

	@Override
	public MultiChunk createMultiChunk(InputStream is) {
		return new ZipMultiChunk(is);
	}

	@Override
	public MultiChunk createMultiChunk(File file) throws IOException {
		try {
			return new ZipMultiChunk(file);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public MultiChunk createMultiChunk(MultiChunkId id, OutputStream os) throws IOException {
		return new ZipMultiChunk(id, minMultiChunkSize, os);
	}

	@Override
	public String toString() {
		return "Zip-" + minMultiChunkSize;
	}

}
