/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.connection.plugins.ftp;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.PluginSetting;
import org.syncany.connection.plugins.PluginSetting.ValueType;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;

/**
 * The FTP connection represents the settings required to connect to an
 * FTP-based storage backend. It can be used to initialize/create an 
 * {@link FtpTransferManager} and is part of the {@link FtpPlugin}.  
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FtpConnection extends Connection {
    private String hostname;
    private String username;
    private String password;
    private String path;
    private int port;
    private Map<String, PluginSetting> settings = null;

    @Override
    public TransferManager createTransferManager() {
        return new FtpTransferManager(this);
    }
  
    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {        
        this.path = path;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

	@Override
	public void init() {
		Map<String, PluginSetting> map = getSettings();
		// Mandatory
		String hostname = map.get("hostname").getValue();
		String username = map.get("username").getValue();
		String password = map.get("password").getValue();
		String path = map.get("path").getValue();
		
		// Optional
		String portStr = map.get("port").getValue();
		
		this.hostname = hostname;
		this.username = username;
		this.password = password;
		this.path = path;
		this.port = Integer.parseInt(portStr);
	}

	@Override 
	public Map<String,PluginSetting> getSettings() {
		if (settings == null) {
			settings = new TreeMap<String, PluginSetting>();
			settings.put("hostname", new PluginSetting(ValueType.STRING,  true, false));
			settings.put("password", new PluginSetting(ValueType.STRING,  true, true));
			settings.put("path", new PluginSetting(ValueType.STRING,  true, false));
			settings.put("port", new PluginSetting(ValueType.INT,  "21", false));
		}
		return settings;
	}
	
    @Override
    public String toString() {
        return FtpConnection.class.getSimpleName()
        + "[hostname=" + hostname + ":" + port + ", username=" + username + ", path=" + path + "]";
    }
}
