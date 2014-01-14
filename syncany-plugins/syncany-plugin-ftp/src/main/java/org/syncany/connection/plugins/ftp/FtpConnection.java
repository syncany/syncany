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
package org.syncany.connection.plugins.ftp;

import java.util.Map;

import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;

/**
 * The FTP connection represents the settings required to connect to an
 * FTP-based storage backend. It can be used to initialize/create an 
 * {@link FtpTransferManager} and is part of the {@link FtpPlugin}.  
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FtpConnection implements Connection {
    private String hostname;
    private String username;
    private String password;
    private String path;
    private int port;

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
	public void init(Map<String, String> map) throws StorageException {
		// Mandatory
		String hostname = map.get("hostname");
		String username = map.get("username");
		String password = map.get("password");
		String path = map.get("path");
		
		// Optional
		String portStr = map.get("port");
		
		if (hostname == null || username == null || password == null || path == null) {
			throw new StorageException("Mandatory fields missing for FTP configuration: hostname, username, password and path.");
		}
		
		this.hostname = hostname;
		this.username = username;
		this.password = password;
		this.path = path;
		
		if (portStr != null) {
			try {
				this.port = Integer.parseInt(portStr);
			}
			catch (NumberFormatException e) {
				throw new StorageException("Invalid port number given: "+portStr, e);
			}
		}
		else {
			this.port = 21;
		}
	}


	@Override
	public String[] getMandatorySettings() {
		return new String[] { "hostname", "username", "password", "path" };
	}

	@Override
	public String[] getOptionalSettings() {
		return new String[] { "port" };
	}
	
    @Override
    public String toString() {
        return FtpConnection.class.getSimpleName()
        + "[hostname=" + hostname + ":" + port + ", username=" + username + ", path=" + path + "]";
    }
}
