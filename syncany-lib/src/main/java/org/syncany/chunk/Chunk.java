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
 * A chunk represents a certain part of a file. It is created during the
 * deduplication process by a {@link Chunker}. 
 * 
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
public class Chunk {
    private byte[] checksum;
    private byte[] contents;
    private int size;
    private byte[] fileChecksum;

    /*package*/ Chunk(byte[] checksum, byte[] contents, int size, byte[] fileChecksum) {
        this.checksum = checksum;
        this.contents = contents;
        this.size = size;
        this.fileChecksum = fileChecksum;
    }

    public byte[] getChecksum() {
        return checksum;
    }

    public byte[] getContent() {
        return contents;
    }

    public byte[] getFileChecksum() {
        return fileChecksum;
    }

    public int getSize() {
        return size;
    }                
}