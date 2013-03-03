package org.syncany.experimental.db.dao;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.syncany.experimental.db.ChunkEntry;
import org.syncany.experimental.db.Database;
import org.syncany.experimental.db.MultiChunkEntry;

public class MultiChunkDAO {
	public void writeMultiChunk(MultiChunkEntry multiChunk, DataOutputStream dos) throws IOException {
		// Multichunk checksum + size
		dos.writeByte(multiChunk.getChecksum().length);
		dos.write(multiChunk.getChecksum());

		// Chunks (size + local references)
		dos.writeShort(multiChunk.getChunks().size());

		for (ChunkEntry chunk : multiChunk.getChunks()) {
			dos.write(chunk.getChecksum());
		}
	}

	public MultiChunkEntry readMultiChunk(Database db, DataInputStream dis) throws IOException {
		MultiChunkEntry multiChunk = new MultiChunkEntry();
		
		// Multichunk checksum
		int checksumLen = dis.readByte();
		byte[] checksum = new byte[checksumLen];
		dis.readFully(checksum, 0, checksumLen);
		multiChunk.setChecksum(checksum);

		// Chunks
		int chunksCount = dis.readShort();

		for (int i = 0; i < chunksCount; i++) {
			byte[] chunkChecksum = new byte[checksumLen];
			dis.readFully(chunkChecksum);

			ChunkEntry chunk = db.getChunk(chunkChecksum);

			if (chunk == null) {
				throw new IOException("Chunk with checksum "
						+ Arrays.toString(chunkChecksum) + " does not exist.");
			}

			multiChunk.addChunk(chunk);
		}

		return multiChunk;
	}
}
