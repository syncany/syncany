/*
 * Syncany, www.syncany.org
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

import org.syncany.util.chunk2.chunking.Chunk;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */


public class TarMetaChunk extends MetaChunk {
    private TarArchiveOutputStream tarOut;
    private TarArchiveInputStream tarIn;
    private Base64 b64;

    public TarMetaChunk(byte[] id, InputStream is) {
        super(id, 0);
        this.tarIn = new TarArchiveInputStream(is);
    }
    
    public TarMetaChunk(byte[] id, int minSize, OutputStream os) throws IOException {
        super(id, minSize);
        
        this.tarOut = new TarArchiveOutputStream(os);
        this.b64 = new Base64();
    }            
    
    @Override
    public boolean isFull() {
        return size >= minSize;
    }

    @Override
    public void write(Chunk chunk) throws IOException {
        chunks.add(chunk);
        size += chunk.getSize();
       
        TarArchiveEntry entry = new TarArchiveEntry(b64.encodeAsString(chunk.getChecksum()).replace("=", ""));
        entry.setSize(chunk.getSize());

        tarOut.putArchiveEntry(entry);
        tarOut.write(chunk.getContents(), 0, chunk.getSize());
        tarOut.closeArchiveEntry();
    }        
    
    @Override
    public Chunk read() throws IOException {
        ArchiveEntry entry = tarIn.getNextEntry();

        if (entry == null) {
            return null;
        }
        
        byte[] content = new byte[(int) entry.getSize()];
        int read; int pos = 0;
        
        while (pos < content.length && -1 != (read = tarIn.read())) {
            content[pos++] = (byte) (0xff & read);
        }
                
        return new Chunk(b64.decode(entry.getName()), content, (int) entry.getSize(), null);
    }
   

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public void close() throws IOException {
        if (tarOut != null) {
            tarOut.close();
        }
        else {
            tarIn.close();
        }
    }    
}

