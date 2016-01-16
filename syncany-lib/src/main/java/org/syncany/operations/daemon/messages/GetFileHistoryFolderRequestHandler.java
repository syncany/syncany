/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import java.util.List;

import org.syncany.config.Config;
import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.daemon.messages.api.FolderRequest;
import org.syncany.operations.daemon.messages.api.FolderRequestHandler;
import org.syncany.operations.daemon.messages.api.Response;

@Deprecated
// TODO [medium] The file history id should be selectable via 'LsRequest'
public class GetFileHistoryFolderRequestHandler extends FolderRequestHandler {
	private SqlDatabase localDatabase;

	public GetFileHistoryFolderRequestHandler(Config config) {
		super(config);
		this.localDatabase = new SqlDatabase(config);
	}

	@Override
	public Response handleRequest(FolderRequest request) {
		GetFileHistoryFolderRequest concreteRequest = (GetFileHistoryFolderRequest) request;
		
		FileHistoryId fileHistoryId = FileHistoryId.parseFileId(concreteRequest.getFileHistoryId());
		List<FileVersion> fileHistory = localDatabase.getFileHistory(fileHistoryId);
		
		return new GetFileHistoryFolderResponse(concreteRequest.getId(), concreteRequest.getRoot(), fileHistory);			
	}
}
