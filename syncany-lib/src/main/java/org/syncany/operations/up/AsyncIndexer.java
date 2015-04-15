package org.syncany.operations.up;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.chunk.Deduper;
import org.syncany.config.Config;
import org.syncany.database.DatabaseVersion;

/**
 * AsyncIndexer provides a Runnable to start as a separate thread. Running
 * this will result in the list of files provided being indexed and deduped. 
 * The result of this indexation will be captured in DatabaseVersions, which
 * will be stored in the provided Queue, which must be threadsafe.
 * 
 * @author Tim Hegeman
 */
public class AsyncIndexer implements Runnable {
	private static final Logger logger = Logger.getLogger(AsyncIndexer.class.getSimpleName());

	public static final DatabaseVersion FINAL_DATABASE_VERSION = new DatabaseVersion();

	private final Indexer indexer;
	private final List<File> files;
	private final Queue<DatabaseVersion> databaseVersionQueue;

	/** 
	 * @param config specifying all necessary options
	 * @param deduper the Deduper, already configured.
	 * @param files List of Files to be indexed.
	 * @param queue a threadsafe Queue to communicate DatabaseVersions.
	 */
	public AsyncIndexer(Config config, Deduper deduper, List<File> files, Queue<DatabaseVersion> queue) {
		this.files = files;
		this.databaseVersionQueue = queue;
		this.indexer = new Indexer(config, deduper);
	}

	@Override
	public void run() {
		try {
			logger.log(Level.INFO, "Starting Indexing.");
			indexer.index(files, databaseVersionQueue);
		}
		catch (IOException e) {
			// TODO: Store this exception as a "result"?
			e.printStackTrace();
		}
		// Signal end-of-stream.
		logger.log(Level.INFO, "Stopping indexing. Signal end of stream with empty databaseversion");
		databaseVersionQueue.offer(FINAL_DATABASE_VERSION);
	}

}
