package org.syncany.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.operations.LoadDatabaseOperation.LoadDatabaseOperationResult;

public class LogOperation extends Operation {
	private static final Logger logger = Logger.getLogger(LogOperation.class.getSimpleName());	
	private LogOperationOptions options;
	
	public LogOperation() {
		super(null);
		this.options = new LogOperationOptions();
	}	

	public LogOperation(Config config) {
		this(config, null);
	}	
	
	public LogOperation(Config config, LogOperationOptions options) {
		super(config);		
		this.options = options;
	}	
		
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Log' at client "+config.getMachineName()+" ...");
		logger.log(Level.INFO, "--------------------------------------------");
		
		Database database = ((LoadDatabaseOperationResult) new LoadDatabaseOperation(config).execute()).getDatabase();		
		DatabaseVersion currentDatabaseVersion = database.getLastDatabaseVersion();
		
		if (currentDatabaseVersion == null) {
			throw new Exception("No database versions yet locally. Nothing to show here.");
		}

		List<PartialFileHistory> fileHistories = null;
		
		if (options.getPaths().isEmpty()) {
			fileHistories = new ArrayList<PartialFileHistory>(database.getFileHistories());			
		}
		else {
			fileHistories = getFileHistoriesByPath(options.getPaths(), database);
		}
		
		return new LogOperationResult(fileHistories);
	}			
	
	private List<PartialFileHistory> getFileHistoriesByPath(List<String> filePaths, Database database) {				
		List<PartialFileHistory> fileHistories = new ArrayList<PartialFileHistory>();

		for (String filePath : filePaths) {
			PartialFileHistory fileHistory = database.getFileHistory(filePath);
			
			if (fileHistory != null) {
				fileHistories.add(fileHistory);
			}
			else {
				logger.log(Level.INFO, "Cannot find file history for file "+filePath);
			}
		}
		
		return fileHistories;
	}
	
	public static class LogOperationOptions implements OperationOptions {
		private List<String> paths;		
		
		public List<String> getPaths() {
			return paths;
		}
		
		public void setPaths(List<String> paths) {
			this.paths = paths;
		}
	}
	
	public class LogOperationResult implements OperationResult {
		private List<PartialFileHistory> fileHistories;
		
		public LogOperationResult(List<PartialFileHistory> fileHistories) {
			this.fileHistories = fileHistories;
		}

		public List<PartialFileHistory> getFileHistories() {
			return fileHistories;
		}				
	}
}
