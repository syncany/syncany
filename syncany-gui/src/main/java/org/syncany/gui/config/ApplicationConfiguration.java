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
package org.syncany.gui.config;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class ApplicationConfiguration {
	private String proxyHost;
	private String proxyPort;
	private String proxyUsername;
	private String proxyPassword;
	private String proxyType;
	private String proxyAuthType;
	private boolean proxyAuth;
	public List<String> watchedFolders;

	public List<String> getWatchedFolders() {
		return watchedFolders;
	}
	
	public void addWatchedFolder(String folder){
		if (watchedFolders == null) watchedFolders = new ArrayList<>();
		if (!watchedFolders.contains(folder)) watchedFolders.add(folder);
	}

	public void setWatchedFolders(List<String> watchedFolders) {
		this.watchedFolders = watchedFolders;
	}
	
	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public String getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(String proxyPort) {
		this.proxyPort = proxyPort;
	}

	public String getProxyUsername() {
		return proxyUsername;
	}

	public void setProxyUsername(String proxyUsername) {
		this.proxyUsername = proxyUsername;
	}

	public String getProxyPassword() {
		return proxyPassword;
	}

	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}

	public boolean isProxyAuth() {
		return proxyAuth;
	}

	public void setProxyAuth(boolean proxyAuth) {
		this.proxyAuth = proxyAuth;
	}
	
	public String getProxyType() {
		return proxyType;
	}
	
	public void setProxyType(String proxyType) {
		this.proxyType = proxyType;
	}

	public String getProxyAuthType() {
		return proxyAuthType;
	}

	public void setProxyAuthType(String proxyAuthType) {
		this.proxyAuthType = proxyAuthType;
	}
	
	public static ApplicationConfiguration from(ApplicationConfigurationTO from) {
		ApplicationConfiguration to = new ApplicationConfiguration();
		to.setProxyAuth(from.isProxyAuth());
		to.setProxyHost(from.getProxyHost());
		to.setProxyPassword(from.getProxyPassword());
		to.setProxyPort(from.getProxyPort());
		to.setProxyType(from.getProxyType());
		to.setProxyUsername(from.getProxyUsername());
		to.setProxyAuthType(from.getProxyAuthType());
		to.setWatchedFolders(from.getWatchedFolders());
		return to;
	}

	public static ApplicationConfigurationTO toTO(ApplicationConfiguration to) {
		ApplicationConfigurationTO from = new ApplicationConfigurationTO();
		from.setProxyAuth(to.isProxyAuth());
		from.setProxyHost(to.getProxyHost());
		from.setProxyPassword(to.getProxyPassword());
		from.setProxyPort(to.getProxyPort());
		from.setProxyType(to.getProxyType());
		from.setProxyAuthType(to.getProxyAuthType());
		from.setWatchedFolders(to.getWatchedFolders());
		return from;
	}
}
