package org.syncany.operations;

import java.util.logging.Level;
import java.util.logging.Logger;

public class InitOperation extends Operation {
	private static final Logger logger = Logger.getLogger(InitOperation.class.getSimpleName());	
	
	public InitOperation() {
		super(null);
	}	
		
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Init'");
		logger.log(Level.INFO, "--------------------------------------------");
		
		System.out.println("NOT YET IMPLEMENTED");
		
		return new InitOperationResult();
	}		

	public class InitOperationResult implements OperationResult {
		
	}
}
