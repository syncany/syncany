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
package org.syncany.config.to;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * PortTO is the access object to the XML file written by the daemon in the client folder.
 * PortTO is used in the CLI to get access to the port information and a user-password
 * pair that can be used to authenticate with the REST-server.
 * 
 * @author Pim Otte
 */
@Root(name = "port", strict = false)
public class PortTO {
	@Element(name = "port", required = true)
	private int port;

	@Element(name = "user", required = true)
	private UserTO user;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public UserTO getUser() {
		return user;
	}

	public void setUser(UserTO user) {
		this.user = user;
	}
}
