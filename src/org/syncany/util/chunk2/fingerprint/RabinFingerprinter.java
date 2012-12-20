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
 * Rolling fingerprinter based on the Rabin's algorithm. 
 * 
 * <p>The code is almost entirely base on Sean Owen's code:
 * http://codesearch.google.com/#ZHqR5cFzsUY/trunk/rabin-hash-function-2.0/src/main/com/planetj/math/rabinhash/RabinHashFunction32.java&type=cs
 * 
 * <p>It has been modified to facilitate easy rolling of single bytes. 
 * Bytes are treated as entire integers, because this is the fastest method.
 * 
 * @author Sean Owen
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @version 2.0
 * @since 2.0
 */
public class RabinFingerprinter extends Fingerprinter {
    /** Represents x<sup>32</sup> + x<sup>7</sup> + x<sup>3</sup> + x<sup>2</sup> + 1. */
    private static final int DEFAULT_IRREDUCIBLE_POLY = 0x0000008D;
    private static final int P_DEGREE = 32;
    private static final int X_P_DEGREE = 1 << (P_DEGREE - 1);

    private final int P;
    private transient int[] table32, table40, table48, table56;
    
    private int value;

    public RabinFingerprinter() {
        this(DEFAULT_IRREDUCIBLE_POLY);
    }   
    
    public RabinFingerprinter(final int P) {
        this.P = P;
        this.value = 0;
        initializeTables();
    }    
    
    private void initializeTables() {
        final int[] mods = new int[P_DEGREE];

        // We want to have mods[i] == x^(P_DEGREE+i)
        mods[0] = P;
        for (int i = 1; i < P_DEGREE; i++) {
            final int lastMod = mods[i - 1];
            // x^i == x(x^(i-1)) (mod P)
            int thisMod = lastMod << 1;
            // if x^(i-1) had a x_(P_DEGREE-1) term then x^i has a
            // x^P_DEGREE term that 'fell off' the top end.
            // Since x^P_DEGREE == P (mod P), we should add P
            // to account for this:
            if ((lastMod & X_P_DEGREE) != 0) {
                thisMod ^= P;
            }
            mods[i] = thisMod;
        }

        // Let i be a number between 0 and 255 (i.e. a byte).
        // Let its bits be b0, b1, ..., b7.
        // Let Q32 be the polynomial b0*x^39 + b1*x^38 + ... + b7*x^32 (mod P).
        // Then table32[i] is Q32, represented as an int (see below).
        // Likewise Q40 be the polynomial b0*x^47 + b1*x^46 + ... + b7*x^40 (mod P).
        // table40[i] is Q40, represented as an int. Likewise table48 and table56.

        table32 = new int[256];
        table40 = new int[256];
        table48 = new int[256];
        table56 = new int[256];

        for (int i = 0; i < 256; i++) {
            int c = i;
            for (int j = 0; j < 8 && c > 0; j++) {
                if ((c & 1) != 0) {
                    table32[i] ^= mods[j];
                    table40[i] ^= mods[j + 8];
                    table48[i] ^= mods[j + 16];
                    table56[i] ^= mods[j + 24];
                }
                c >>>= 1;
            }
        }
    }    
    
    private int computeWShifted(final int w) {
        return table32[w & 0xFF] ^
               table40[(w >>> 8) & 0xFF] ^
               table48[(w >>> 16) & 0xFF] ^
               table56[(w >>> 24) & 0xFF];
    }    
    
    private int hash(final byte[] A, final int offset, final int length, int w) {
        for (int s = offset; s < offset+length; s++) {
            w = computeWShifted(w) ^ A[s];
        }

        return w;
    }
      
    @Override
    public int getValue() {        
        return value;

        // Convert to integer value
        // cmp. http://codesearch.google.com/#ZHqR5cFzsUY/trunk/rabin-hash-function-2.0/src/main/com/planetj/math/rabinhash/RHF32.java&type=cs
        /*int value = (0xff & hash[0]) << 24
            | (0xff & hash[1]) << 16
            | (0xff & hash[2]) << 8
            | (0xff & hash[3]);
        
        System.out.println("val = "+value);
        return value;*/
    }

    @Override
    public void reset() {
        value = 0;
    }

    @Override
    public void roll(byte bt) {
        value = hash(new byte[]{bt}, 0, 1, value);
    }

    @Override
    public void check(byte[] buf, int off, int len) {
        value = hash(buf, off, len, 0);
    }

    @Override
    public String toString() {
        return "Rabin";
    }
    
}
