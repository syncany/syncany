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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.util.StringUtil;

public class RequestFactory {
	private static final Logger logger = Logger.getLogger(RequestFactory.class.getSimpleName());
	private static final Pattern REQUEST_TYPE_PATTERN = Pattern.compile("^\\<([^>\\s]+)");
	private static final int REQUEST_TYPE_PATTERN_GROUP = 1;
	
	private static Serializer serializer = new Persister();
	
	public static Request createRequest(String requestMessage) throws Exception {
		String requestType = getRequestType(requestMessage);			
		Class<? extends Request> requestClass = getRequestClass(requestType);
		
		Request request = serializer.read(requestClass, requestMessage);
		logger.log(Level.INFO, "Request received: " + request);
		
		return request;
	}
	
	private static String getRequestType(String requestMessage) throws Exception {
		Matcher requestTypeMatcher = REQUEST_TYPE_PATTERN.matcher(requestMessage);
		
		if (requestTypeMatcher.find()) {
			return requestTypeMatcher.group(REQUEST_TYPE_PATTERN_GROUP);
		}
		else {
			throw new Exception("Cannot find request type of message. Invalid XML.");
		}
	}
	
	private static Class<? extends Request> getRequestClass(String requestType) throws Exception {
		String thisPackage = RequestFactory.class.getPackage().getName();
		String camelCaseRequestType = StringUtil.toCamelCase(requestType);
		String fqRequestClassName = thisPackage + "." + camelCaseRequestType;

		// Try to load!
		try {		
			Class<? extends Request> requestClass = Class.forName(fqRequestClassName).asSubclass(Request.class);
			return requestClass;
		} 
		catch (Exception e) {
			logger.log(Level.INFO, "Could not find FQCN " + fqRequestClassName, e);
			throw new Exception("Cannot read request class from request type: " + requestType, e);
		}		
	}
}