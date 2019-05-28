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
package org.syncany.operations.daemon.messages.api;

import java.util.logging.Level;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author Christian Roth (christian.roth@port17.de)
 */
public abstract class JsonMessageFactory extends MessageFactory {
	private static final Gson SERIALIZER = new Gson();
	private static final JsonParser PARSER = new JsonParser();

	public static Response toResponse(String responseMessageString) throws Exception {
		Message responseMessage = toMessage(responseMessageString);

		if (!(responseMessage instanceof Response)) {
			throw new Exception("Invalid class: Message is not a response type: " + responseMessage.getClass());
		}

		return (Response) responseMessage;
	}

	public static Request toRequest(String responseMessageString) throws Exception {
		Message requestMessage = toMessage(responseMessageString);

		if (!(requestMessage instanceof Request)) {
			throw new Exception("Invalid class: Message is not a request type:" + requestMessage.getClass());
		}

		return (Request) requestMessage;
	}

	public static Message toMessage(String messageStr) throws Exception {
		String messageType = getMessageType(messageStr);
		Class<? extends Message> messageClass = getMessageClass(messageType);

		JsonObject result = PARSER.parse(messageStr).getAsJsonObject();
		JsonElement serializedMessage = Optional.fromNullable(Iterables.get(result.entrySet(), 0).getValue()).or(new JsonObject());

		Message message = SERIALIZER.fromJson(serializedMessage, messageClass);
		logger.log(Level.INFO, "Message created: " + message);

		return message;
	}

	public static String toJson(Message response) throws Exception {
		JsonElement je = SERIALIZER.toJsonTree(response);
		JsonObject jo = new JsonObject();
		jo.add(response.getClass().getSimpleName(), je);
		return jo.toString();
	}

	/** 
	 * Parses JSON message for message type. 
	 * First key contains message name:
	 * 
	 * <pre>
	 * { "messageType":
	 *   {
	 *     // payload
	 *   }
	 * }
	 * </pre>
	 */
	private static String getMessageType(String message) throws Exception {
		try {
			JsonObject result = PARSER.parse(message).getAsJsonObject();
			return Iterables.get(result.entrySet(), 0).getKey();
		}
		catch (Exception e) {
			throw new Exception("Cannot find type of message. Invalid JSON: " + message);
		}
	}
}
