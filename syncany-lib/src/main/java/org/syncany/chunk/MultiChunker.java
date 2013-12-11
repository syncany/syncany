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
 * A multichunker combines a set of {@link Chunk}s into a single file. It can be implemented
 * by a simple container or archive format. The multichunker is used by the {@link Deduper}
 * to write multichunks, and by other parts of the application to read multichunks and
 * re-assemble files.
 * 
 * <p>The class supports two modes: 
 * 
 * <ul>
 * <li>When writing a {@link MultiChunker}, the {@link #createMultiChunk(byte[], OutputStream)}
 *     must be used. The method emits a new implementation-specific {@link MultiChunk} 
 *     to which new chunks can be added/written to.
 *      
 * <li>When reading a multichunk from a file or input stream, the {@link #createMultiChunk(InputStream)}
 *     or {@link #createMultiChunk(File)} must be used. The emitted multichunk object can be read from.
 * </ul>
 * 
 * <p><b>Important:</b> Implementations must make sure that when providing a readable multichunk,
 * the individual chunk objects must be randomly accessible. A sequential read (like with TAR, for
 * instance), is not sufficient for the quick processing required in the application.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
// TODO [low] The multichunk API is really odd; Think of something more sensible 
public abstract class MultiChunker {
    protected int minMultiChunkSize;
    
    /**
     * Creates a new multichunker, and sets the minimum size of a multichunk.
     * 
     * <p>Implementations should react on the minimum multichunk size by allowing
     * at least the given amount of KBs to be written to a multichunk, and declaring
     * a multichunk 'full' if this limit is reached.
     * 
     * @param minMultiChunkSize Minimum multichunk file size in kilo-bytes
     */
    public MultiChunker(int minMultiChunkSize)  {
        this.minMultiChunkSize = minMultiChunkSize;
    }
    
    /**
     * Create a new multichunk in <b>write mode</b>.
     * 
     * <p>Using this method only allows writing to the returned multichunk. The resulting
     * data will be written to the underlying output stream given in the parameter. 
     *   
     * @param id Identifier of the newly created multichunk 
     * @param os Underlying output stream to write the new multichunk to
     * @return Returns a new multichunk object which can only be used for writing 
     * @throws IOException
     */
    public abstract MultiChunk createMultiChunk(byte[] id, OutputStream os) throws IOException;
        
    /**
     * Open existing multichunk in <b>read mode</b> using an underlying input stream.
     * 
     * <p>Using this method only allows reading from the returned multichunk. The underlying
     * input stream is opened and can be used to retrieve chunk data.
     * 
     * @param is InputStream to initialize an existing multichunk for read-operations only
     * @return Returns an existing multichunk object that allows read operations only
     */
    public abstract MultiChunk createMultiChunk(InputStream is);
    
    /**
     * Open existing multichunk in <b>read mode</b> using an underlying file.
     * 
     * <p>Using this method only allows reading from the returned multichunk. The underlying
     * input stream is opened and can be used to retrieve chunk data.
     * 
     * @param is InputStream to initialize an existing multichunk for read-operations only
     * @return Returns an existing multichunk object that allows read operations only
     */    
    public abstract MultiChunk createMultiChunk(File file) throws IOException;
    
    /**
     * Returns a comprehensive string representation of a multichunker
     */
    public abstract String toString();    
}
