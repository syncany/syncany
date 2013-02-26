package org.syncany.tests.index;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.syncany.chunk.Chunk;
import org.syncany.chunk.Chunker;
import org.syncany.chunk.CustomMultiChunker;
import org.syncany.chunk.Deduper;
import org.syncany.chunk.FixedOffsetChunker;
import org.syncany.chunk.GzipCompressor;
import org.syncany.chunk.DeduperListener;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.Transformer;
import org.syncany.util.FileLister;
import org.syncany.util.FileLister.FileListerListener;
import org.syncany.util.FileLister.FileListerAdapter;
import org.syncany.util.StringUtil;

public class IndexerTestJustPrint {


	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		final Deduper deduper = new Deduper();
		final List<File> files = new ArrayList<File>();
		Chunker chunker = new FixedOffsetChunker(16 * 1024);
		MultiChunker multiChunker = new CustomMultiChunker(512 * 1024, 0);
		Transformer transformer = new GzipCompressor();	
		
		final File localCacheDir = new File("/tmp/syncany-db-cache");		
		localCacheDir.mkdirs();
		
		new FileLister(new File("/tmp/syncany-db-test"), new FileListerAdapter() {
			@Override public void proceedFile(File f) { files.add(f); }			
		}).start();
		  
		deduper.deduplicate(files, chunker, multiChunker, transformer, new DeduperListener() {
			@Override
			public boolean onChunk(Chunk chunk) {
				System.out.println("CHUNK       "+chunk);
				return true;
			}
			
			@Override
			public void onFileStart(File file) {
				System.out.println("FILE OPEN   "+file);
			}

			@Override
			public void onOpenMultiChunk(MultiChunk multiChunk) {
				System.out.println("MULTI OPEN  "+multiChunk);
			}

			@Override
			public void onCloseMultiChunk(MultiChunk multiChunk) {
				System.out.println("MULTI CLOSE  ");
			}

			@Override
			public File getMultiChunkFile(byte[] multiChunkId) {
					return new File(localCacheDir 
									+ "/multichunk-"
									+ StringUtil.toHex(multiChunkId));
			}

			@Override
			public void onWriteMultiChunk(MultiChunk multiChunk, Chunk chunk) {
				System.out.println("WRITE CHUNK TO MULTI "+chunk);			
			}

			@Override
			public void onFileAddChunk(Chunk chunk) {
				System.out.println("ADD CHUNK TO CONTENT "+chunk);			
			}

			@Override
			public void onFileEnd(byte[] checksum) {
				if (checksum != null) {
					System.out.println("FILE END "+StringUtil.toHex(checksum));
				}
				else {
					System.out.println("FILE END ");
				}
			}
						
		});

	}

}
