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
package org.syncany.util;

/**
 *
 * @author pheckel
 */
public class NumberUtil {
    public static long toLong(byte[] b) {
        long l = 0;
        
        for (int i = 0; i < b.length; i++) {
            l = (l << 8) + (b[i] & 0xff);
        }
        
        return l;
    }
    
    public static byte[] toByteArray(long l) {
        byte[] b = new byte[8];
        
        for (int i = 0; i < b.length; ++i) {
            b[i] = (byte) (l >> (b.length - i - 1 << 3));
        }    
        
        return b;
    }            
}
