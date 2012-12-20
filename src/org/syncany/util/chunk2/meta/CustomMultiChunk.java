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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import org.syncany.util.chunk2.chunking.Chunk;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class CustomMultiChunk extends MultiChunk {
    protected int checksumLength;
    protected DataOutputStream os;
    protected DataInputStream is;
    
    public CustomMultiChunk(InputStream is) {
        super(0);
        this.is = new DataInputStream(is);
        this.checksumLength = -1;
    }
    
    public CustomMultiChunk(byte[] id, int minSize, OutputStream os) {
        super(id, minSize);
        this.os = new DataOutputStream(os);
        this.checksumLength = -1;
    }            

    @Override
    public void close() throws IOException {
        if (os == null) {
            is.close();
        }
        else {
            os.close();
        }
    }    

    @Override
    public void write(Chunk chunk) throws IOException {
        chunks.add(chunk);
        size += chunk.getSize();
        
        // First chunk: write checksum length!
        if (checksumLength == -1) {
        	os.writeByte(this.getId().length);
        	os.write(this.getId());
        	
            checksumLength = chunk.getChecksum().length;
            os.writeByte(chunk.getChecksum().length);
        }
        
        os.write(chunk.getChecksum());

        os.writeShort(chunk.getSize());
        os.write(chunk.getContent(), 0, chunk.getSize());  
    }
    

    @Override
    public Chunk read() throws IOException {
        // First chunk: read checksum length!
        if (checksumLength == -1) {
        	int idLength = is.readByte();
        	this.id = new byte[idLength];
        	is.read(this.id);
        	
            this.checksumLength = is.readByte();
        }
        
        byte[] checksum = new byte[checksumLength];
        is.read(checksum);
        
        int chunkSize = is.readShort();
        byte[] contents = new byte[chunkSize];        
        is.read(contents);
        
        return new Chunk(checksum, contents, chunkSize, checksum);
    }    
}

