/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.operations.init;

import java.io.File;

import org.syncany.config.to.ConfigTO;
import org.syncany.operations.OperationOptions;

public class ConnectOperationOptions implements OperationOptions {
	public enum ConnectOptionsStrategy {
		CONNECTION_LINK, CONNECTION_TO
	}		
	
	private ConnectOptionsStrategy strategy = ConnectOptionsStrategy.CONNECTION_TO;
	private ConfigTO configTO;
	private String connectLink;
	private File localDir;
	private String password;
	private boolean daemon;
	
	public ConnectOptionsStrategy getStrategy() {
		return strategy;
	}

	public void setStrategy(ConnectOptionsStrategy strategy) {
		this.strategy = strategy;
	}

	public String getConnectLink() {
		return connectLink;
	}

	public void setConnectLink(String connectionLink) {
		this.connectLink = connectionLink;
	}

	public File getLocalDir() {
		return localDir;
	}

	public void setLocalDir(File localDir) {
		this.localDir = localDir;
	}

	public ConfigTO getConfigTO() {
		return configTO;
	}

	public void setConfigTO(ConfigTO configTO) {
		this.configTO = configTO;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isDaemon() {
		return daemon;
	}

	public void setDaemon(boolean daemon) {
		this.daemon = daemon;
	}		
}