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

import java.io.File;
import java.util.ArrayList;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.ConfigException;

@Root(name = "daemon", strict = false)
public class DaemonConfigTO {
	@Element(name = "webServer", required = false)
	private WebServerTO webServer = new WebServerTO();
	
	@ElementList(name = "folders", entry = "folder", required = true)
	private ArrayList<FolderTO> folders = new ArrayList<FolderTO>();

	@ElementList(name = "users", entry = "user", required = false)
	private ArrayList<UserTO> users = new ArrayList<UserTO>();

	private PortTO portTO; // This is generated dynamically by the daemon. It should't be in the XML.
	
	public static DaemonConfigTO load(File file) throws ConfigException {
		try {
			return new Persister().read(DaemonConfigTO.class, file);
		}
		catch (Exception e) {
			throw new ConfigException("Config file does not exist or is invalid: " + file, e);
		}
	}
	
	public static void save(DaemonConfigTO daemonConfigTO, File file) throws ConfigException {
		try {
			new Persister().write(daemonConfigTO, file);
		}
		catch (Exception e) {
			throw new ConfigException("Config could not be written: " + file, e);
		}
	}
	
	public ArrayList<FolderTO> getFolders() {
		return folders;
	}
	
	public void setFolders(ArrayList<FolderTO> folders) {
		this.folders = folders;
	}
	
	public ArrayList<UserTO> getUsers() {
		return users;
	}
	
	public void setUsers(ArrayList<UserTO> users) {
		this.users = users;
	}

	public WebServerTO getWebServer() {
		return webServer;
	}

	public void setWebServer(WebServerTO webServer) {
		this.webServer = webServer;
	}
	
	public PortTO getPortTO() {
		return portTO;
	}
	
	public void setPortTO(PortTO portTO) {
		this.portTO = portTO;
	}
}
