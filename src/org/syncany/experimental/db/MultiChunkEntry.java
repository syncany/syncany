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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pheckel
 */
public class MultiChunkEntry  {
    private Long id;
    private byte[] checksum;    
    private List<ChunkEntry> chunks;
        
    public MultiChunkEntry() {
        this.chunks = new ArrayList<ChunkEntry>();
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

}
