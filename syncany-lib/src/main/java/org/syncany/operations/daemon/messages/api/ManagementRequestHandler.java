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
package org.syncany.operations.daemon.messages.api;

import java.util.logging.Logger;

import org.syncany.config.LocalEventBus;

public abstract class ManagementRequestHandler {
	protected static final Logger logger = Logger.getLogger(ManagementRequestHandler.class.getSimpleName());
	protected LocalEventBus eventBus;
	
	public ManagementRequestHandler() {
		this.eventBus = LocalEventBus.getInstance();
	}
	
	public abstract Response handleRequest(ManagementRequest request);
	
	public static ManagementRequestHandler createManagementRequestHandler(Request request) throws Exception {		
		String fqClassName = request.getClass().getName() + "Handler"; // TODO [medium] Ugly hardcoded string
		Class<?> clazz = Class.forName(fqClassName);

		return (ManagementRequestHandler) clazz.newInstance();
	}    
}
