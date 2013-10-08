package org.syncany.operations;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.operations.LoadDatabaseOperation.LoadDatabaseOperationResult;

public class WatchOperation extends Operation {
	private static final Logger logger = Logger.getLogger(WatchOperation.class.getSimpleName());
	private WatchOperationOptions options;
	
	public WatchOperation(Config config, WatchOperationOptions options) {
		super(config);
		this.options = options;
	}	
	
	public OperationResult execute() throws Exception {
		Database database = ((LoadDatabaseOperationResult) new LoadDatabaseOperation(config).execute()).getDatabase();
		
		while (true) {
			logger.log(Level.INFO, "Running sync ...");
			
			new DownOperation(config, database).execute();
			new UpOperation(config, database).execute();
			
			logger.log(Level.INFO, "Sync done, waiting {0} seconds ...", options.getInterval()/1000);
			Thread.sleep(options.getInterval());
		}
	}
	
	public static class WatchOperationOptions implements OperationOptions {
		private int interval = 30000;

		public int getInterval() {
			return interval;
		}

		public void setInterval(int interval) {
			this.interval = interval;
		}				
	}
	
	public static class WatchOperationResult implements OperationResult {
		// Fressen
	}
}
