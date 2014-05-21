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
package org.syncany.operations.cleanup;

import org.syncany.operations.OperationOptions;
import org.syncany.operations.status.StatusOperationOptions;

public class CleanupOperationOptions implements OperationOptions {
	private StatusOperationOptions statusOptions = new StatusOperationOptions();
	private boolean mergeRemoteFiles = true;
	private boolean removeOldVersions = true;
	private int keepVersionsCount = 5;
	private boolean repackageMultiChunks = true; // TODO [medium] Not used
	private double repackageUnusedThreshold = 0.7; // TODO [medium] Not used
	
	public StatusOperationOptions getStatusOptions() {
		return statusOptions;
	}

	public void setStatusOptions(StatusOperationOptions statusOptions) {
		this.statusOptions = statusOptions;
	}

	public boolean isMergeRemoteFiles() {
		return mergeRemoteFiles;
	}

	public boolean isRemoveOldVersions() {
		return removeOldVersions;
	}

	public double getRepackageUnusedThreshold() {
		return repackageUnusedThreshold;
	}

	public int getKeepVersionsCount() {
		return keepVersionsCount;
	}

	public boolean isRepackageMultiChunks() {
		return repackageMultiChunks;
	}

	public void setMergeRemoteFiles(boolean mergeRemoteFiles) {
		this.mergeRemoteFiles = mergeRemoteFiles;
	}

	public void setRemoveOldVersions(boolean removeOldVersions) {
		this.removeOldVersions = removeOldVersions;
	}

	public void setKeepVersionsCount(int keepVersionsCount) {
		this.keepVersionsCount = keepVersionsCount;
	}

	public void setRepackageMultiChunks(boolean repackageMultiChunks) {
		this.repackageMultiChunks = repackageMultiChunks;
	}

	public void setRepackageUnusedThreshold(double repackageUnusedThreshold) {
		this.repackageUnusedThreshold = repackageUnusedThreshold;
	}
}