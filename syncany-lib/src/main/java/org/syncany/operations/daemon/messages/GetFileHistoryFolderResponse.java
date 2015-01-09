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
package org.syncany.operations.daemon.messages;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.syncany.database.FileVersion;
import org.syncany.operations.daemon.messages.api.FolderResponse;

public class GetFileHistoryFolderResponse extends FolderResponse {
	@Element(required = true)
	private String root;

	@ElementList(required = true, entry="file")
	private ArrayList<FileVersion> files;	
	
	public GetFileHistoryFolderResponse(int requestId, String root, List<FileVersion> files) {
		super(200, requestId, null);
		
		this.root = root;
		this.files = new ArrayList<FileVersion>(files);
	}	
	
	public List<FileVersion> getFiles() {
		return files;
	}
}
