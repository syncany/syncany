/**
 * A simple 32-bit "rolling" checksum. This checksum algorithm is based
 * upon the algorithm outlined in the paper "The rsync algorithm" by
 * Andrew Tridgell and Paul Mackerras. The algorithm works in such a way
 * that if one knows the sum of a block
 * <em>X<sub>k</sub>...X<sub>l</sub></em>, then it is a simple matter to
 * compute the sum for <em>X<sub>k+1</sub>...X<sub>l+1</sub></em>.
 *
 * @author Casey Marshall
 * @version $Revision: 188 $
 */
package org.syncany.util.chunk.fingerprint;

public class Adler32Fingerprinter extends Fingerprinter {
    protected final int char_offset;
    /**
     * The first half of the checksum.
     *
     * @since 1.1
     */
    protected int a;
    /**
     * The second half of the checksum.
     *
     * @since 1.1
     */
    protected int b;
    /**
     * The place from whence the current checksum has been computed.
     *
     * @since 1.1
     */
    protected int pos;
    /**
     * The place to where the current checksum has been computed.
     *
     * @since 1.1
     */
    protected int len;
    /**
     * The block from which the checksum is computed.
     *
     * @since 1.1
     */
    protected byte[] block;
    /**
     * The index in {@link #new_block} where the newest byte has
     * been stored.
     *
     * @since 1.1
     */
    protected int new_index;
    /**
     * The block that is recieving new input.
     *
     * @since 1.1
     */
    protected byte[] new_block;

// Constructors.
    // -----------------------------------------------------------------
    /**
     * Creates a new rolling checksum. The <i>char_offset</i> argument
     * affects the output of this checksum; rsync uses a char offset of
     * 0, librsync 31.
     */
    public Adler32Fingerprinter(int char_offset) {
        this.char_offset = char_offset;
        a = b = 0;
        pos = 0;
    }

    public Adler32Fingerprinter() {
        this(0);
    }

    // Public instance methods.
    // -----------------------------------------------------------------
    /**
     * Return the value of the currently computed checksum.
     *
     * @return The currently computed checksum.
     * @since 1.1
     */
    public int getValue() {
        return (a & 0xffff) | (b << 16);
    }

    /**
     * Reset the checksum.
     *
     * @since 1.1
     */
    public void reset() {
        pos = 0;
        a = b = 0;
        len = 0;
    }

    /**
     * "Roll" the checksum. This method takes a single byte as byte
     * <em>X<sub>l+1</sub></em>, and recomputes the checksum for
     * <em>X<sub>k+1</sub>...X<sub>l+1</sub></em>. This is the
     * preferred method for updating the checksum.
     *
     * @param bt The next byte.
     * @since 1.1
     */
    public void roll(byte bt) {
        a -= block[pos] + char_offset;
        b -= len * (block[pos] + char_offset);
        a += bt + char_offset;
        b += a;
        block[pos] = bt;
        pos++;
        if (pos == len) {
            pos = 0;
        }
    }

    /**
     * Update the checksum by trimming off a byte only, not adding
     * anything.
     */
    public void trim() {
        a -= block[pos % block.length] + char_offset;
        b -= len * (block[pos % block.length] + char_offset);
        pos++;
        len--;
    }

    /**
     * Update the checksum with an entirely different block, and
     * potentially a different block length.
     *
     * @param buf The byte array that holds the new block.
     * @param off From whence to begin reading.
     * @param len The length of the block to read.
     * @since 1.1
     */
    public void check(byte[] buf, int off, int len) {
        block = new byte[len];
        System.arraycopy(buf, off, block, 0, len);
        reset();
        this.len = block.length;
        int i;

        for (i = 0; i < block.length - 4; i += 4) {
            b += 4 * (a + block[i]) + 3 * block[i + 1]
                    + 2 * block[i + 2] + block[i + 3] + 10 * char_offset;
            a += block[i] + block[i + 1] + block[i + 2]
                    + block[i + 3] + 4 * char_offset;
        }
        for (; i < block.length; i++) {
            a += block[i] + char_offset;
            b += a;
        }
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new Error();
        }
    }

    @Override
    public boolean equals(Object o) {
        return ((Adler32Fingerprinter) o).a == a && ((Adler32Fingerprinter) o).b == b;
    }

    @Override
    public String toString() {
        return "Adler32";
    }
}