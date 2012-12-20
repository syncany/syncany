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
package org.syncany.util.chunk2.transform.compress;

import org.syncany.util.chunk2.transform.Transformer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

/**
 *
 * @author pheckel
 */
public class Bzip2Compressor extends Transformer {
    private int level;
    
    public Bzip2Compressor() {
        this(null);
    }

    public Bzip2Compressor(Transformer nextTransformer) {
        super(nextTransformer);
    }    
        
    @Override
    public OutputStream transform(OutputStream out) throws IOException {
        if (nextTransformer == null) {
            return new BZip2CompressorOutputStream(out);
        }
        else {
            return new BZip2CompressorOutputStream(nextTransformer.transform(out));
        }
    }

    @Override
    public InputStream transform(InputStream in) throws IOException {
        if (nextTransformer == null) {
            return new BZip2CompressorInputStream(in);
        }
        else {
            return new BZip2CompressorInputStream(nextTransformer.transform(in));
        }
    }

    @Override
    public String toString() {
        return (nextTransformer == null) ? "Bzip2" : "Bzip2-"+nextTransformer;
    }

}
