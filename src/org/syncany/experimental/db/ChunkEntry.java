/*
 * Syncany
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
package org.syncany.experimental.db;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author pheckel
 */
public class ChunkEntry {
    private byte[] checksum;      
    private int size;    
    private transient List<MultiChunkEntry> multiChunks;
    
    public ChunkEntry() {
        this.multiChunks = new LinkedList<MultiChunkEntry>();
    }
    
    public ChunkEntry(byte[] checksum, int size) {
        this();        
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
    
    @Deprecated
    // FIXME how to handle cross-references in general
    public void addMetaChunk(MultiChunkEntry metaChunk) {
        multiChunks.add(metaChunk);
    }
    
    public List<MultiChunkEntry> getMultiChunks() {
        return multiChunks;
    }
    
    public MultiChunkEntry getMultiChunk() {
        return (!multiChunks.isEmpty()) ? multiChunks.get(0) : null;
    }
    

    
}
