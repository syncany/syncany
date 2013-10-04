package org.syncany.cli;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.util.StringUtil;

public class CommandFactory {
	private static final Logger logger = Logger.getLogger(CommandFactory.class.getSimpleName());
	
	public static Command getInstance(String operation) throws Exception {
		String thisPackage = CommandFactory.class.getPackage().getName();
		String camelCaseOperationName = StringUtil.toCamelCase(operation);
		String fqCommandClassName = thisPackage+"."+camelCaseOperationName+"Command";
		
		// Try to load!
		try {
			Class<?> commandClass = Class.forName(fqCommandClassName);
			return (Command) commandClass.newInstance();
		} 
		catch (Exception ex) {
			logger.log(Level.INFO, "Could not find operation FQCN " + fqCommandClassName, ex);
			return null;
		}		
	}
}
