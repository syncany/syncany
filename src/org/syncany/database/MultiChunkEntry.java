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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author pheckel
 */
public class MultiChunkEntry  {
    private byte[] id;    
    private List<ChunkEntry> chunks;
        
    public MultiChunkEntry(byte[] id) {
        this.chunks = new ArrayList<ChunkEntry>();
        this.id = id;
    }
    
    public void addChunk(ChunkEntry chunk) {
        chunks.add(chunk);
    }    

    public byte[] getId() {
        return id;
    }

    public void setId(byte[] id) {
        this.id = id;
    }

    public List<ChunkEntry> getChunks() {
        return chunks;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(id);
		result = prime * result + ((chunks == null) ? 0 : chunks.hashCode());
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
		MultiChunkEntry other = (MultiChunkEntry) obj;
		if (!Arrays.equals(id, other.id))
			return false;
		if (chunks == null) {
			if (other.chunks != null)
				return false;
		} else if (!chunks.equals(other.chunks))
			return false;
		return true;
	}

}
