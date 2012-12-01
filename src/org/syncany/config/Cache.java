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
import org.syncany.db.CloneChunk;
import java.io.File;
import java.io.IOException;
import org.syncany.util.chunk.MultiChunk;
import org.syncany.util.exceptions.CacheException;

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

    private int size;
    private File folder;
    
    public Cache(){
    	this.folder = Settings.getInstance().getAppCacheDir();
    	this.size = Constants.DEFAULT_CACHE_SIZE;
    }

    public void setFolder(File folder) {
        this.folder = folder;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public File getFolder() {
        return folder;
    }

    public int getSize() {
        return size;
    }

    // TODO: not used anymore?
//    public File getCacheChunk(CloneChunk chunk) {
//        return new File(String.format(CHUNK_FORMAT,
//            folder.getAbsoluteFile(),
//            chunk.getFileName())
//        );
//    }
//    
//    public File getCacheChunk(String chunkIdStr) {
//        return new File(String.format(CHUNK_FORMAT,
//            folder.getAbsoluteFile(),
//            CloneChunk.getFileName(chunkIdStr))
//        );
//    }    
    
    public File getMetaChunkFile(MultiChunk metaChunk) {
        return new File(String.format(CHUNK_FORMAT,
            folder.getAbsoluteFile(),
            CloneChunk.getFileName(metaChunk.getId(), null))
        );
    }
    
    public File getMetaChunkFile(byte[] metaChunkId) {
        return new File(String.format(CHUNK_FORMAT,
            folder.getAbsoluteFile(),
            CloneChunk.getFileName(metaChunkId, null))
        );
    }       
    
    public File getPlainChunkFile(byte[] checksum) {
        return new File(String.format(PLAIN_CHUNK_FORMAT,
            folder.getAbsoluteFile(),
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
                ".tmp", folder);
       }
       catch (IOException e) {
           throw new CacheException("Unable to create temporary file in cache.", e);
       }
   }
}
