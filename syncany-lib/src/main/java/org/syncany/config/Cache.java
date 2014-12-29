/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.database.MultiChunkEntry.MultiChunkId;

/**
 * The cache class represents the local disk cache. It is used for storing multichunks
 * or other metadata files before upload, and as a download location for the same
 * files. 
 * 
 * <p>The cache implements an LRU strategy based on the last modified date of the 
 * cached files. When files are accessed using the respective getters, the last modified
 * date is updated. Using the {@link #clear()}/{@link #clear(long)} method, the cache
 * can be cleaned.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Cache {
	private static final Logger logger = Logger.getLogger(Cache.class.getSimpleName());

    private static long DEFAULT_CACHE_KEEP_BYTES = 500*1024*1024;
	private static String FILE_FORMAT_MULTICHUNK_ENCRYPTED = "multichunk-%s";
	private static String FILE_FORMAT_MULTICHUNK_DECRYPTED = "multichunk-%s-decrypted";
    private static String FILE_FORMAT_DATABASE_FILE_ENCRYPTED = "%s";
    
    private long keepBytes;
    private File cacheDir;
    
    public Cache(File cacheDir) {
    	this.cacheDir = cacheDir;
    	this.keepBytes = DEFAULT_CACHE_KEEP_BYTES;
    }
    
    /**
     * Returns a file path of a decrypted multichunk file, 
     * given the identifier of a multichunk.
     */
    public File getDecryptedMultiChunkFile(MultiChunkId multiChunkId) {
    	return getFileInCache(FILE_FORMAT_MULTICHUNK_DECRYPTED, multiChunkId.toString());
    }    

    /**
     * Returns a file path of a encrypted multichunk file, 
     * given the identifier of a multichunk.
     */
    public File getEncryptedMultiChunkFile(MultiChunkId multiChunkId) {
    	return getFileInCache(FILE_FORMAT_MULTICHUNK_ENCRYPTED, multiChunkId.toString());
    }    
    
    /**
     * Returns a file path of a database remote file.
     */
	public File getDatabaseFile(String name) { // TODO [low] This shoule be a database file or another key
		return getFileInCache(FILE_FORMAT_DATABASE_FILE_ENCRYPTED, name);		
	}    

	public long getKeepBytes() {
		return keepBytes;
	}

	public void setKeepBytes(long keepBytes) {
		this.keepBytes = keepBytes;
	}

	/**
	 * Deletes files in the the cache directory using a LRU-strategy until <tt>keepBytes</tt>
	 * bytes are left. This method calls {@link #clear(long)} using the <tt>keepBytes</tt> 
	 * property.
	 * 
	 * <p>This method should not be run while an operation is executed.
	 */
	public void clear() {
		clear(keepBytes);
	}
	
	/**
	 * Deletes files in the the cache directory using a LRU-strategy until <tt>keepBytes</tt>
	 * bytes are left.
	 * 
	 * <p>This method should not be run while an operation is executed.
	 */
	public void clear(long keepBytes) {		
		List<File> cacheFiles = getSortedFileList();
			
		// Determine total size
		long totalSize = 0;
		
		for (File cacheFile : cacheFiles) {
			totalSize += cacheFile.length();
		}
		
		// Delete until total cache size <= keep size
		if (totalSize > keepBytes) {
			logger.log(Level.INFO, "Cache too large (" + (totalSize/1024) + " KB), deleting until <= " + (keepBytes/1024/1024) + " MB ...");

			while (totalSize > keepBytes && cacheFiles.size() > 0) {
				File eldestCacheFile = cacheFiles.remove(0);
				
				long fileSize = eldestCacheFile.length();
				long fileLastModified = eldestCacheFile.lastModified();
				
				logger.log(Level.INFO, "- Deleting from cache (" + new Date(fileLastModified) + ", " + (fileSize/1024) + " KB): " + eldestCacheFile.getName());
				
				totalSize -= fileSize;
				eldestCacheFile.delete();				
			}
		}
		else {
			logger.log(Level.INFO, "Cache size okay (" + (totalSize/1024) + " KB), no need to clean (keep size is " + (keepBytes/1024/1024) + " MB)");
		}
	}

	/**
	 * Creates temporary file in the local directory cache, typically located at
	 * .syncany/cache. If not deleted by the application, the returned file is automatically
	 * deleted on exit by the JVM.
	 * 
	 * @return Temporary file in local directory cache
	 */
    public File createTempFile(String name) throws IOException {
       File tempFile = File.createTempFile(String.format("temp-%s-", name), ".tmp", cacheDir);
       tempFile.deleteOnExit();
       
       return tempFile;
    }
    
    /**
     * Returns the file using the given format and parameters, and 
     * updates the last modified date of the file (used for LRU strategy).
     */
    private File getFileInCache(String format, Object... params) {
        File fileInCache = new File(cacheDir.getAbsoluteFile(), String.format(format, params));

        if (fileInCache.exists()) {
        	touchFile(fileInCache);
        }
        
        return fileInCache;
    }
    
    /**
     * Sets the last modified date of the given file to the current date/time.
     */
    private void touchFile(File fileInCache) {
    	fileInCache.setLastModified(System.currentTimeMillis());
    }
    
    /**
     * Returns a list of all files in the cache, sorted by the last modified
     * date -- eldest first.
     */
	private List<File> getSortedFileList() {
		File[] cacheFilesList = cacheDir.listFiles();
		List<File> sortedCacheFiles = new ArrayList<File>();
		
		if (cacheFilesList != null) {
			sortedCacheFiles.addAll(Arrays.asList(cacheFilesList));
			
			Collections.sort(sortedCacheFiles, new Comparator<File>() {
				@Override
				public int compare(File file1, File file2) {				
					return Long.compare(file1.lastModified(), file2.lastModified());
				}
			});
		}
		
		return sortedCacheFiles;
	}
}
