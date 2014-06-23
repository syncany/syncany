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
package org.syncany.operations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.config.Config;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.SqlDatabase;

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

		// Create file
		MultiChunker multiChunker = config.getMultiChunker();
		FileOutputStream reconstructedFileOutputStream = new FileOutputStream(reconstructedFileInCache);

		if (fileContent != null) { // File can be empty!
			Collection<ChunkChecksum> fileChunks = fileContent.getChunks();

			for (ChunkChecksum chunkChecksum : fileChunks) {
				MultiChunkId multiChunkIdForChunk = localDatabase.getMultiChunkId(chunkChecksum);

				if (multiChunkIdForChunk == null && memoryDatabase != null) {
					multiChunkIdForChunk = memoryDatabase.getMultiChunkIdForChunk(chunkChecksum);
				}

				File decryptedMultiChunkFile = config.getCache().getDecryptedMultiChunkFile(multiChunkIdForChunk);

				MultiChunk multiChunk = multiChunker.createMultiChunk(decryptedMultiChunkFile);
				InputStream chunkInputStream = multiChunk.getChunkInputStream(chunkChecksum.getRaw());

				// TODO [medium] Calculate checksum while writing file, to verify correct content
				IOUtils.copy(chunkInputStream, reconstructedFileOutputStream);
				
				chunkInputStream.close();
				multiChunk.close();
			}
		}

		reconstructedFileOutputStream.close();
		
		return reconstructedFileInCache;
	}	
}
