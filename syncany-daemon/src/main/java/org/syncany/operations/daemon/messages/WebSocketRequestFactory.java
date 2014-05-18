/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
import java.util.logging.Logger;

import org.syncany.util.StringUtil;

public class WebSocketRequestFactory {
	private static final Logger logger = Logger.getLogger(WebSocketRequestFactory.class.getSimpleName());
	
	public static Class<? extends WebSocketRequest> getRequestClass(String requestType) {
		String thisPackage = WebSocketRequestFactory.class.getPackage().getName();
		String camelCaseRequestType = StringUtil.toCamelCase(requestType);
		String fqRequestClassName = thisPackage+"."+camelCaseRequestType+WebSocketRequest.class.getSimpleName();
		
		// Try to load!
		try {
			
			Class<? extends WebSocketRequest> requestClass = Class.forName(fqRequestClassName).asSubclass(WebSocketRequest.class);
			return requestClass;
		} 
		catch (Exception ex) {
			logger.log(Level.INFO, "Could not find FQCN " + fqRequestClassName, ex);
			return null;
		}		
	}
	
	public static WebSocketRequest getInstance(String requestType) {
		String thisPackage = WebSocketRequestFactory.class.getPackage().getName();
		String camelCaseRequestType = StringUtil.toCamelCase(requestType);
		String fqRequestClassName = thisPackage+"."+camelCaseRequestType+WebSocketRequest.class.getSimpleName();
		
		// Try to load!
		try {
			Class<?> requestClass = Class.forName(fqRequestClassName);
			return (WebSocketRequest) requestClass.newInstance();
		} 
		catch (Exception ex) {
			logger.log(Level.INFO, "Could not find FQCN " + fqRequestClassName, ex);
			return null;
		}		
	}
}