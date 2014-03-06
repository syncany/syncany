/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The fixed chunker is an implementation of the {@link Chunker}. It implements a simple
 * fixed-offset chunking, i.e. it breaks files at multiples of the given chunk size
 * parameter. 
 * 
 * <p>While it is very fast due to its offset-based approach (and not content-based), it 
 * performs very badly when bytes are added or removed from the beginning of a file. 
 * 
 * <p>Details can be found in chapter 3.4 of the thesis at <a href="http://blog.philippheckel.com/2013/05/20/minimizing-remote-storage-usage-and-synchronization-time-using-deduplication-and-multichunking-syncany-as-an-example/3/#Fixed-Size%20Chunking">blog.philippheckel.com</a>.
 * The <tt>FixedChunker</tt> implements the chunker described in chapter 3.4.2.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FixedChunker extends Chunker {
    private static final Logger logger = Logger.getLogger(FixedChunker.class.getSimpleName());   

    public static final String DEFAULT_DIGEST_ALG = "SHA1";
	public static final String TYPE = "fixed";

    private int chunkSize;   
    private String checksumAlgorithm;
    
    /**
     * Creates a new fixed offset chunker with the default file/chunk 
     * checksum algorithm SHA1.
     * 
     * @param chunkSize Size of a chunk in bytes
     */
    public FixedChunker(int chunkSize) {
        this(chunkSize, DEFAULT_DIGEST_ALG);
    }
    
    /**
     * Creates a new fixed offset chunker.
     * 
     * @param chunkSize Size of a chunk in bytes
     * @param checksumAlgorithm Algorithm to calculare the chunk and file checksums (e.g. SHA1, MD5)
     */
    public FixedChunker(int chunkSize, String checksumAlgorithm) {
        this.chunkSize = chunkSize;        
        this.checksumAlgorithm = checksumAlgorithm;        
    }
  
    @Override
    public ChunkEnumeration createChunks(File file) throws IOException {
    	return new FixedChunkEnumeration(new FileInputStream(file));
    }
    
	@Override
	public String getChecksumAlgorithm() {
		return checksumAlgorithm;
	}    

    @Override
    public String toString() {
        return "Fixed-"+chunkSize+"-"+checksumAlgorithm;
    }

    public class FixedChunkEnumeration implements ChunkEnumeration {
    	private MessageDigest digest;
        private MessageDigest fileDigest;    
        
        private InputStream in;           
        private byte[] buffer;
        private boolean closed;
        
        public FixedChunkEnumeration(InputStream in) {
            this.in = in;
            this.buffer = new byte[chunkSize];
            this.closed = false;
            
            try {
                this.digest = MessageDigest.getInstance(checksumAlgorithm);
                this.fileDigest = MessageDigest.getInstance(checksumAlgorithm);     
                
                this.fileDigest.reset();                                          
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }                    
        }
        
        @Override
        public boolean hasMoreElements() {
            if (closed) {
                return false;
            }
            
            try {
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
                int read = in.read(buffer);
                
                if (read == -1) {
                    return null;
                }
                
                // Close if this was the last bytes
                if (in.available() == 0) {
                    in.close();
                    closed = true;
                }
                
                // Chunk checksum
                digest.reset();
                digest.update(buffer, 0, read);
                
                // File checksum
                fileDigest.update(buffer, 0, read);                                
                byte[] fileChecksum = (closed) ? fileDigest.digest() : null;

                // Create chunk
                return new Chunk(digest.digest(), buffer, read, fileChecksum);
            } 
            catch (IOException ex) {                
                logger.log(Level.SEVERE, "Error while retrieving next chunk.", ex);
                return null;
            }
        }

        @Override
        public void close() {
        	try { in.close(); }
        	catch (Exception e) { /* Not necessary */ }
        }
    }
}