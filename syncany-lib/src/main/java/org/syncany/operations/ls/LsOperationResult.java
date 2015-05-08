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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;
import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.operations.OperationResult;

public class LsOperationResult implements OperationResult {
	@ElementList(name = "fileList", required = false, entry = "fileVersion")
	private ArrayList<FileVersion> fileList;
	
	@ElementMap(name = "fileVersions", required = false, key = "fileHistoryId", value = "partialFileHistory")
	private HashMap<FileHistoryId, PartialFileHistory> fileVersions;
	
	public LsOperationResult() {
		// Nothing
	}
	
	public LsOperationResult(List<FileVersion> fileList, Map<FileHistoryId, PartialFileHistory> fileVersions) {
		this.fileList = (fileList != null) ? new ArrayList<>(fileList) : null;
		this.fileVersions = (fileVersions != null) ? new HashMap<>(fileVersions) : null;
	}

	public List<FileVersion> getFileList() {
		return fileList;
	}

	public Map<FileHistoryId, PartialFileHistory> getFileVersions() {
		return fileVersions;
	}
}