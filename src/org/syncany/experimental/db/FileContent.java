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

import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;

/**
 *
 * @author pheckel
 */
public class FileContent {
    private byte[] checksum;
    private int contentSize;
    
    private TreeMap<Integer, ChunkEntry> chunks;
    
    public FileContent() {
        this.chunks = new TreeMap<Integer, ChunkEntry>();
    }
       
    public void addChunk(ChunkEntry chunk) {
        chunks.put(chunks.size(), chunk);        
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

    public Collection<ChunkEntry> getChunks() {
    	return Collections.unmodifiableCollection(chunks.values());
    }
            
}
