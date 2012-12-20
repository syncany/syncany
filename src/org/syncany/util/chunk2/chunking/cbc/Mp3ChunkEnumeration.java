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
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.syncany.util.StringUtil;

/**
 *
 * @author pheckel
 */
public class Mp3ChunkEnumeration implements Enumeration<Chunk> {
    protected static final Logger logger = Logger.getLogger(Mp3ChunkEnumeration.class.getSimpleName());   
    
    private InputStream in;           
    private byte[] buffer;
    private boolean closed;
    private boolean headerdone;
    private MessageDigest digest;
    
    public Mp3ChunkEnumeration(InputStream in, MessageDigest digest) {
        this.in = in;
        this.buffer = new byte[100*1024];
        this.closed = false;
        this.headerdone = false;
        this.digest = digest;
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
        
        try {
            // MP3 header in an extra chunk
            if (!headerdone) {            
                int read = in.read(buffer, 0, 10);

                
                // TODO exception handling
                
                
                // 00 00 1f 76 -> -001 1111 -111 0110 --[unsynch]--> 0000 1111 1111 0110
                //System.out.println(StringUtil.toHex(new byte[] {buffer[6], buffer[7], buffer[8], buffer[9]}));
                // Bytes 6-10 are the tag header length minus 10, in a synched form (= bit 7 is zero)
                // http://en.wikipedia.org/wiki/Synchsafe + http://www.id3.org/id3v2.3.0
                int headerLength = 10 + unsynchsafe(
                      (buffer[9] & 0xff)
                    | (buffer[8] & 0xff) << 8
                    | (buffer[7] & 0xff) << 16
                    | (buffer[6] & 0xff) << 24
                );                

                read = in.read(buffer, 11, headerLength-10);
                
                digest.reset();
                digest.update(buffer, 0, headerLength);
                
                headerdone = true;                
                return new Chunk(digest.digest(), buffer, headerLength, null);
            }

            // Payload chunked in fixed pieces
            else {
                
                int read = in.read(buffer);

                if (read == -1) {
                    return null;
                }

                // Close if this was the last bytes
                if (in.available() == 0) {
                    in.close();
                    closed = true;
                }

                //byte[] chunkContents = (read == buffer.length) ? buffer : Arrays.copyOf(buffer, read);

                digest.reset();
                digest.update(buffer, 0, read);            

                return new Chunk(digest.digest(), buffer, read, new byte[] {0});
            }
        } 
        catch (IOException ex) {                
            logger.log(Level.SEVERE, "Error while retrieving next chunk.", ex);
            return null;
        }
    }
    
    // http://en.wikipedia.org/wiki/Synchsafe
    private int unsynchsafe(int inData) {
        int outData = 0, mask = 0x7F000000;

        while (mask > 0) {
            outData >>= 1;
            outData |= inData & mask;
            mask >>= 8;
        }

        return outData;
    }
    
}
