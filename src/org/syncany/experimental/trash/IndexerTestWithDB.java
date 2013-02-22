package org.syncany.experimental.trash;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.syncany.chunk.chunking.Chunk;
import org.syncany.chunk.chunking.Chunker;
import org.syncany.chunk.chunking.FixedOffsetChunker;
import org.syncany.chunk.multi.CustomMultiChunker;
import org.syncany.chunk.multi.MultiChunk;
import org.syncany.chunk.multi.MultiChunker;
import org.syncany.chunk.transform.GzipCompressor;
import org.syncany.chunk.transform.Transformer;
import org.syncany.experimental.db.ChunkEntry;
import org.syncany.experimental.db.FileContent;
import org.syncany.experimental.db.Database;
import org.syncany.experimental.db.FileHistory;
import org.syncany.experimental.db.FileVersion;
import org.syncany.experimental.db.MultiChunkEntry;
import org.syncany.experimental.trash.Deduper.IndexerListener;
import org.syncany.util.FileLister;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;
import org.syncany.util.FileLister.FileListerListener;

public class IndexerTestWithDB {


	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		final Deduper indexer = new Deduper();
		final Database db = new Database();
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
		
		indexer.deduplicate(files, chunker, multiChunker, transformer, new IndexerListener() {
			private FileHistory fileHistory;
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
					fileHistory = new FileHistory();
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
				multiChunkEntry.setChecksum(chunkEntry.getChecksum());
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
					db.addContent(content);
				}
				
				content = null;		
				

				// fileHistory.addVersion(fileVersion);
				db.addFileHistory(fileHistory);
				
			}
			
			
		});
		
		
		db.save(new File("/tmp/syncany-db-full"), true);

	}

}
