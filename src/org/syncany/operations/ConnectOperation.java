package org.syncany.operations;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.database.Database;

public class ConnectOperation extends Operation {
	private static final Logger logger = Logger.getLogger(ConnectOperation.class.getSimpleName());		
	
	private ConnectOperationOptions options;
	
	public ConnectOperation(ConnectOperationOptions options) {
		super(null);
		this.options = options;
	}		
	
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Connect'");
		logger.log(Level.INFO, "--------------------------------------------");
		
		
				
		return new ConnectOperationResult();
	}		
	
	public class ConnectOperationOptions implements OperationOptions {
		private ConnectionTO connectionTO;

		public ConnectionTO getConnectionTO() {
			return connectionTO;
		}

		public void setConnectionTO(ConnectionTO connectionTO) {
			this.connectionTO = connectionTO;
		}				
	}
	
	public class ConnectOperationResult implements OperationResult {
		// Nothing here yet.
	}
}
