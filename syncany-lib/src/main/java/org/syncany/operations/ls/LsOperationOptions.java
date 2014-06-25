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
package org.syncany.operations.ls;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.syncany.database.FileVersion.FileType;
import org.syncany.operations.OperationOptions;

public class LsOperationOptions implements OperationOptions {
	private Date date = null;
	private String pathExpression = null;	
	private boolean recursive = false;
	private List<FileType> fileTypes = Arrays.asList(new FileType[] { FileType.FILE, FileType.FOLDER, FileType.SYMLINK });
	private boolean fetchHistories;
	
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getPathExpression() {
		return pathExpression;
	}

	public void setPathExpression(String pathExpression) {
		this.pathExpression = pathExpression;
	}

	public boolean isRecursive() {
		return recursive;
	}

	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}

	public List<FileType> getFileTypes() {
		return fileTypes;
	}

	public void setFileTypes(List<FileType> fileTypes) {
		this.fileTypes = fileTypes;
	}

	public boolean isFetchHistories() {
		return fetchHistories;
	}

	public void setFetchHistories(boolean fetchHistories) {
		this.fetchHistories = fetchHistories;
	}	
}