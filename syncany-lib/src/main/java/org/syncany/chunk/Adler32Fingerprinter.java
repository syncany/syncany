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

/**
 * A simple 32-bit "rolling" checksum. This checksum algorithm is based
 * upon the algorithm outlined in the paper "The rsync algorithm" by
 * Andrew Tridgell and Paul Mackerras. The algorithm works in such a way
 * that if one knows the sum of a block
 * <em>X<sub>k</sub>...X<sub>l</sub></em>, then it is a simple matter to
 * compute the sum for <em>X<sub>k+1</sub>...X<sub>l+1</sub></em>.
 *
 * <p>The class has been adapted to work with the Syncany chunking classes.
 *
 * @author Casey Marshall
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @version $Revision: 188 $
 */
public class Adler32Fingerprinter extends Fingerprinter {
	protected final int charOffset;

	/**
	 * The first half of the checksum.
	 */
	protected int a;

	/**
	 * The second half of the checksum.
	 */
	protected int b;

	/**
	 * The place from whence the current checksum has been computed.
	 */
	protected int pos;

	/**
	 * The place to where the current checksum has been computed.
	 */
	protected int len;

	/**
	 * The block from which the checksum is computed.
	 */
	protected byte[] block;

	/**
	 * The index in {@link #newBlock} where the newest byte has
	 * been stored.
	 */
	protected int newIndex;

	/**
	 * The block that is receiving new input.
	 */
	protected byte[] newBlock;

	/**
	 * Creates a new rolling checksum. The <i>charOffset</i> argument
	 * affects the output of this checksum; rsync uses a char offset of
	 * 0, librsync 31.
	 */
	public Adler32Fingerprinter(int charOffset) {
		this.charOffset = charOffset;
		a = b = 0;
		pos = 0;
	}

	public Adler32Fingerprinter() {
		this(0);
	}

	@Override
	public int getValue() {
		return (a & 0xffff) | (b << 16);
	}

	@Override
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
	 */
	@Override
	public void roll(byte bt) {
		a -= block[pos] + charOffset;
		b -= len * (block[pos] + charOffset);
		a += bt + charOffset;
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
		a -= block[pos % block.length] + charOffset;
		b -= len * (block[pos % block.length] + charOffset);
		pos++;
		len--;
	}

	@Override
	public void check(byte[] buf, int off, int len) {
		block = new byte[len];
		System.arraycopy(buf, off, block, 0, len);
		reset();
		this.len = block.length;
		int i;

		for (i = 0; i < block.length - 4; i += 4) {
			b += 4 * (a + block[i]) + 3 * block[i + 1]
					+ 2 * block[i + 2] + block[i + 3] + 10 * charOffset;
			a += block[i] + block[i + 1] + block[i + 2]
					+ block[i + 3] + 4 * charOffset;
		}
		for (; i < block.length; i++) {
			a += block[i] + charOffset;
			b += a;
		}
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + a;
		result = prime * result + b;
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o instanceof Adler32Fingerprinter) {
			return ((Adler32Fingerprinter) o).a == a && ((Adler32Fingerprinter) o).b == b;
		}
		return false;
	}

	@Override
	public String toString() {
		return "Adler32";
	}
}