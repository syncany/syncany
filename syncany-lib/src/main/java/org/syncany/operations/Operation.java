/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.operations;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.DatabaseDAO;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.WriteDatabaseDAO;
import org.syncany.database.XmlDatabaseDAO;

/**
 * Operations represent and implement Syncany's business logic. They typically
 * correspond to a command or an action initiated by either a user, or by a 
 * periodic action. 
 * 
 * <p>Each operation might be configured using an operation-specific implementation of
 * the {@link OperationOptions} interface and is run using the {@link #execute()} method.
 * While the input options are optional, it must return a corresponding {@link OperationResult}
 * object.  
 *  
 * @see OperationOptions
 * @see OperationResult
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class Operation {
	private static final Logger logger = Logger.getLogger(Operation.class.getSimpleName());
	protected Config config;
	
	public Operation(Config config) {
		this.config = config;
	}	

	/**
	 * Executes the operation synchronously and returns a result when 
	 * the operation exits. Using covariance is recommend, that is OperationFoo should
	 * override execute so as to return a OperationFooResult rather than OperationResult.   
	 *   
	 * @return Returns an operation-specific operation result
	 * @throws Exception If the operation fails
	 */
	public abstract OperationResult execute() throws Exception;

	protected void saveLocalDatabase(Database db, File localDatabaseFile) throws IOException {
		saveLocalDatabase(db, null, null, localDatabaseFile);
	}	
	
	protected void saveLocalDatabase(Database db, DatabaseVersion fromVersion, DatabaseVersion toVersion, File localDatabaseFile) throws IOException {
		logger.log(Level.INFO, "- Saving database to "+localDatabaseFile+" ...");
		
		DatabaseDAO dao = new XmlDatabaseDAO(config.getTransformer());
		dao.save(db, fromVersion, toVersion, localDatabaseFile);		
	}		
	
	protected Database loadLocalDatabase() throws IOException {
		return loadLocalDatabase(config.getDatabaseFile());
	}
	
	protected Database loadLocalDatabase(File localDatabaseFile) throws IOException {
		Database db = new Database();
		DatabaseDAO dao = new XmlDatabaseDAO(config.getTransformer());
		
		if (localDatabaseFile.exists()) {
			logger.log(Level.INFO, "- Loading database from "+localDatabaseFile+" ...");
			dao.load(db, localDatabaseFile);
		}
		else {
			logger.log(Level.INFO, "- NOT loading. File does not exist.");
		}
		
		return db;
	}
}
