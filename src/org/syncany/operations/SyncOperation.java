package org.syncany.operations;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.operations.DownOperation.DownOperationOptions;
import org.syncany.operations.DownOperation.DownOperationResult;
import org.syncany.operations.UpOperation.UpOperationOptions;
import org.syncany.operations.UpOperation.UpOperationResult;

public class SyncOperation extends Operation {
	private Database loadedDatabase;
	private SyncOperationOptions options;
	
	public SyncOperation(Config config) {
		this(config, null, new SyncOperationOptions());
	}	
	
	public SyncOperation(Config config, Database database, SyncOperationOptions options) {
		super(config);		
		
		this.loadedDatabase = database;
		this.options = options;
	}		
	
	@Override
	public OperationResult execute() throws Exception {
		DownOperation syncDown = new DownOperation(config, loadedDatabase, options.getSyncDownOptions());
		UpOperation syncUp = new UpOperation(config, loadedDatabase, options.getSyncUpOptions());
		
		DownOperationResult syncDownResults = (DownOperationResult) syncDown.execute();
		UpOperationResult syncUpResults = (UpOperationResult) syncUp.execute();
		
		return new SyncOperationResult(syncDownResults, syncUpResults);
	}
	
	public static class SyncOperationOptions implements OperationOptions {
		private UpOperationOptions syncUpOptions = new UpOperationOptions();
		private DownOperationOptions syncDownOptions = new DownOperationOptions();

		public DownOperationOptions getSyncDownOptions() {
			return syncDownOptions;
		}
		
		public void setSyncDownOptions(DownOperationOptions syncDownOptions) {
			this.syncDownOptions = syncDownOptions;
		}				
		
		public UpOperationOptions getSyncUpOptions() {
			return syncUpOptions;
		}
		
		public void setSyncUpOptions(UpOperationOptions syncUpOptions) {
			this.syncUpOptions = syncUpOptions;
		}		
	}
	
	public class SyncOperationResult implements OperationResult {
		private DownOperationResult syncDownResult;
		private UpOperationResult syncUpResult;
		
		public SyncOperationResult(DownOperationResult syncDownResult, UpOperationResult syncUpResult) {
			this.syncDownResult = syncDownResult;
			this.syncUpResult = syncUpResult;
		}

		public DownOperationResult getSyncDownResult() {
			return syncDownResult;
		}		
		
		public UpOperationResult getSyncUpResult() {
			return syncUpResult;
		}				
	}
}
