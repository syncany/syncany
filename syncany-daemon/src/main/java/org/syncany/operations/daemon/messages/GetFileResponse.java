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

public class GetFileResponse extends Response {
	@Element(required = true)
	private String name;
	
	@Element(required = true)
	private int length;
	
	@Element(required = true)
	private int frames;
	
	@Element(required = false)
	private String mimeType;

	public GetFileResponse(int requestId, String name, int length, int frames, String mimeType) {
		super(200, requestId, null);
		
		this.name = name;
		this.length = length;
		this.frames = frames;
		this.mimeType = mimeType;
	}

	public String getName() {
		return name;
	}

	public int getLength() {
		return length;
	}

	public int getFrames() {
		return frames;
	}

	public String getMimeType() {
		return mimeType;
	}
}
