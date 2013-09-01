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
package org.syncany.chunk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.syncany.util.StringUtil;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */


public class TarMultiChunk extends MultiChunk {
    private TarArchiveOutputStream tarOut;
    private TarArchiveInputStream tarIn;

    public TarMultiChunk(InputStream is) {
        super(0);
        this.tarIn = new TarArchiveInputStream(is);
    }
    
    public TarMultiChunk(byte[] id, int minSize, OutputStream os) throws IOException {
        super(id, minSize);        
        this.tarOut = new TarArchiveOutputStream(os);
    }            
    
    @Override
    public boolean isFull() {
        return size >= minSize;
    }

    @Override
    public void write(Chunk chunk) throws IOException {
        chunks.add(chunk);
        size += chunk.getSize();
       
        TarArchiveEntry entry = new TarArchiveEntry(StringUtil.toHex(chunk.getChecksum()));
        entry.setSize(chunk.getSize());

        tarOut.putArchiveEntry(entry);
        tarOut.write(chunk.getContent(), 0, chunk.getSize());
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
        
        return new Chunk(StringUtil.fromHex(entry.getName()), content, (int) entry.getSize(), null);
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

