/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.operations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.syncany.chunk.Chunker;
import org.syncany.chunk.Deduper;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.config.Config;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.SqlDatabase;
import org.syncany.util.StringUtil;

/**
 * The assembler re-assembles files broken down through the deduplication
 * mechanisms of the {@link Deduper} and its corresponding classes (chunker,
 * multichunker, etc.).
 * 
 * <p>It uses the local {@link SqlDatabase} and an optional {@link MemoryDatabase}
 * to perform file checksum and chunk checksum lookups.   
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Assembler {
	private static final Logger logger = Logger.getLogger(Assembler.class.getSimpleName());
	
	private Config config;
	private SqlDatabase localDatabase;
	private MemoryDatabase memoryDatabase;
	
	public Assembler(Config config, SqlDatabase localDatabase) {
		this(config, localDatabase, null);
	}
	
	public Assembler(Config config, SqlDatabase localDatabase, MemoryDatabase memoryDatabase) {
		this.config = config;
		this.localDatabase = localDatabase;
		this.memoryDatabase = memoryDatabase;
	}

	/**
	 * Assembles the given file version to the local cache and returns a reference
	 * to the cached file after successfully assembling the file. 
	 */
	public File assembleToCache(FileVersion fileVersion) throws Exception {
		File reconstructedFileInCache = config.getCache().createTempFile("reconstructedFileVersion");
		logger.log(Level.INFO, "     - Creating file " + fileVersion.getPath() + " to " + reconstructedFileInCache + " ...");

		FileContent fileContent = localDatabase.getFileContent(fileVersion.getChecksum(), true);

		if (fileContent == null && memoryDatabase != null) {
			fileContent = memoryDatabase.getContent(fileVersion.getChecksum());
		}
		
		// Check consistency!
		if (fileContent == null && fileVersion.getChecksum() != null) {
			throw new Exception("Cannot determine file content for checksum "+fileVersion.getChecksum());
		}

		// Create empty file
		if (fileContent == null) {
			FileUtils.touch(reconstructedFileInCache);	
			return reconstructedFileInCache;
		}
				
		// Create non-empty file
		Chunker chunker = config.getChunker();
		MultiChunker multiChunker = config.getMultiChunker();
		
		FileOutputStream reconstructedFileOutputStream = new FileOutputStream(reconstructedFileInCache);		
		MessageDigest reconstructedFileChecksum = MessageDigest.getInstance(chunker.getChecksumAlgorithm());
		
		if (fileContent != null) { // File can be empty!
			Collection<ChunkChecksum> fileChunks = fileContent.getChunks();

			for (ChunkChecksum chunkChecksum : fileChunks) {
				MultiChunkId multiChunkIdForChunk = localDatabase.getMultiChunkId(chunkChecksum);

				if (multiChunkIdForChunk == null && memoryDatabase != null) {
					multiChunkIdForChunk = memoryDatabase.getMultiChunkIdForChunk(chunkChecksum);
				}

				File decryptedMultiChunkFile = config.getCache().getDecryptedMultiChunkFile(multiChunkIdForChunk);

				MultiChunk multiChunk = multiChunker.createMultiChunk(decryptedMultiChunkFile);
				InputStream chunkInputStream = multiChunk.getChunkInputStream(chunkChecksum.getBytes());

				byte[] buffer = new byte[4096];
				int read = 0;

				while (-1 != (read = chunkInputStream.read(buffer))) {
					reconstructedFileChecksum.update(buffer, 0, read);
					reconstructedFileOutputStream.write(buffer, 0, read);
				}

				chunkInputStream.close();
				multiChunk.close();
			}
		}

		reconstructedFileOutputStream.close();

		// Validate checksum
		byte[] reconstructedFileExpectedChecksum = fileContent.getChecksum().getBytes();
		byte[] reconstructedFileActualChecksum = reconstructedFileChecksum.digest();
		
		if (!Arrays.equals(reconstructedFileActualChecksum, reconstructedFileExpectedChecksum)) {
			throw new Exception("Checksums do not match: expected " + StringUtil.toHex(reconstructedFileExpectedChecksum) + " != actual "
					+ StringUtil.toHex(reconstructedFileActualChecksum));
		}
		
		return reconstructedFileInCache;
	}	
}
