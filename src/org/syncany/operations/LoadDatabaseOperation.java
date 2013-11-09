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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.DatabaseDAO;
import org.syncany.database.XmlDatabaseDAO;

public class LoadDatabaseOperation extends Operation {
	private static final Logger logger = Logger.getLogger(LoadDatabaseOperation.class.getSimpleName());
	private File databaseFile;
	
	public LoadDatabaseOperation(Config config) {
		this(config, config.getDatabaseFile());
	}
	
	public LoadDatabaseOperation(Config config, File databaseFile) {
		super(config);
		this.databaseFile = databaseFile;
	}
	
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "Loading database file from "+databaseFile+" ...");
		
		Database db = new Database();
		DatabaseDAO dao = new XmlDatabaseDAO(config.getTransformer());
		
		if (databaseFile.exists()) {
			dao.load(db, databaseFile);
		}
		else {
			logger.log(Level.INFO, "- NOT loading. File does not exist.");
		}
		
		return new LoadDatabaseOperationResult(db);
	}		
	
	public class LoadDatabaseOperationResult implements OperationResult {
		private Database database;

		public LoadDatabaseOperationResult(Database database) {
			this.database = database;
		}

		public Database getDatabase() {
			return database;
		}
	}
}
