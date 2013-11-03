/*
 * Syncany, www.syncany.org
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
package org.syncany.chunk;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Logger;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class Chunker {
    protected static final Logger logger = Logger.getLogger(Chunker.class.getSimpleName());   

    /**
     * Creates enumeration of Chunks which is filled on demand. 
     * Does not read the whole file at once. 
     * @param file to chunk
     * @return
     * @throws IOException
     */
    public abstract Enumeration<Chunk> createChunks(File file) throws IOException;        
	public abstract void close();         
    public abstract String toString();
    public abstract String getChecksumAlgorithm();
}