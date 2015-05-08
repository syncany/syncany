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
import org.syncany.operations.plugin.PluginOperation;
import org.syncany.operations.plugin.PluginOperationResult;

public class PluginManagementRequestHandler extends ManagementRequestHandler {
	public PluginManagementRequestHandler() {
		// Nothing
	}

	@Override
	public Response handleRequest(final ManagementRequest request) {
		final PluginManagementRequest concreteRequest = (PluginManagementRequest) request;		
		logger.log(Level.SEVERE, "Executing PluginOperation for action " + concreteRequest.getOptions().getAction() + " ...");

		Thread pluginThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {					
					PluginOperation pluginOperation = new PluginOperation(null, concreteRequest.getOptions());
					PluginOperationResult operationResult = pluginOperation.execute();
					
					switch (operationResult.getResultCode()) {
					case OK:
						eventBus.post(new PluginManagementResponse(PluginManagementResponse.OK, operationResult, request.getId()));
						break;

					case NOK:
						eventBus.post(new PluginManagementResponse(PluginManagementResponse.NOK_FAILED_UNKNOWN, operationResult, request.getId()));
						break;
					}
				}
				catch (Exception e) {
					logger.log(Level.WARNING, "Error executing plugin management request.", e);
					eventBus.post(new PluginManagementResponse(PluginManagementResponse.NOK_OPERATION_FAILED, new PluginOperationResult(), request.getId()));
				}
			}
		}, "PlugRq/" + concreteRequest.getOptions().getAction());

		pluginThread.start();
		
		return null;
	}
}
