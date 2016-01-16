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

import java.util.logging.Level;

import org.syncany.config.Config;
import org.syncany.operations.daemon.messages.api.FolderRequest;
import org.syncany.operations.daemon.messages.api.FolderRequestHandler;
import org.syncany.operations.daemon.messages.api.Response;
import org.syncany.operations.log.LogOperation;
import org.syncany.operations.log.LogOperationResult;

public class LogFolderRequestHandler extends FolderRequestHandler {
	public LogFolderRequestHandler(Config config) {
		super(config);		
	}

	@Override
	public Response handleRequest(FolderRequest request) {
		LogFolderRequest concreteRequest = (LogFolderRequest) request;

		try {
			LogOperation operation = new LogOperation(config, concreteRequest.getOptions());
			LogOperationResult operationResult = operation.execute();
			LogFolderResponse response = new LogFolderResponse(operationResult, request.getId(), request.getRoot());
		
			return response;
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Cannot obtain status.", e);
			return new BadRequestResponse(request.getId(), "Cannot execute operation: " + e.getMessage());
		}		
	}
}
