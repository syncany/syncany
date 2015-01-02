/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
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
package org.syncany.operations.log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.Operation;

public class LogOperation extends Operation {
	private static final Logger logger = Logger.getLogger(LogOperation.class.getSimpleName());
	private LogOperationOptions options;
	private SqlDatabase localDatabase;

	public LogOperation(Config config, LogOperationOptions options) {
		super(config);

		this.options = options;
		this.localDatabase = new SqlDatabase(config);
	}

	@Override
	public LogOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Log' at client " + config.getMachineName() + " ...");
		logger.log(Level.INFO, "--------------------------------------------");

		Iterator<DatabaseVersion> databaseVersionsIterator = localDatabase.getLastDatabaseVersions(options.getMaxCount());
		List<DatabaseVersion> databaseVersions = new ArrayList<>();
				
		while (databaseVersionsIterator.hasNext()) {
			DatabaseVersion databaseVersion = databaseVersionsIterator.next();
			
			if (options.isExcludeChunkData()) {
				databaseVersion.getChunks().clear();
				databaseVersion.getMultiChunks().clear();
				databaseVersion.getFileContents().clear();
			}
			
			databaseVersions.add(databaseVersion);
		}
		
		return new LogOperationResult(databaseVersions);
	}
}
