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
package org.syncany.database;

import java.util.Arrays;
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

    public int getSize() {
        return contentSize;
    }

    public void setSize(int contentSize) {
        this.contentSize = contentSize;
    }

    public Collection<ChunkEntry> getChunks() {
    	return Collections.unmodifiableCollection(chunks.values());
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(checksum);
		result = prime * result + ((chunks == null) ? 0 : chunks.hashCode());
		result = prime * result + contentSize;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileContent other = (FileContent) obj;
		if (!Arrays.equals(checksum, other.checksum))
			return false;
		if (chunks == null) {
			if (other.chunks != null)
				return false;
		} else if (!chunks.equals(other.chunks))
			return false;
		if (contentSize != other.contentSize)
			return false;
		return true;
	}
            
}
