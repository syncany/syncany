package org.syncany.experimental.db.dao;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.syncany.experimental.db.ChunkEntry;

public class ChunkDAO {
	public void writeChunk(ChunkEntry chunk, DataOutputStream dos) throws IOException {		
		// Size
		dos.writeInt(chunk.getSize());
        
        // Checksum
        dos.writeByte(chunk.getChecksum().length);
        dos.write(chunk.getChecksum());		
	}
	
	public ChunkEntry readChunk(DataInputStream dis) throws IOException {
        ChunkEntry chunk = new ChunkEntry();
		
        // Size
        int chunkSize = dis.readInt();
        chunk.setSize(chunkSize);
        
        // Checksum
		int checksumLen = dis.readByte();        
        byte[] checksum = new byte[checksumLen];
        dis.readFully(checksum, 0, checksumLen);
        chunk.setChecksum(checksum);
        
        return chunk;
	}
	
}
