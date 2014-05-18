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

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

@Root(name = "response", strict = false)
@Namespace(reference = "http://syncany.org/ws/1")
public class WebSocketResponse {
	@Element(required = true)
	private int code;
	
	@Element
	private int requestId;
	
	@Element(required = true)
	private String message;

	public WebSocketResponse(int code, int requestId, String message) {
		this.code = code;
		this.requestId = requestId;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public int getRequestId() {
		return requestId;
	}
	
	public String getMessage() {
		return message;
	}
}
