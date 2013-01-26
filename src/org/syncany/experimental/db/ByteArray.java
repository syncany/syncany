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
package org.syncany.experimental.db;

/**
 *
 * @author pheckel
 */
import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.io.IOException;

/**
ByteArray wraps java byte arrays (byte[]) to allow
byte arrays to be used as keys in hashtables.

This is required because the equals function on
byte[] directly uses reference equality.

This class also allows the trio of array, offset and length
to be carried around as a single object.
 */
public final class ByteArray {

    private byte[] array;
    private int offset;
    private int length;

    /**
    Create an instance of this class that wraps ths given array.
    This class does not make a copy of the array, it just saves
    the reference.
     */
    public ByteArray(byte[] array, int offset, int length) {
        this.array = array;
        this.offset = offset;
        this.length = length;
    }

    public ByteArray(byte[] array) {
        this(array, 0, array.length);
    }

    public ByteArray() {
    }

    public void setBytes(byte[] array) {
        this.array = array;
        offset = 0;
        length = array.length;
    }

    public void setBytes(byte[] array, int length) {
        this.array = array;
        this.offset = 0;
        this.length = length;
    }

    public void setBytes(byte[] array, int offset, int length) {
        this.array = array;
        this.offset = offset;
        this.length = length;
    }

    /**
    Value equality for byte arrays.
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof ByteArray) {
            ByteArray ob = (ByteArray) other;
            return ByteArray.equals(array, offset, length, ob.array, ob.offset, ob.length);
        }
        return false;
    }

    /**
     */
    @Override
    public int hashCode() {

        byte[] larray = array;

        int hash = length;
        for (int i = 0; i < length; i++) {
            hash += larray[i + offset];
        }
        return hash;
    }

    public final byte[] getArray() {
        return array;
    }

    public final int getOffset() {
        return offset;
    }

    public final int getLength() {
        return length;
    }

    public final void setLength(int newLength) {
        length = newLength;
    }

    /**
     * Read this object from a stream of stored objects.
     *
     * @param in read this.
     *
     * @exception IOException         thrown on error
     */
    public void readExternal(ObjectInput in) throws IOException {
        int len = length = in.readInt();
        offset = 0;
        array = new byte[len];

        in.readFully(array, 0, len);
    }

    /**
     * Write the byte array out w/o compression
     *
     * @param out write bytes here.
     *
     * @exception IOException   thrown on error
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(length);
        out.write(array, offset, length);
    }

    /**
    Compare two byte arrays using value equality.
    Two byte arrays are equal if their length is
    identical and their contents are identical.
     */
    private static boolean equals(byte[] a, int aOffset, int aLength, byte[] b, int bOffset, int bLength) {

        if (aLength != bLength) {
            return false;
        }

        for (int i = 0; i < aLength; i++) {
            if (a[i + aOffset] != b[i + bOffset]) {
                return false;
            }
        }
        return true;
    }
}