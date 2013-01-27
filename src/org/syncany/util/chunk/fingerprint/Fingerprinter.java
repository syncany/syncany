/*
 * Syncany
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
package org.syncany.util.chunk.fingerprint;

import java.security.NoSuchAlgorithmException;

/**
 *
 * @author pheckel
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
    
    public abstract int getValue();
    public abstract void reset();
    public abstract void roll(byte bt);
    public abstract void check(byte[] buf, int off, int len);
    
    @Override
    public abstract String toString();
}
