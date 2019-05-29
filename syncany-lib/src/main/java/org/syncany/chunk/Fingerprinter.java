/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import java.security.NoSuchAlgorithmException;

/**
 * A fingerprinter is used in content based {@link Chunker}s to determine at which  
 * byte to break a file into {@link Chunk}s. 
 * 
 * <p>Implementations should make sure that the underlying algorithm is fast, because
 * the {@link #roll(byte) roll()}-method is called for each byte of a file. It
 * should rely on a rolling checksum algorithm (also: rolling hash) to reach optimal
 * performance.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Rolling_hash">http://en.wikipedia.org/wiki/Rolling_hash</a>
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
public abstract class Fingerprinter {    
    public static Fingerprinter getInstance(String name) throws NoSuchAlgorithmException {
        try {
            Class<?> clazz = Class.forName(Fingerprinter.class.getPackage().getName()+"."+name+Fingerprinter.class.getSimpleName());
            return (Fingerprinter) clazz.newInstance();
        }
        catch (Exception e) {
            throw new NoSuchAlgorithmException("No such fingerprinting algorithm: "+name, e);
        }        
    }
    
    /**
     * Return the value of the currently computed checksum.
     * @return The currently computed checksum.
     */
    public abstract int getValue();
    
    /**
     * Reset the checksum.
     */
    public abstract void reset();
    
    /**
     * "Roll" the checksum, i.e. update the underlying 
     * rolling checksum by the given content byte.
     *
     * @param bt The next byte.
     */
    public abstract void roll(byte bt);
    
    /**
     * Update the checksum with an entirely different block, and
     * potentially a different block length.
     *
     * @param buf The byte array that holds the new block.
     * @param off From whence to begin reading.
     * @param len The length of the block to read.
     */    
    public abstract void check(byte[] buf, int off, int len);
    
    /**
	 * Returns a string representation of the fingerprinter
	 * implementation.
	 */
    public abstract String toString();
}
