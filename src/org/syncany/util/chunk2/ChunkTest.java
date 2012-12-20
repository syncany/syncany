package org.syncany.util.chunk2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Enumeration;

import org.apache.commons.lang.StringUtils;
import org.syncany.util.StringUtil;
import org.syncany.util.chunk2.chunking.Chunk;
import org.syncany.util.chunk2.chunking.Chunker;
import org.syncany.util.chunk2.chunking.FixedOffsetChunker;
import org.syncany.util.chunk2.meta.CustomMultiChunker;
import org.syncany.util.chunk2.meta.MultiChunk;
import org.syncany.util.chunk2.meta.MultiChunker;
import org.syncany.util.chunk2.transform.Transformer;
import org.syncany.util.chunk2.transform.compress.GzipCompressor;

public class ChunkTest {
	public static void main(String[] args) throws Exception {
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
