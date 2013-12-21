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
package org.syncany.connection.plugins.webdav;

import java.util.Map;

import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;

public class WebdavConnection implements Connection {
	private String url;
	private String username;
	private String password;

	@Override
	public TransferManager createTransferManager() {
		return new WebdavTransferManager(this);
	}

	@Override
	public void init(Map<String, String> map) throws StorageException {
		// Mandatory
		String url = map.get("url");
		String username = map.get("username");
		String password = map.get("password");

		if (url == null || username == null || password == null) {
			throw new StorageException("Mandatory fields missing for Webdav configuration: url, username and password.");
		}

		this.url = url;
		this.username = username;
		this.password = password;
	}

	@Override
	public String[] getMandatorySettings() {
		return new String[] { "url", "username", "password" };
	}

	@Override
	public String[] getOptionalSettings() {
		return new String[] {};
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
}
