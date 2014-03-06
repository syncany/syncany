/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
import java.util.Enumeration;

/**
 * The chunker implements a core part of the deduplication process by breaking
 * files into individual {@link Chunk}s. A chunker emits an enumeration of chunks,
 * allowing the application to process one chunk after the other. 
 * 
 * <p>Note: Implementations should never read the entire file into memory at once,
 *          but instead use an input stream for processing.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class Chunker {	
	/**
	 * Property used by the config to indicate the exact or 
	 * approximate size of a chunk. In bytes. 
	 */
	public static final String PROPERTY_SIZE = "size";
	
    /**
     * Opens the given file and creates enumeration of {@link Chunk}s. This method 
     * should not read the file into memory at once, but instead read and emit new 
     * chunks when requested using {@link Enumeration#nextElement() nextElement()}.
     * 
     * <p>The enumeration must be closed by the {@link ChunkEnumeration#close() close()} 
     * method to remove any possible locks.
     * 
     * @param file The file that is supposed to be chunked
     * @return An enumeration of individual chunks, must be closed at the end of processing
     * @throws IOException If any file exceptions occur
     */	
	public abstract ChunkEnumeration createChunks(File file) throws IOException;
			
	/**
	 * Returns a string representation of the chunker implementation.
	 */
    public abstract String toString();
    
    /**
     * Returns the checksum algorithm used by the chunker to calculate the chunk
     * and file checksums. For the deduplication process to function properly,
     * the checksum algorithms of all chunkers must be equal. 
     */
    public abstract String getChecksumAlgorithm();
    
    /**
     * The chunk enumeration is implemented by the actual chunkers and emits a new
     * chunk when {@link ChunkEnumeration#nextElement() nextElement()} is called. When no more 
     * elements are available, {@link ChunkEnumeration#hasMoreElements() hasMoreElements()} returns
     * false. Any open streams must be closed with {@link ChunkEnumeration#close() close()}.
     */
    public static interface ChunkEnumeration extends Enumeration<Chunk> {
    	/**
    	 * Returns true if the chunker can return at least one more chunk.
    	 */
    	public boolean hasMoreElements();
    	
    	/**
    	 * Returns the next chunk (if there are any). 
    	 */
    	public Chunk nextElement();
    	
    	/**
    	 * Closes the file opened by the {@link Chunker#createChunks(File) createChunks()} method.  
    	 * This method must be called at the end of processing to release any read-/write locks.
    	 */
    	public void close();   
    }
}