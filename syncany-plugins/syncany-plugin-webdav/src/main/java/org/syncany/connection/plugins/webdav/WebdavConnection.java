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
package org.syncany.connection.plugins.webdav;

import java.util.Map;

import org.syncany.config.Config;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.PluginListener;
import org.syncany.connection.plugins.PluginOptionSpec;
import org.syncany.connection.plugins.PluginOptionSpec.ValueType;
import org.syncany.connection.plugins.PluginOptionSpecs;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;

public class WebdavConnection implements Connection {
	private Config config;
	private PluginListener pluginListener;
	
	private String url;
	private String username;
	private String password;
	private boolean secure;

	@Override
	public TransferManager createTransferManager() {
		return new WebdavTransferManager(this, pluginListener);
	}

	@Override
	public void init(Config config, Map<String, String> optionValues, PluginListener pluginListener) throws StorageException {
		getOptionSpecs().validate(optionValues);
		
		this.config = config;
		this.pluginListener = pluginListener;
		
		this.url = optionValues.get("url");
		this.username = optionValues.get("username");
		this.password = optionValues.get("password");
		this.secure = url.toLowerCase().startsWith("https");
	}

	@Override
	public PluginOptionSpecs getOptionSpecs() {
		return new PluginOptionSpecs(
			new PluginOptionSpec("url", "URL (incl. path & port)", ValueType.STRING, true, false, null),
			new PluginOptionSpec("username", "Username", ValueType.STRING, true, false, null),
			new PluginOptionSpec("password", "Password", ValueType.STRING, true, true, null)
		);				
	}

	@Override
	public String toString() {
		return WebdavConnection.class.getSimpleName() + "[url=" + url + ", username=" + username + "]";
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getURL(String filename) {
		return (url.endsWith("/") ? "" : "/") + filename;
	}

	public boolean isSecure() {
		return secure;
	}
	
	public Config getConfig() {
		return config;
	}
}
