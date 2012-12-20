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
package org.syncany.util.chunk2.chunking.cbc;

import org.syncany.util.chunk2.chunking.Chunk;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pheckel
 */
public class JpegChunkEnumeration implements Enumeration<Chunk> {
    protected static final Logger logger = Logger.getLogger(JpegChunkEnumeration.class.getSimpleName());   
    
    private InputStream in;           
    private byte[] buffer;
    private boolean closed;

    public JpegChunkEnumeration(InputStream in) {
        this.in = in;
        this.buffer = new byte[100*1024];
        this.closed = false;
    }
    
    @Override
    public boolean hasMoreElements() {
        if (closed) {
            return false;
        }

        try {
            //System.out.println("fis ="+fis.available());
            return in.available() > 0;
        }
        catch (IOException ex) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Error while reading from file input stream.", ex);
            }

            return false;
        }
    }

    @Override
    public Chunk nextElement() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
