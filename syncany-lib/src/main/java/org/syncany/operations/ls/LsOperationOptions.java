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
package org.syncany.operations.ls;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.syncany.database.FileVersion.FileType;
import org.syncany.operations.OperationOptions;

import com.google.common.collect.Sets;

public class LsOperationOptions implements OperationOptions {
	@Element(required = false)
	private Date date;

	@Element(required = false)
	private String pathExpression;
	
	@Element(required = false)
	private boolean fileHistoryId;

	@Element(required = false)
	private boolean recursive;

	@ElementList(required = false, entry = "fileType")
	private HashSet<FileType> fileTypes;

	@Element(required = false)
	private boolean fetchHistories;

	@Element(required = false)
	private boolean deleted;
	
	public LsOperationOptions() {
		this.date = null;
		this.pathExpression = null;
		this.fileHistoryId = false;
		this.recursive = false;
		this.fileTypes = Sets.newHashSet(FileType.FILE, FileType.FOLDER, FileType.SYMLINK);
		this.fetchHistories = false;
		this.deleted = false;
	}

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

	public boolean isFileHistoryId() {
		return fileHistoryId;
	}

	public void setFileHistoryId(boolean fileHistoryId) {
		this.fileHistoryId = fileHistoryId;
	}

	public boolean isRecursive() {
		return recursive;
	}

	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}

	public Set<FileType> getFileTypes() {
		return fileTypes;
	}

	public void setFileTypes(HashSet<FileType> fileTypes) {
		this.fileTypes = fileTypes;
	}

	public boolean isFetchHistories() {
		return fetchHistories;
	}

	public void setFetchHistories(boolean fetchHistories) {
		this.fetchHistories = fetchHistories;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}		
}
