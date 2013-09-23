package org.syncany.operations;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.util.StringUtil;

public class OperationFactory {
	private static final Logger logger = Logger.getLogger(OperationFactory.class.getSimpleName());
	
	public static Operation getInstance(String operation) throws Exception {
		String thisPackage = OperationFactory.class.getPackage().getName();
		String camelCaseOperationName = StringUtil.toCamelCase(operation);
		String fqOperationClassName = thisPackage+"."+camelCaseOperationName;
		
		// Try to load!
		try {
			Class<?> operationClass = Class.forName(fqOperationClassName);
			return (Operation) operationClass.newInstance();
		} 
		catch (Exception ex) {
			logger.log(Level.SEVERE, "Could not load operation : " + fqOperationClassName, ex);
			throw ex;
		}		
	}
}
