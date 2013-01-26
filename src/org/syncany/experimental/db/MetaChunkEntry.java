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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author pheckel
 */
public class MetaChunkEntry implements Persistable {
    private Long id;
    private byte[] checksum;    
    private int actualSize;
    private int chunkSize;
    private List<ChunkEntry> chunks;
    
    private transient Database db; // TODO this is ugly!
    
    public MetaChunkEntry() {
        this.chunks = new ArrayList<ChunkEntry>();
        this.actualSize = 0;
        this.chunkSize = 0;
    }
    
    public MetaChunkEntry(Database db) {
        this();
        this.db = db;
    }    

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public void addChunk(ChunkEntry chunk) {
        chunks.add(chunk);
        
        // Cross-reference
        chunk.addMetaChunk(this);
    }    

    public byte[] getChecksum() {
        return checksum;
    }

    public void setChecksum(byte[] checksum) {
        this.checksum = checksum;
    }

    public List<ChunkEntry> getChunks() {
        return chunks;
    }

    public int getActualSize() {
        return actualSize;
    }

    public void setActualSize(int actualSize) {
        this.actualSize = actualSize;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
    
    @Override
    public void write(DataOutput out) throws IOException {
        // Content checksum + size
        out.writeByte(checksum.length);
        out.write(checksum);        
        
        // Chunks (size + local references)
        out.writeShort(chunks.size());

        for (ChunkEntry chunk : getChunks()) {                    
            out.write(chunk.getChecksum());                    
        }        
    }

    @Override
    public int read(DataInput in) throws IOException {        
        int checksumLen = in.readByte();
        
        checksum = new byte[checksumLen];
        in.readFully(checksum, 0, checksumLen);
        
        int chunksCount = in.readShort();
        
        for (int i = 0; i < chunksCount; i++) {
            byte[] chunkChecksum = new byte[checksumLen];
            in.readFully(chunkChecksum);
            
            ChunkEntry chunk = db.getChunk(chunkChecksum);
            
            if (chunk == null) {
                throw new IOException("Chunk with checksum "+Arrays.toString(chunkChecksum)+" does not exist.");
            }

            // Add to list
            chunks.add(chunk);
            chunkSize += chunk.getChunksize();

            // Cross-reference
            chunk.addMetaChunk(this);            
        }
        
        return 1+checksumLen+2+chunksCount*checksumLen;
    }


            
}
