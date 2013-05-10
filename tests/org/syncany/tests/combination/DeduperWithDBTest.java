package org.syncany.tests.combination;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.syncany.chunk.Chunk;
import org.syncany.chunk.Chunker;
import org.syncany.chunk.CustomMultiChunker;
import org.syncany.chunk.Deduper;
import org.syncany.chunk.DeduperListener;
import org.syncany.chunk.FixedOffsetChunker;
import org.syncany.chunk.GzipCompressor;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.Transformer;
import org.syncany.db.ChunkEntry;
import org.syncany.db.FileContent;
import org.syncany.db.FileHistoryPart;
import org.syncany.db.FileVersion;
import org.syncany.db.MultiChunkEntry;
import org.syncany.util.FileLister;
import org.syncany.util.FileLister.FileListerListener;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class DeduperWithDBTest {


	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		final Deduper indexer = new Deduper();
		final DatabaseOLD db = new DatabaseOLD();
		final List<File> files = new ArrayList<File>();
		Chunker chunker = new FixedOffsetChunker(16 * 1024);
		MultiChunker multiChunker = new CustomMultiChunker(512 * 1024, 0);
		Transformer transformer = new GzipCompressor();	
		
		final File localRepoDir = new File("/tmp/syncany-test-db");
		final File localCacheDir = new File("/tmp/syncany-db-cache");		

		new FileLister(new File("/tmp/syncany-db-test"), new FileListerListener() {
			@Override public void proceedFile(File f) { files.add(f); }			

			@Override public void startProcessing() { }			
			@Override public void outDirectory(File directory) { }			
			@Override public boolean fileFilter(File file) { return true; }			
			@Override public void enterDirectory(File directory) { }			
			@Override public void endOfProcessing() { }			
			@Override public boolean directoryFilter(File directory) { return true; }
		}).start();		
		
		indexer.deduplicate(files, chunker, multiChunker, transformer, new DeduperListener() {
			private FileHistoryPart fileHistory;
			private FileVersion fileVersion;
			private ChunkEntry chunkEntry;		
			private MultiChunkEntry multiChunkEntry;	
			private FileContent content;
		
			@Override
			public boolean onChunk(Chunk chunk) {
				System.out.println("CHUNK       "+chunk);
				chunkEntry = db.getChunk(chunk.getChecksum());

				if (chunkEntry == null) {
					chunkEntry = new ChunkEntry(chunk.getChecksum(), chunk.getSize());
					db.addChunk(chunkEntry);
					
					return true;
				}
				
				return false;
			}
			
			@Override
			public void onFileStart(File file) {
				System.out.println("FILE OPEN   "+file);
				// Check if file exists, or create new
				fileHistory = db.getFileHistory(
						FileUtil.getRelativePath(localRepoDir, file), file.getName());
	
				if (fileHistory == null) {
					fileHistory = new FileHistoryPart();
				}
	
				// Check for versions
				fileVersion = fileHistory.getLastVersion();
	
				if (fileVersion == null) {
					fileVersion = db.createFileVersion(fileHistory);
				}
	
				fileVersion.setVersion(1L);
				fileVersion.setPath(FileUtil.getRelativePath(localRepoDir,
						file.getParentFile()));
				fileVersion.setName(file.getName());

			}


			@Override
			public void onOpenMultiChunk(MultiChunk multiChunk) {
				System.out.println("MULTI OPEN  "+multiChunk);
				multiChunkEntry = new MultiChunkEntry();
				multiChunkEntry.setId(chunkEntry.getChecksum());
			}

			@Override
			public void onCloseMultiChunk(MultiChunk multiChunk) {
				System.out.println("MULTI CLOSE  ");

				db.addMultiChunk(multiChunkEntry);
				multiChunkEntry = null;
				
				
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
				multiChunkEntry.addChunk(chunkEntry);				
			}

			@Override
			public void onFileAddChunk(Chunk chunk) {
				System.out.println("ADD CHUNK TO CONTENT "+chunk);			
				if (content == null) {
					content = new FileContent();
				}
				
				content.addChunk(chunkEntry);				
			}

			@Override
			public void onFileEnd(byte[] checksum) {
				if (checksum != null) {
					System.out.println("FILE END "+StringUtil.toHex(checksum));
				}
				else {
					System.out.println("FILE END ");
				}
				
				
				if (content != null) {
					content.setChecksum(checksum);

					fileVersion.setContent(content);
					db.addFileContent(content);
				}
				
				content = null;		
				

				// fileHistory.addVersion(fileVersion);
				db.addFileHistory(fileHistory);
				
			}
			
			
		});
		
		
		db.save(new File("/tmp/syncany-db-full"), true);

	}

}
