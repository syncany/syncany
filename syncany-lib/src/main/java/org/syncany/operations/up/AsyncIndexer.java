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
 * @author Tim Hegeman
 */
public class AsyncIndexer implements Runnable {
	private static final Logger logger = Logger.getLogger(AsyncIndexer.class.getSimpleName());

	private final Indexer indexer;
	private final List<File> files;
	private final Queue<DatabaseVersion> databaseVersionQueue;
	private boolean done;

	public AsyncIndexer(Config config, Deduper deduper, List<File> files, Queue<DatabaseVersion> queue) {
		this.files = files;
		this.databaseVersionQueue = queue;
		this.indexer = new Indexer(config, deduper);
		this.done = false;
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
		databaseVersionQueue.offer(new DatabaseVersion());
		this.done = true;
	}

}
