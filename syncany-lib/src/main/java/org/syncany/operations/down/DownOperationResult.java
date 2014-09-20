/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.operations.down;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.operations.ChangeSet;
import org.syncany.operations.OperationResult;
import org.syncany.operations.ls_remote.LsRemoteOperationResult;

public class DownOperationResult implements OperationResult {
	public enum DownResultCode {
		OK_NO_REMOTE_CHANGES, OK_WITH_REMOTE_CHANGES, NOK
	}
	
	private DownResultCode resultCode;
	private ChangeSet changeSet = new ChangeSet();
	private List<DatabaseVersionHeader> dirtyDatabasesCreated = new ArrayList<DatabaseVersionHeader>();
	private Set<String> downloadedUnknownDatabases = new HashSet<String>();
	private Set<MultiChunkId> downloadedMultiChunks = new HashSet<MultiChunkId>();
	private LsRemoteOperationResult lsRemoteResult = null;

	public DownResultCode getResultCode() {
		return resultCode;
	}

	public void setResultCode(DownResultCode resultCode) {
		this.resultCode = resultCode;
	}

	public void setChangeSet(ChangeSet ChangeSet) {
		this.changeSet = ChangeSet;
	}

	public ChangeSet getChangeSet() {
		return changeSet;
	}

	public List<DatabaseVersionHeader> getDirtyDatabasesCreated() {
		return dirtyDatabasesCreated;
	}

	public void setDirtyDatabasesCreated(List<DatabaseVersionHeader> dirtyDatabasesCreated) {
		this.dirtyDatabasesCreated = dirtyDatabasesCreated;
	}

	public Set<String> getDownloadedUnknownDatabases() {
		return downloadedUnknownDatabases;
	}

	public void setDownloadedUnknownDatabases(Set<String> downloadedUnknownDatabases) {
		this.downloadedUnknownDatabases = downloadedUnknownDatabases;
	}

	public Set<MultiChunkId> getDownloadedMultiChunks() {
		return downloadedMultiChunks;
	}

	public void setDownloadedMultiChunks(Set<MultiChunkId> downloadedMultiChunks) {
		this.downloadedMultiChunks = downloadedMultiChunks;
	}

	public LsRemoteOperationResult getLsRemoteResult() {
		return lsRemoteResult;
	}

	public void setLsRemoteResult(LsRemoteOperationResult lsRemoteResult) {
		this.lsRemoteResult = lsRemoteResult;
	}
}