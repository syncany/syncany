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
public class ArrayUtil {
    /**
     * Returns true if the array is either null or filled with 
     * zeros.
     * 
     * @param a Array
     * @return 
     */
    public static boolean isEmpty(byte[] a) {
        if (a == null) {
            return true;
        }
        
        for (byte b : a) {
            if (b != 0) {
                return false;
            }            
        }
        
        return true;
    }
}
