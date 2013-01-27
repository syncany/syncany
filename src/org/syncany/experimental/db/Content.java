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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author pheckel
 */
public class Content implements Persistable, Serializable {
    private byte[] checksum;
    private int contentSize;
    private List<ContentChunk> chunks;
    
    private transient Database db; // TODO this is ugly!
    
    public Content() {
        this.chunks = new ArrayList<ContentChunk>();
    }
       
    public Content(Database db) {
        this();
        this.db = db;
    }        

    public void addChunk(ChunkEntry chunk) {
        chunks.add(new ContentChunk(this, chunk, chunks.size()));        
    }    

    public byte[] getChecksum() {
        return checksum;
    }

    public void setChecksum(byte[] checksum) {
        this.checksum = checksum;
    }

    public int getContentSize() {
        return contentSize;
    }

    public void setContentSize(int contentSize) {
        this.contentSize = contentSize;
    }

    public List<ChunkEntry> getChunks() {
        List<ChunkEntry> realChunks = new LinkedList<ChunkEntry>();
        
        for (ContentChunk contentChunk : chunks) {
            realChunks.add(contentChunk.getChunk());
        }
        
        return realChunks;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        // Content checksum + size
        out.writeByte(checksum.length);
        out.write(checksum);        
        
        out.writeInt(contentSize);

        // Chunks (size + local references)
        out.writeInt(chunks.size());

        for (ContentChunk chunk : chunks) {
            out.write(chunk.getChunk().getChecksum());                    
        }
    }

    @Override
    public int read(DataInput in) throws IOException {
        int checksumLen = in.readByte();

        checksum = new byte[checksumLen];
        in.readFully(checksum, 0, checksumLen);        
        
        contentSize = in.readInt();
        
        int chunksCount = in.readInt();
        
        for (int i = 0; i < chunksCount; i++) {
            byte[] chunkChecksum = new byte[checksumLen];
            in.readFully(chunkChecksum);
            
            ChunkEntry chunk = db.getChunk(chunkChecksum);
            
            if (chunk == null) {
                throw new IOException("Chunk with checksum "+Arrays.toString(chunkChecksum)+" does not exist.");
            }
            
            addChunk(chunk);
        }
        
        return 1+checksumLen+4+4+chunksCount*checksumLen;        
    }

    
            
}
