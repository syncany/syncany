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
package org.syncany.util.chunk2.meta;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author pheckel
 */
public class NoMetaChunker extends MetaChunker {
    public NoMetaChunker(int sleepMillis) {
        super(0, sleepMillis);
    }

    @Override
    public MetaChunk create(byte[] id, InputStream is) {
        sleep();
        return new NoMetaChunk(id, is);
    }

    @Override
    public MetaChunk create(byte[] id, OutputStream os) throws IOException {
        sleep();
        return new NoMetaChunk(id, os);
    }

    @Override
    public String toString() {
        return "None-"+sleepMillis;
    }
}
