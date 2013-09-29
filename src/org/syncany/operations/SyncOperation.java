package org.syncany.operations;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.operations.SyncDownOperation.SyncDownOperationOptions;
import org.syncany.operations.SyncDownOperation.SyncDownOperationResult;
import org.syncany.operations.SyncUpOperation.SyncUpOperationOptions;
import org.syncany.operations.SyncUpOperation.SyncUpOperationResult;

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
		SyncDownOperation syncDown = new SyncDownOperation(config, loadedDatabase, options.getSyncDownOptions());
		SyncUpOperation syncUp = new SyncUpOperation(config, loadedDatabase, options.getSyncUpOptions());
		
		SyncDownOperationResult syncDownResults = (SyncDownOperationResult) syncDown.execute();
		SyncUpOperationResult syncUpResults = (SyncUpOperationResult) syncUp.execute();
		
		return new SyncOperationResult(syncDownResults, syncUpResults);
	}
	
	public static class SyncOperationOptions implements OperationOptions {
		private SyncUpOperationOptions syncUpOptions;
		private SyncDownOperationOptions syncDownOptions;		

		public SyncDownOperationOptions getSyncDownOptions() {
			return syncDownOptions;
		}
		
		public void setSyncDownOptions(SyncDownOperationOptions syncDownOptions) {
			this.syncDownOptions = syncDownOptions;
		}				
		
		public SyncUpOperationOptions getSyncUpOptions() {
			return syncUpOptions;
		}
		
		public void setSyncUpOptions(SyncUpOperationOptions syncUpOptions) {
			this.syncUpOptions = syncUpOptions;
		}		
	}
	
	public class SyncOperationResult implements OperationResult {
		private SyncDownOperationResult syncDownResult;
		private SyncUpOperationResult syncUpResult;
		
		public SyncOperationResult(SyncDownOperationResult syncDownResult, SyncUpOperationResult syncUpResult) {
			this.syncDownResult = syncDownResult;
			this.syncUpResult = syncUpResult;
		}

		public SyncDownOperationResult getSyncDownResult() {
			return syncDownResult;
		}		
		
		public SyncUpOperationResult getSyncUpResult() {
			return syncUpResult;
		}				
	}
}
