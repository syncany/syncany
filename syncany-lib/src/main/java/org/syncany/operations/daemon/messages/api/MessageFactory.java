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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.util.StringUtil;

/**
 * The message factory serializes and deserializes messages sent to
 * or from the daemon via the REST/WS API.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class MessageFactory {
	protected static final Logger logger = Logger.getLogger(MessageFactory.class.getSimpleName());

	protected static Class<? extends Message> getMessageClass(String requestType) throws Exception {
		String thisPackage = Message.class.getPackage().getName();
		String parentPackage = thisPackage.substring(0, thisPackage.lastIndexOf("."));
		String camelCaseMessageType = StringUtil.toCamelCase(requestType);
		String fqMessageClassName = parentPackage + "." + camelCaseMessageType;

		// Try to load!
		try {
			return Class.forName(fqMessageClassName).asSubclass(Message.class);
		}
		catch (Exception e) {
			logger.log(Level.INFO, "Could not find FQCN " + fqMessageClassName, e);
			throw new Exception("Cannot read request class from request type: " + requestType, e);
		}
	}
}