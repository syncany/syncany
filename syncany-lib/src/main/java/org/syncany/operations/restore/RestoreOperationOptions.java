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
package org.syncany.operations.restore;

import java.util.Date;
import java.util.List;

import org.syncany.operations.OperationOptions;

public class RestoreOperationOptions implements OperationOptions {
	public static enum RestoreOperationStrategy {
		DATABASE_DATE, FILE_VERSION
	}
	
	private RestoreOperationStrategy strategy;
	private Date databaseBeforeDate;
	private Integer fileVersionNumber;
	private List<String> restoreFilePaths;

	public Date getDatabaseBeforeDate() {
		return databaseBeforeDate;
	}

	public void setDatabaseBeforeDate(Date databaseBeforeDate) {
		this.databaseBeforeDate = databaseBeforeDate;
	}

	public List<String> getRestoreFilePaths() {
		return restoreFilePaths;
	}

	public void setRestoreFilePaths(List<String> restoreFiles) {
		this.restoreFilePaths = restoreFiles;
	}

	public RestoreOperationStrategy getStrategy() {
		return strategy;
	}

	public void setStrategy(RestoreOperationStrategy strategy) {
		this.strategy = strategy;
	}

	public Integer getFileVersionNumber() {
		return fileVersionNumber;
	}

	public void setFileVersionNumber(Integer fileVersionNumber) {
		this.fileVersionNumber = fileVersionNumber;
	}
}