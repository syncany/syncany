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
package org.syncany.chunk;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author pheckel
 */
public class GzipCompressor extends Transformer {
    private int level;
    
    public GzipCompressor() {
        this(Deflater.DEFAULT_COMPRESSION, null);
    }

    public GzipCompressor(int level) {
        this(level, null);
    }
    
    public GzipCompressor(Transformer nextTransformer) {
        this(Deflater.DEFAULT_COMPRESSION, nextTransformer);
    }
    
    public GzipCompressor(int level, Transformer nextTransformer) {
        super(nextTransformer);
        this.level = level;
    }
        
    @Override
    public OutputStream transform(OutputStream out) throws IOException {
        if (nextTransformer == null) {
            return new GZIPOutputStreamEx(out, level);
        }
        else {
            return new GZIPOutputStreamEx(nextTransformer.transform(out), level);
        }
    }

    @Override
    public InputStream transform(InputStream in) throws IOException {
        if (nextTransformer == null) {
            return new GZIPInputStream(in);
        }
        else {
            return new GZIPInputStream(nextTransformer.transform(in));
        }
    }
    
    public static class GZIPOutputStreamEx extends GZIPOutputStream {
        /**
         * Level is 1-9 -- 1 being best speed, and 9 being best compression
         */
        public GZIPOutputStreamEx(OutputStream out, int level) throws IOException {
            super(out);
            def.setLevel(level);
        }
    }    
    
    @Override
    public String toString() {
        return (nextTransformer == null) ? "Gzip" : "Gzip-"+nextTransformer;
    }
    
}
