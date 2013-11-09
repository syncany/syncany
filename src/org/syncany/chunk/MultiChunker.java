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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class MultiChunker {
    protected int minMultiChunkSize;
    
    /**
     * Sets the minimum size of a multi chunk in kb.
     * @param minMultiChunkSize
     */
    public MultiChunker(int minMultiChunkSize)  {
        this.minMultiChunkSize = minMultiChunkSize;
    }

    /**
     * 
     * @return min multi-chunk size in kb
     */
    public int getMinMultiChunkSize() {
        return minMultiChunkSize;
    }
    
    /**
     * Creates multi chunk for read operations. 
     * @param is InputStream to initialize an existing multiChunk for read-operations only
     * @return
     */
    public abstract MultiChunk createMultiChunk(InputStream is);
    
    public abstract MultiChunk createMultiChunk(File file) throws IOException;
    
    /**
     * 
     * @param id of multichunk to create
     * @param os Outputstream for new file
     * @return
     * @throws IOException
     */
    public abstract MultiChunk createMultiChunk(byte[] id, OutputStream os) throws IOException;
    
    @Override
    public abstract String toString();    
}
