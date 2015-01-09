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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.ChangeSet;
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

		ArrayList<LightweightDatabaseVersion> databaseVersions = new ArrayList<>();
		Iterator<DatabaseVersion> databaseVersionsIterator = localDatabase.getLastDatabaseVersions(options.getMaxDatabaseVersionCount(),
				options.getStartDatabaseVersionIndex(), options.getMaxFileHistoryCount());
				
		while (databaseVersionsIterator.hasNext()) {
			DatabaseVersion databaseVersion = databaseVersionsIterator.next();
			LightweightDatabaseVersion lightweightDatabaseVersion = createLightweightDatabaseVersion(databaseVersion);			
			
			databaseVersions.add(lightweightDatabaseVersion);
		}
		
		return new LogOperationResult(databaseVersions);
	}

	private LightweightDatabaseVersion createLightweightDatabaseVersion(DatabaseVersion databaseVersion) {
		// Create changeset
		ChangeSet changedFiles = new ChangeSet();

		for (PartialFileHistory fileHistory : databaseVersion.getFileHistories()) {
			FileVersion fileVersion = fileHistory.getLastVersion();
			
			switch (fileVersion.getStatus()) {
			case NEW:
				changedFiles.getNewFiles().add(fileVersion.getPath());
				break;
				
			case CHANGED:
			case RENAMED:
				changedFiles.getChangedFiles().add(fileVersion.getPath());
				break;

			case DELETED:
				changedFiles.getDeletedFiles().add(fileVersion.getPath());
				break;
			}
		}
		
		// Create lightweight database version
		LightweightDatabaseVersion lightweightDatabaseVersion = new LightweightDatabaseVersion();

		lightweightDatabaseVersion.setClient(databaseVersion.getHeader().getClient());
		lightweightDatabaseVersion.setDate(databaseVersion.getHeader().getDate());
		lightweightDatabaseVersion.setChangeSet(changedFiles);
		
		return lightweightDatabaseVersion;
	}
}
