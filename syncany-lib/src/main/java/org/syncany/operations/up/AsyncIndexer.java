package org.syncany.operations.up;

import org.syncany.chunk.Deduper;
import org.syncany.config.Config;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Tim Hegeman
 */
public class AsyncIndexer implements Runnable {

	private final Indexer indexer;
	private final List<File> files;
	private final long transactionSizeLimit;
	private final Indexer.IndexerNewDatabaseVersionListener databaseVersionListener;
	private boolean done;

	public AsyncIndexer(Config config, Deduper deduper, List<File> files, long transactionSizeLimit,
			Indexer.IndexerNewDatabaseVersionListener databaseVersionListener) {
		this.files = files;
		this.transactionSizeLimit = transactionSizeLimit;
		this.databaseVersionListener = databaseVersionListener;
		this.indexer = new Indexer(config, deduper);
		this.done = false;
	}

	@Override
	public void run() {
		try {
			indexer.index(files, transactionSizeLimit, databaseVersionListener);
		}
		catch (IOException e) {
			// TODO: Store this exception as a "result"?
			e.printStackTrace();
		}
		this.done = true;
	}

	public boolean isDone() {
		return this.done;
	}
}
