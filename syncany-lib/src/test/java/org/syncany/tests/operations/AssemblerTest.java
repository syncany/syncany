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
package org.syncany.tests.operations;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.syncany.chunk.Chunk;
import org.syncany.chunk.MultiChunk;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class AssemblerTest {		
	/**
	 * Alters multichunk data to test whether integrity checks work.
	 */
	@Test
	public void testAssembler() throws Exception {	
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		clientA.createNewFile("file1.jpg", 20); // small, only one chunk, one multichunk
		clientA.upWithForceChecksum();
		
		// Read chunk from original multichunk
		File repoMultiChunksFolder = new File(testConnection.getPath(), "multichunks");
		File multiChunkFile = repoMultiChunksFolder.listFiles()[0];
		
		MultiChunk multiChunk = clientA.getConfig().getMultiChunker().createMultiChunk(
			clientA.getConfig().getTransformer().createInputStream(new FileInputStream(multiChunkFile)));		
		Chunk chunk = multiChunk.read();
		multiChunk.close();
		
		// Flip byte in chunk and write new "altered" multichunk
		File alteredMultiChunkFile = new File(multiChunkFile + "-altered");
		MultiChunk alteredMultiChunk = clientA.getConfig().getMultiChunker().createMultiChunk(
			multiChunk.getId(), clientA.getConfig().getTransformer().createOutputStream(new FileOutputStream(alteredMultiChunkFile)));
		
		chunk.getContent()[0] ^= 0x01; // Flip one byte!
		alteredMultiChunk.write(chunk);
		alteredMultiChunk.close();
		
		// Now delete old multichunk, and swap by "altered" file
		multiChunkFile.delete();
		FileUtils.moveFile(alteredMultiChunkFile, multiChunkFile);
		
		boolean exceptionThrown = false;
		
		try {
			clientB.down(); // If this does not throw an exception, it's bad!
		}
		catch (Exception e) {
			exceptionThrown = true;
		}
		
		assertTrue(exceptionThrown);
		
		clientA.deleteTestData();
		clientB.deleteTestData();
	}	
}
