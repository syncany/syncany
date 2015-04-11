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
package org.syncany.operations.cleanup;

import org.simpleframework.xml.Element;
import org.syncany.operations.OperationResult;

public class CleanupOperationResult implements OperationResult {
	public enum CleanupResultCode {
		OK, OK_NOTHING_DONE, NOK_REMOTE_CHANGES, NOK_RECENTLY_CLEANED, NOK_LOCAL_CHANGES, NOK_DIRTY_LOCAL, NOK_OTHER_OPERATIONS_RUNNING, NOK_REPO_BLOCKED
	}

	@Element(name = "resultCode", required = true)
	private CleanupResultCode resultCode;
	
	@Element(name = "mergedDatabaseFilesCount", required = false)
	private int mergedDatabaseFilesCount;
	
	@Element(name = "removedOldVersionsCount", required = false)
	private int removedOldVersionsCount;
	
	@Element(name = "removedMultiChunksCount", required = false)
	private int removedMultiChunksCount;

	@Element(name = "removedMultiChunksSize", required = false)
	private long removedMultiChunksSize;

	public CleanupOperationResult() {
		this(CleanupResultCode.OK_NOTHING_DONE);
	}

	public CleanupOperationResult(CleanupResultCode resultCode) {
		this.resultCode = resultCode;
		this.mergedDatabaseFilesCount = 0;
		this.removedOldVersionsCount = 0;
		this.removedMultiChunksCount = 0;
		this.removedMultiChunksSize = 0L;
	}

	public void setResultCode(CleanupResultCode resultCode) {
		this.resultCode = resultCode;
	}

	public CleanupResultCode getResultCode() {
		return resultCode;
	}

	public int getMergedDatabaseFilesCount() {
		return mergedDatabaseFilesCount;
	}

	public void setMergedDatabaseFilesCount(int mergedDatabaseFilesCount) {
		this.mergedDatabaseFilesCount = mergedDatabaseFilesCount;
	}

	public int getRemovedOldVersionsCount() {
		return removedOldVersionsCount;
	}

	public void setRemovedOldVersionsCount(int removedOldVersionsCount) {
		this.removedOldVersionsCount = removedOldVersionsCount;
	}

	public int getRemovedMultiChunksCount() {
		return removedMultiChunksCount;
	}

	public void setRemovedMultiChunksCount(int removedMultiChunksCount) {
		this.removedMultiChunksCount = removedMultiChunksCount;
	}

	public long getRemovedMultiChunksSize() {
		return removedMultiChunksSize;
	}

	public void setRemovedMultiChunksSize(long removedMultiChunksSize) {
		this.removedMultiChunksSize = removedMultiChunksSize;
	}
}