/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.util.chunk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class MultiChunk {
    protected byte[] id;
    protected List<Chunk> chunks;
    protected int chunkSize;
    protected int minSize;
    protected int actualSize;
    
    public MultiChunk(byte[] id, int minSize) {
        this.id = id;
        this.minSize = minSize;
        this.chunks = new ArrayList<Chunk>();
        this.chunkSize = 0;
        this.actualSize = 0;
    }
    
    public boolean isFull() {
        return chunkSize >= minSize;
    }

    public int getChunkSize() {
        return chunkSize;
    }
    
    public int getActualSize() {
        return actualSize;
    }

    public byte[] getId() {
        return id;
    }

    public void setId(byte[] id) {
        this.id = id;
    }

    public abstract Chunk read() throws IOException;
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
