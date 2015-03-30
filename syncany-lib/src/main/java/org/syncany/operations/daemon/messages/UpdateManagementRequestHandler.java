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

import org.syncany.operations.daemon.messages.api.ManagementRequest;
import org.syncany.operations.daemon.messages.api.ManagementRequestHandler;
import org.syncany.operations.daemon.messages.api.Response;
import org.syncany.operations.update.UpdateOperation;
import org.syncany.operations.update.UpdateOperationResult;

public class UpdateManagementRequestHandler extends ManagementRequestHandler {
	public UpdateManagementRequestHandler() {
		// Nothing
	}

	@Override
	public Response handleRequest(final ManagementRequest request) {
		final UpdateManagementRequest concreteRequest = (UpdateManagementRequest) request;		
		logger.log(Level.SEVERE, "Executing UpdateOperation for action " + concreteRequest.getOptions().getAction() + " ...");

		Thread updateThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {					
					UpdateOperation updateOperation = new UpdateOperation(null, concreteRequest.getOptions());
					UpdateOperationResult operationResult = updateOperation.execute();
					
					switch (operationResult.getResultCode()) {
					case OK:
						eventBus.post(new UpdateManagementResponse(UpdateManagementResponse.OK, operationResult, request.getId()));
						break;

					case NOK:
						eventBus.post(new UpdateManagementResponse(UpdateManagementResponse.NOK_FAILED_UNKNOWN, operationResult, request.getId()));
						break;
					}
				}
				catch (Exception e) {
					logger.log(Level.WARNING, "Error executing update management request.", e);
					eventBus.post(new UpdateManagementResponse(UpdateManagementResponse.NOK_OPERATION_FAILED, new UpdateOperationResult(), request.getId()));
				}
			}
		}, "UpdRq/" + concreteRequest.getOptions().getAction());

		updateThread.start();
		
		return null;
	}
}
