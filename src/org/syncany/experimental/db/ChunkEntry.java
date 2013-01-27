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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author pheckel
 */
public class ChunkEntry implements Serializable, Persistable {
    private byte[] checksum;      
    private int chunksize;    
    private transient List<MultiChunkEntry> metaChunks;
    
    public ChunkEntry() {
        this.metaChunks = new LinkedList<MultiChunkEntry>();
    }
    
    public ChunkEntry(byte[] checksum, int size) {
        this();        
        this.checksum = checksum;
        this.chunksize = size;
    }    

    public void setChunksize(int chunksize) {
        this.chunksize = chunksize;
    }

    public int getChunksize() {
        return chunksize;
    }   
    
    public byte[] getChecksum() {
        return checksum;
    }

    public void setChecksum(byte[] checksum) {
        this.checksum = checksum;
    }
    
    public void addMetaChunk(MultiChunkEntry metaChunk) {
        metaChunks.add(metaChunk);
    }
    
    public List<MultiChunkEntry> getMetaChunks() {
        return metaChunks;
    }
    
    public MultiChunkEntry getMetaChunk() {
        return (!metaChunks.isEmpty()) ? metaChunks.get(0) : null;
    }
    
    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(checksum.length);
        out.write(checksum);
    }

    @Override
    public int read(DataInput in) throws IOException {
        int checksumLen = in.readByte();
        
        checksum = new byte[checksumLen];
        in.readFully(checksum, 0, checksumLen);
        
        return 1+checksumLen;
    }
   
    
    
}
