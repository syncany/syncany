/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com>
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
package org.syncany.operations.ls_remote;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.config.LocalEventBus;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.Operation;
import org.syncany.operations.daemon.messages.LsRemoteEndSyncExternalEvent;
import org.syncany.operations.daemon.messages.LsRemoteStartSyncExternalEvent;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.TransferManagerFactory;
import org.syncany.plugins.transfer.features.PathAware;
import org.syncany.plugins.transfer.files.DatabaseRemoteFile;

/**
 * The list remote operation queries the transfer manager for any unknown
 * {@link DatabaseRemoteFile}s.
 *
 * <p>It first uses a {@link TransferManager} to list all remote databases and then
 * uses the local list of known databases to filter already processed files. The local
 * list of known databases is loaded.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class LsRemoteOperation extends Operation {
	private static final Logger logger = Logger.getLogger(LsRemoteOperation.class.getSimpleName());

	private TransferManager loadedTransferManager;
	private SqlDatabase localDatabase;
	private LocalEventBus eventBus;

	public LsRemoteOperation(Config config) {
		this(config, null);
	}

	public LsRemoteOperation(Config config, TransferManager transferManager) {
		super(config);

		this.loadedTransferManager = transferManager;
		this.localDatabase = new SqlDatabase(config);
		this.eventBus = LocalEventBus.getInstance();
	}

	@Override
	public LsRemoteOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Remote Status' at client " + config.getMachineName() + " ...");
		logger.log(Level.INFO, "--------------------------------------------");

		eventBus.post(new LsRemoteStartSyncExternalEvent(config.getLocalDir().getAbsolutePath()));

		TransferManager transferManager = createTransferManager(loadedTransferManager);

		List<DatabaseRemoteFile> knownDatabases = localDatabase.getKnownDatabases();
		List<DatabaseRemoteFile> unknownRemoteDatabases = listUnknownRemoteDatabases(transferManager, knownDatabases);

		transferManager.disconnect();

		boolean hasChanges = unknownRemoteDatabases.size() > 0;
		eventBus.post(new LsRemoteEndSyncExternalEvent(config.getLocalDir().getAbsolutePath(), hasChanges));

		return new LsRemoteOperationResult(new ArrayList<>(unknownRemoteDatabases));
	}

	private TransferManager createTransferManager(TransferManager loadedTransferManager) throws StorageException {
		if (loadedTransferManager != null) {
			return loadedTransferManager;
		}
		else {
			return TransferManagerFactory
					.build(config)
					.withFeature(PathAware.class)
					.asDefault();
		}
	}

	private List<DatabaseRemoteFile> listUnknownRemoteDatabases(TransferManager transferManager, List<DatabaseRemoteFile> knownDatabases)
			throws StorageException {
		logger.log(Level.INFO, "Retrieving remote database list.");

		List<DatabaseRemoteFile> unknownRemoteDatabases = new ArrayList<DatabaseRemoteFile>();

		// List all remote database files
		Map<String, DatabaseRemoteFile> remoteDatabaseFiles = transferManager.list(DatabaseRemoteFile.class);

		for (DatabaseRemoteFile remoteDatabaseFile : remoteDatabaseFiles.values()) {
			// This does NOT filter 'lock' files!
			if (knownDatabases.contains(remoteDatabaseFile)) {
				logger.log(Level.INFO, "- Remote database {0} is already known (in local database). Ignoring.", remoteDatabaseFile.getName());
			}
			else {
				logger.log(Level.INFO, "- Remote database {0} is new.", remoteDatabaseFile.getName());
				unknownRemoteDatabases.add(remoteDatabaseFile);
			}
		}

		return unknownRemoteDatabases;
	}
}
