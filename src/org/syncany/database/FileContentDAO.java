package org.syncany.database;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;


public class FileContentDAO {
	public void writeFileContent(FileContent fileContent, DataOutputStream dos) throws IOException {
        // Content checksum 
        dos.writeByte(fileContent.getChecksum().length);
        dos.write(fileContent.getChecksum());        
        
        // Content size
        dos.writeInt(fileContent.getContentSize());

        // Chunks (size + local references)
        dos.writeInt(fileContent.getChunks().size());

        for (ChunkEntry chunk : fileContent.getChunks()) {
            dos.write(chunk.getChecksum());                    
        }		
	}

	public FileContent readFileContent(Database db, DatabaseVersion dbv, DataInputStream dis) throws IOException {
		FileContent fileContent = new FileContent();
		
		// Content checksum
		int checksumLen = dis.readByte();
		byte[] checksum = new byte[checksumLen];
		dis.readFully(checksum, 0, checksumLen);
		fileContent.setChecksum(checksum);
		
		// Content size
		int contentSize = dis.readInt();
		fileContent.setContentSize(contentSize);

		// Chunks
		int chunksCount = dis.readInt();

		for (int i = 0; i < chunksCount; i++) {
			byte[] chunkChecksum = new byte[checksumLen];
			dis.readFully(chunkChecksum);

			ChunkEntry chunk = db.getChunk(chunkChecksum);

			if (chunk == null) {
				chunk = dbv.getChunk(chunkChecksum);
				
				if (chunk == null) {
					throw new IOException("Chunk with checksum " + Arrays.toString(chunkChecksum) + " does not exist.");
				}
			}

			fileContent.addChunk(chunk);
		}

		return fileContent;
	}
}
