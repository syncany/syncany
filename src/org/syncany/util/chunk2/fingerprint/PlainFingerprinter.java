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
package org.syncany.util.chunk2.fingerprint;

/**
 * "Sharing and bandwidth consumption in the low bandwidth file system"
 * A Spiridonov, S Thaker, 2005
 * 
 * @see http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.127.2149&rep=rep1&type=pdf
 * @author pheckel
 */
public class PlainFingerprinter extends Fingerprinter {
    private byte[] block;
    private int len;
    private int pos;
    private int value;

    @Override
    public int getValue() {
        return value;
    }

    @Override
    public void reset() {
        pos = 0;
        value = 0;
        len = 0;        
    }

    @Override
    public void roll(byte bt) {
        value -= block[pos];
        
        block[pos] = bt;        
        value += bt;
        
        pos++;
        
        if (pos == len) {
            pos = 0;
        }
    }

    @Override
    public void check(byte[] buf, int off, int l) {
        block = new byte[l];
        System.arraycopy(buf, off, block, 0, l);
        reset();
        len = block.length;
        pos = 0;
        value = 0;
        
        // Calculate value
        for (byte b : block) {
            value += b;
        }
    }

    @Override
    public String toString() {
        return "PLAIN";
    }
    
}
