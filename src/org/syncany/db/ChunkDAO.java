package org.syncany.db;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class ChunkDAO {
	public void writeChunk(ChunkEntry chunk, DataOutputStream dos) throws IOException {		
		// Size
		dos.writeInt(chunk.getSize());
        
        // Checksum
        dos.writeByte(chunk.getChecksum().length);
        dos.write(chunk.getChecksum());		
	}
	
	public ChunkEntry readChunk(DataInputStream dis) throws IOException {
		
        // Size
        int chunkSize = dis.readInt();
        
        // Checksum
		int checksumLen = dis.readByte();        
        byte[] chunkChecksum = new byte[checksumLen];
        dis.readFully(chunkChecksum, 0, checksumLen);

        // Create chunk entry object
        ChunkEntry chunk = new ChunkEntry(chunkChecksum, chunkSize);
        
        return chunk;
	}
	
}
