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
package org.syncany.chunk;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class MultiChunk {
    protected byte[] id;
    protected long size;
    protected int minSize;
    
    public MultiChunk(byte[] id, int minSize) {
        this.id = id;
        this.minSize = minSize;
        this.size = 0;
    }
    
    public MultiChunk(int minSize) {
    	this(null, minSize);
    }
    
    public boolean isFull() {
        return size >= minSize;
    }

    public long getSize() {
        return size;
    }

    public byte[] getId() {
        return id;
    }

    public void setId(byte[] id) {
        this.id = id;
    }

    public abstract InputStream getChunkInputStream(byte[] checksum) throws IOException;
    public abstract Chunk read() throws IOException; // TODO [low] Not necessary anymore
    public abstract void write(Chunk chunk) throws IOException;
    public abstract void close() throws IOException;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MultiChunk other = (MultiChunk) obj;
        if (!Arrays.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }
    
}
