package org.syncany.db;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;


public class MultiChunkDAO {
	public void writeMultiChunk(MultiChunkEntry multiChunk, DataOutputStream dos) throws IOException {
		// Multichunk id + size
		dos.writeByte(multiChunk.getId().length);
		dos.write(multiChunk.getId());

		// Chunks (size + local references)
		dos.writeShort(multiChunk.getChunks().size());

		for (ChunkEntry chunk : multiChunk.getChunks()) {
			dos.write(chunk.getChecksum());
		}
	}

	public MultiChunkEntry readMultiChunk(Database db, DatabaseVersion dbv, DataInputStream dis) throws IOException {		
		// Multichunk checksum
		int multiChunkIdLen = dis.readByte();
		byte[] multiChunkId = new byte[multiChunkIdLen];
		dis.readFully(multiChunkId, 0, multiChunkIdLen);
		
		// Create multichunk entry object
		MultiChunkEntry multiChunk = new MultiChunkEntry(multiChunkId);	

		// Chunks
		int chunksCount = dis.readShort();

		for (int i = 0; i < chunksCount; i++) {
			byte[] chunkChecksum = new byte[multiChunkIdLen];
			dis.readFully(chunkChecksum);

			ChunkEntry chunk = db.getChunk(chunkChecksum);

			if (chunk == null) {
				chunk = dbv.getChunk(chunkChecksum);
				
				if (chunk == null) {
					throw new IOException("Chunk with checksum " + Arrays.toString(chunkChecksum) + " does not exist.");
				}
			}

			multiChunk.addChunk(chunk);
		}

		return multiChunk;
	}
}
