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
package org.syncany.daemon.config;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class DeamonConfiguration {
	private String proxyHost;
	private String proxyPort;
	private String proxyUsername;
	private String proxyPassword;
	private String proxyType;
	private boolean proxyAuth;
	
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
	
	public static DeamonConfiguration from(DaemonConfigurationTO acto) {
		DeamonConfiguration ac = new DeamonConfiguration();
		ac.setProxyAuth(acto.isProxyAuth());
		ac.setProxyHost(acto.getProxyHost());
		ac.setProxyPassword(acto.getProxyPassword());
		ac.setProxyPort(acto.getProxyPort());
		ac.setProxyType(acto.getProxyType());
		ac.setProxyUsername(acto.getProxyUsername());
		return ac;
	}
	
}
