package org.syncany.operations;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.database.Database;

public abstract class AbstractInitOperation extends Operation {
	private static final Logger logger = Logger.getLogger(AbstractInitOperation.class.getSimpleName());			

	public AbstractInitOperation(Config config) {
		super(config);
	}	
}
