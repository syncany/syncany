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
package org.syncany.config;

import java.io.File;
import java.io.IOException;

import org.syncany.util.StringUtil;


/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Cache {
	private static String FILE_FORMAT_MULTICHUNK_ENCRYPTED = "multichunk-%s";
	private static String FILE_FORMAT_MULTICHUNK_DECRYPTED = "multichunk-%s-decrypted";
    private static String FILE_FORMAT_DATABASE_FILE_ENCRYPTED = "%s";

    private File cacheDir;
    
    public Cache(File cacheDir) {
    	this.cacheDir = cacheDir;
    }

    public File getEncryptedMultiChunkFile(byte[] multiChunkId) {
    	return getFileInCache(FILE_FORMAT_MULTICHUNK_ENCRYPTED, StringUtil.toHex(multiChunkId));
    }
    
    public File getDecryptedMultiChunkFile(byte[] multiChunkId) {
    	return getFileInCache(FILE_FORMAT_MULTICHUNK_DECRYPTED, StringUtil.toHex(multiChunkId));
    }    
    
	public File getDatabaseFile(String name) {
		return getFileInCache(FILE_FORMAT_DATABASE_FILE_ENCRYPTED, name);		
	}    

    public File createTempFile() throws Exception {
        return createTempFile("temp");
    }

    public File createTempFile(String name) throws Exception {
       try {
           return File.createTempFile(String.format("temp-%s-", name), ".tmp", cacheDir);
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
