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

import java.io.StringWriter;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.simpleframework.xml.Serializer;
import org.syncany.config.to.XmlSerializer;

/**
 * Factory class to serialize and deserialize {@link Message}s from/to 
 * XML (via SimpleXML).  
 * 
 * @see <a href="http://simple.sourceforge.net/">Simple framework</a>
 * @author Christian Roth <christian.roth@port17.de>
 */
public abstract class XmlMessageFactory extends MessageFactory {
	private static final Pattern MESSAGE_TYPE_PATTERN = Pattern.compile("\\<([^\\/>\\s]+)");
	private static final int MESSAGE_TYPE_PATTERN_GROUP = 1;

	private static final Serializer serializer = XmlSerializer.getInstance();

	public static Response toResponse(String responseMessageXml) throws Exception {
		Message responseMessage = toMessage(responseMessageXml);

		if (!(responseMessage instanceof Response)) {
			throw new Exception("Invalid class: Message is not a response type: " + responseMessage.getClass());
		}

		return (Response) responseMessage;
	}

	public static Request toRequest(String requestMessageXml) throws Exception {
		Message requestMessage = toMessage(requestMessageXml);

		if (!(requestMessage instanceof Request)) {
			throw new Exception("Invalid class: Message is not a request type: " + requestMessage.getClass());
		}

		return (Request) requestMessage;
	}

	public static Message toMessage(String messageStr) throws Exception {
		String messageType = getMessageType(messageStr);
		Class<? extends Message> messageClass = getMessageClass(messageType);

		Message message = serializer.read(messageClass, messageStr);
		logger.log(Level.INFO, "Message created: " + message);

		return message;
	}

	public static String toXml(Message response) throws Exception {
		StringWriter messageWriter = new StringWriter();
		serializer.write(response, messageWriter);

		return messageWriter.toString();
	}

	private static String getMessageType(String message) throws Exception {
		Matcher messageTypeMatcher = MESSAGE_TYPE_PATTERN.matcher(message);

		if (messageTypeMatcher.find()) {
			return messageTypeMatcher.group(MESSAGE_TYPE_PATTERN_GROUP);
		}
		else {
			throw new Exception("Cannot find type of message. Invalid XML: " + message);
		}
	}
}
