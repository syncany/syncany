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
package org.syncany.operations.daemon.messages;

import org.simpleframework.xml.Element;

public class CleanUpResponse extends Response {
	@Element(required = true)
	private String resultCode;
	
	@Element(required = true)
	private int mergedDatabaseFilesCount = 0;
	
	@Element(required = true)
	private int removedOldVersionsCount = 0;
	
	public CleanUpResponse() {
		// Required default constructor!
	}
	
	public CleanUpResponse(int requestId, String message) {
		super(200, requestId, message);
	}	
	
	public void setRemovedOldVersionsCount(int removedOldVersionsCount) {
		this.removedOldVersionsCount = removedOldVersionsCount;
	}
	public void setResultCode(String resultCode) {
		this.resultCode = resultCode;
	}
	public void setMergedDatabaseFilesCount(int mergedDatabaseFilesCount) {
		this.mergedDatabaseFilesCount = mergedDatabaseFilesCount;
	}
	public int getMergedDatabaseFilesCount() {
		return mergedDatabaseFilesCount;
	}
	public String getResultCode() {
		return resultCode;
	}
	public int getRemovedOldVersionsCount() {
		return removedOldVersionsCount;
	}
}
