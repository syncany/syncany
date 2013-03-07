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
package org.syncany.config;

import org.syncany.Constants;
import org.syncany.chunk.MultiChunk;
import org.syncany.exceptions.CacheException;

import java.io.File;
import java.io.IOException;


/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Cache {
	
  	/**
     * 1 = Cache folder
     * 2 = Chunk file name (defined by {@link CloneChunk#getFileName()})
     */
    private static String CHUNK_FORMAT = "%1$s/enc-%2$s"; 
    private static String PLAIN_CHUNK_FORMAT = "%1$s/plain-%2$s";

    private File cacheDir;
    
    public Cache(File cacheDir) {
    	this.cacheDir = cacheDir;
    }

    public File getMultiChunkFile(MultiChunk multiChunk) {
        return new File(String.format(CHUNK_FORMAT,
            cacheDir.getAbsoluteFile(),
            CloneChunk.getFileName(multiChunk.getId(), null))
        );
    }
    
    public File getMetaChunkFile(byte[] metaChunkId) {
        return new File(String.format(CHUNK_FORMAT,
            cacheDir.getAbsoluteFile(),
            CloneChunk.getFileName(metaChunkId, null))
        );
    }       
    
    public File getPlainChunkFile(byte[] checksum) {
        return new File(String.format(PLAIN_CHUNK_FORMAT,
            cacheDir.getAbsoluteFile(),
            CloneChunk.getFileName(null, checksum))
        );            
    }    
    

    public File createTempFile() throws CacheException {
        return createTempFile("temp");
    }

    public File createTempFile(String name) throws CacheException {
       try {
           return File.createTempFile(
                String.format("temp-%s-", name),
                ".tmp", cacheDir);
       }
       catch (IOException e) {
           throw new CacheException("Unable to create temporary file in cache.", e);
       }
   }
}
