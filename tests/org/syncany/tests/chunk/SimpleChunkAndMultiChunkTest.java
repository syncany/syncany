package org.syncany.tests.chunk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Enumeration;

import org.junit.Test;
import org.syncany.chunk.Chunk;
import org.syncany.chunk.Chunker;
import org.syncany.chunk.CustomMultiChunker;
import org.syncany.chunk.FixedOffsetChunker;
import org.syncany.chunk.GzipCompressor;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.Transformer;
import org.syncany.util.StringUtil;

public class SimpleChunkAndMultiChunkTest {
	@Test
	public void simpleChunkAndMultiChunkTest() throws Exception {
		Chunker c = new FixedOffsetChunker(5*1024);
		MultiChunker multiChunker = new CustomMultiChunker(512*1024, 0);
		Transformer trans = new GzipCompressor();
		
		File file = new File("test.html");
		File outputFile = null;
		if(file.exists()) {
			Enumeration<Chunk> chunks = c.createChunks(file);
			MultiChunk mc = null;
			while(chunks.hasMoreElements()) {
				
				Chunk currentChunk = chunks.nextElement();
				if(mc != null && mc.isFull()) {
					mc.close();
					mc = null;
				}
				if(mc == null) {
					outputFile = new File("outputFile" + StringUtil.toHex(currentChunk.getChecksum()) + ".gz");
					mc = multiChunker.createMultiChunk(currentChunk.getChecksum(),trans.transform(new FileOutputStream(outputFile)));	
				}

				mc.write(currentChunk);
			}
			if(mc != null) {
				mc.close();
			}
		} else {
			System.out.println("File loud failed at " + file.getAbsolutePath());
		}
		
		File reassembledFile = new File("outputTest.html");
		FileInputStream reassembledInputStream = new FileInputStream(outputFile);
		Transformer decTrans = new GzipCompressor();
		
		MultiChunk outputMultiChunk = multiChunker.createMultiChunk(decTrans.transform(reassembledInputStream));
		Chunk outputChunk;
		FileOutputStream assembledStream = new FileOutputStream(reassembledFile);
		while((outputChunk = outputMultiChunk.read()) != null) {
			System.out.println(outputChunk.getSize());
			assembledStream.write(outputChunk.getContent(), 0, outputChunk.getSize());
		}
		assembledStream.close();
		outputMultiChunk.close();
	}
}
