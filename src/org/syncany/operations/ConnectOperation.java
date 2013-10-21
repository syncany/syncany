package org.syncany.operations;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.database.Database;

public class ConnectOperation extends Operation {
	private static final Logger logger = Logger.getLogger(ConnectOperation.class.getSimpleName());		
	
	public ConnectOperation(Config config) {
		this(config, null, null);
	}	
	
	public ConnectOperation(Config config, Database database, TransferManager transferManager) {
		super(config);				
	}	
	
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Connect' at client "+config.getMachineName()+" ...");
		logger.log(Level.INFO, "--------------------------------------------");
				
		return new ConnectOperationResult();
	}		
	
	public class ConnectOperationResult implements OperationResult {
		// Nothing here yet.
	}
}
