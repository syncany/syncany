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
package org.syncany.util.chunk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.syncany.util.StringUtil;
import org.syncany.util.io.CipherInputStreamUtils;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class CustomMultiChunk extends MultiChunk {
    protected int checksumLength;
    protected DataOutputStream os;
    protected DataInputStream is;
    
    public CustomMultiChunk(byte[] id, InputStream is) {
        super(id, 0);
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
            actualSize = os.size();
            os.close();
        }
    }    

    @Override
    public void write(Chunk chunk) throws IOException {
        chunks.add(chunk);
        chunkSize += chunk.getSize();
        
        // First chunk: write checksum length!
        if (checksumLength == -1) {
            checksumLength = chunk.getChecksum().length;
            os.writeByte(chunk.getChecksum().length);
        }
        
        os.write(chunk.getChecksum());
        os.writeShort(chunk.getSize());
        
        os.write(chunk.getContents(), 0, chunk.getSize());
        
       // System.out.println(StringUtil.toHex(chunk.getContents()));
    }
    
    @Override
    public Chunk read() throws IOException {
    	// First chunk: read checksum length!
        if (checksumLength == -1) {
            checksumLength = is.readByte();
        }
        
        try{
        	byte[] checksum = new byte[checksumLength];
        	// changed due to wrong behavior of standard methods
        	//is.read(checksum);
        	CipherInputStreamUtils.read(checksum, is);
        	
        	int chunkSize = is.readShort() & 0xffff;
            byte[] contents = new byte[chunkSize]; 
            // changed due to wrong behavior of standard methods
            //is.read(contents, 0, chunkSize);
            CipherInputStreamUtils.read(contents, 0, chunkSize, is);
            
           // System.out.println(StringUtil.toHex(contents));
            
            return new Chunk(checksum, contents, chunkSize, checksum);
        } catch(EOFException ex){
        	return null;
        }
    }    
}

