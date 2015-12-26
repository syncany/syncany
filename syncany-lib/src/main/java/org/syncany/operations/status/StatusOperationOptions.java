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
package org.syncany.operations.status;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.syncany.config.FileNameMatcher;
import org.syncany.operations.OperationOptions;

@Root(name="status")
public class StatusOperationOptions implements OperationOptions {
	@Element(required = false)
	private boolean forceChecksum = false;

	@Element(required = false)
	private boolean delete = true;
	
	@Element(required = false)
	private FileNameMatcher ignoredFilesPattern = null;

	public boolean isForceChecksum() {
		return forceChecksum;
	}

	public void setForceChecksum(boolean forceChecksum) {
		this.forceChecksum = forceChecksum;
	}
	
	public boolean isDelete() {
		return delete;
	}

	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	public FileNameMatcher getFilePattern() {
		return ignoredFilesPattern;
	}

	public void setIncludeFilePattern(FileNameMatcher filePattern) {
		this.ignoredFilesPattern = filePattern;
	}
}