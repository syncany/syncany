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
package org.syncany.config;

import java.io.File;
import java.io.IOException;

import org.syncany.database.MultiChunkEntry.MultiChunkId;

/**
 * The cache class represents the local disk cache. It is used for storing multichunks
 * or other metadata files before upload, and as a download location for the same
 * files. 
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
// TODO [low] Cache: maybe make a more sensible structure, add timestamp?! LRU cache?!
public class Cache {
	private static String FILE_FORMAT_MULTICHUNK_ENCRYPTED = "multichunk-%s";
	private static String FILE_FORMAT_MULTICHUNK_DECRYPTED = "multichunk-%s-decrypted";
    private static String FILE_FORMAT_DATABASE_FILE_ENCRYPTED = "%s";

    private File cacheDir;
    
    public Cache(File cacheDir) {
    	this.cacheDir = cacheDir;
    }
    
    public File getDecryptedMultiChunkFile(MultiChunkId multiChunkId) {
    	return getFileInCache(FILE_FORMAT_MULTICHUNK_DECRYPTED, multiChunkId.toString());
    }    

    public File getEncryptedMultiChunkFile(MultiChunkId multiChunkId) {
    	return getFileInCache(FILE_FORMAT_MULTICHUNK_ENCRYPTED, multiChunkId.toString());
    }    
    
	public File getDatabaseFile(String name) {
		return getFileInCache(FILE_FORMAT_DATABASE_FILE_ENCRYPTED, name);		
	}    

	/**
	 * Deletes all files in the cache directory. This method should not be run 
	 * while an operation is executed, but only while no operation is run. 
	 */
	public void clear() {
		File[] cacheFiles = cacheDir.listFiles();
		
		if (cacheFiles != null) {
			for (File cacheFile : cacheFiles) {
				cacheFile.delete();				
			}
		}
	}
	
	/**
	 * Creates temporary file in the local directory cache, typically located at
	 * .syncany/cache. If not deleted by the application, the returned file is automatically
	 * deleted on exit by the JVM.
	 * 
	 * @return Temporary file in local directory cache
	 */
    public File createTempFile(String name) throws Exception {
       try {
           File tempFile = File.createTempFile(String.format("temp-%s-", name), ".tmp", cacheDir);
           tempFile.deleteOnExit();
           
           return tempFile;
       }
       catch (IOException e) {
           throw new Exception("Unable to create temporary file in cache.", e);
       }
    }
    
    private File getFileInCache(String format, Object... params) {
        return new File(
    		cacheDir.getAbsoluteFile()+File.separator+
    		String.format(format, params)
        );
    }
}
