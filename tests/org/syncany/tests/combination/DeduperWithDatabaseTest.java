package org.syncany.tests.combination;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
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
import org.syncany.config.Constants;
import org.syncany.database.ChunkEntry;
import org.syncany.database.Database;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.tests.util.TestUtil;
import org.syncany.util.FileLister;
import org.syncany.util.FileLister.FileListerListener;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class DeduperWithDatabaseTest {
	private File tempLocalSourceDir;
	private File tempLocalCacheDir;
	
	@Before
	public void setUp() throws Exception {
		File rootDir = TestUtil.createTempDirectoryInSystemTemp();
		
		tempLocalSourceDir = new File(rootDir+"/local");
		tempLocalSourceDir.mkdir();
		
		tempLocalCacheDir = new File(rootDir+"/cache");		
		tempLocalCacheDir.mkdir();
	}
	
	@After
	public void tearDown() {
		TestUtil.deleteDirectory(tempLocalSourceDir);
	}
	
	@Test
	@Ignore
	public void testDeduperWithDatabase() throws IOException {
		final Database db = new Database();
		final List<File> files = FileUtil.getRecursiveFileList(tempLocalSourceDir);
		Chunker chunker = new FixedOffsetChunker(16 * 1024);
		MultiChunker multiChunker = new CustomMultiChunker(512 * 1024);
		Transformer transformer = new GzipCompressor();	
		final Deduper deduper = new Deduper(chunker, multiChunker, transformer);		
		
		deduper.deduplicate(files, new DeduperListener() {
			private PartialFileHistory fileHistory;
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
				String relativeFilePath = FileUtil.getRelativePath(tempLocalSourceDir, file) + Constants.DATABASE_FILE_SEPARATOR + file.getName(); 
				fileHistory = db.getFileHistory(relativeFilePath);
	
				if (fileHistory == null) {
					fileHistory = new PartialFileHistory();
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
				multiChunkEntry = new MultiChunkEntry(multiChunk.getId());
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
					return new File(tempLocalCacheDir 
									+ "/multichunk-"
									+ StringUtil.toHex(multiChunkId));
			}

			@Override
			public void onWriteMultiChunk(MultiChunk multiChunk, Chunk chunk) {
				System.out.println("WRITE CHUNK TO MULTI "+chunk);			
				multiChunkEntry.addChunk(chunkEntry);				
			}

		
			@Override
			public void onFinish() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStart() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onFileAddChunk(File file, Chunk chunk) {
				System.out.println("ADD CHUNK TO CONTENT "+chunk);			
				if (content == null) {
					content = new FileContent();
				}
				
				content.addChunk(chunkEntry);				
			}

			@Override
			public void onFileEnd(File file, byte[] checksum) {
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
		
		
		//db.save(new File("/tmp/syncany-db-full"), true);

	}

}
