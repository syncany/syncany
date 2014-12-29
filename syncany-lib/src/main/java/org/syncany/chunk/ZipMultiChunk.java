/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.util.StringUtil;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class ZipMultiChunk extends MultiChunk {
    private ZipOutputStream zipOut;
    private ZipInputStream zipIn;
    private ZipFile zipFile;

    public ZipMultiChunk(InputStream is) {
        super(0);
        this.zipIn = new ZipInputStream(is);
    }
    
    public ZipMultiChunk(File file) throws ZipException, IOException {
		super(0);
		this.zipFile = new ZipFile(file);
	}    
    
    public ZipMultiChunk(MultiChunkId id, int minSize, OutputStream os) throws IOException {
        super(id, minSize);        
        
        this.zipOut = new ZipOutputStream(os);
        this.zipOut.setLevel(ZipOutputStream.STORED); // No compression        
    }                

	@Override
    public boolean isFull() {
        return size >= minSize*1024; // minSize is in KB!
    }

    @Override
    public void write(Chunk chunk) throws IOException {
        size += chunk.getSize();
       
        ZipEntry entry = new ZipEntry(StringUtil.toHex(chunk.getChecksum()));
        entry.setSize(chunk.getSize());

        zipOut.putNextEntry(entry);
        zipOut.write(chunk.getContent(), 0, chunk.getSize());
        zipOut.closeEntry();
    }    
    
    @Override
    public InputStream getChunkInputStream(byte[] checksum) throws IOException {
    	ZipEntry chunkEntry = zipFile.getEntry(StringUtil.toHex(checksum));
    	InputStream chunkInputStream = zipFile.getInputStream(chunkEntry);
    	
    	return chunkInputStream;
    }
    
    @Override
    public Chunk read() throws IOException {
        ZipEntry entry = zipIn.getNextEntry();

        if (entry == null) {
            return null;
        }
        
        int read;
        ByteArrayOutputStream contentByteArray = new ByteArrayOutputStream();
        
        while (-1 != (read = zipIn.read())) {
        	contentByteArray.write(read);
        }       
        
        return new Chunk(StringUtil.fromHex(entry.getName()), contentByteArray.toByteArray(), contentByteArray.size(), null);
    }
   

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public void close() throws IOException {
        if (zipOut != null) {
            zipOut.close();
        }

        if (zipIn != null) {
            zipIn.close();
        }

        if (zipFile != null) {
            zipFile.close();
        }
    }
}

