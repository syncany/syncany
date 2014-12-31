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

import java.util.logging.Level;

import org.syncany.operations.daemon.EventUserInteractionListener;
import org.syncany.operations.daemon.messages.api.ManagementRequest;
import org.syncany.operations.daemon.messages.api.ManagementRequestHandler;
import org.syncany.operations.daemon.messages.api.Response;
import org.syncany.operations.init.InitOperation;
import org.syncany.operations.init.InitOperationResult;

public class InitManagementRequestHandler extends ManagementRequestHandler {
	public InitManagementRequestHandler() {
		// Nothing
	}

	@Override
	public Response handleRequest(final ManagementRequest request) {
		final InitManagementRequest concreteRequest = (InitManagementRequest) request;		
		logger.log(Level.SEVERE, "Executing InitOperation for folder " + concreteRequest.getOptions().getLocalDir() + " ...");

		Thread initThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					InitOperation initOperation = new InitOperation(concreteRequest.getOptions(), new EventUserInteractionListener());
					InitOperationResult operationResult = initOperation.execute();

					switch (operationResult.getResultCode()) {
					case OK:
						eventBus.post(new InitManagementResponse(InitManagementResponse.OK, operationResult, request.getId()));
						break;

					case NOK_TEST_FAILED:
						eventBus.post(new InitManagementResponse(InitManagementResponse.NOK_FAILED_TEST, operationResult, request.getId()));
						break;

					default:
						eventBus.post(new InitManagementResponse(InitManagementResponse.NOK_FAILED_UNKNOWN, operationResult, request.getId()));
						break;
					}
				}
				catch (Exception e) {
					logger.log(Level.WARNING, "Error adding watch to daemon config.", e);
					eventBus.post(new InitManagementResponse(InitManagementResponse.NOK_OPERATION_FAILED, new InitOperationResult(), request.getId()));
				}
			}
		}, "IntRq/" + concreteRequest.getOptions().getLocalDir().getName());

		initThread.start();
		
		return null;
	}
}
