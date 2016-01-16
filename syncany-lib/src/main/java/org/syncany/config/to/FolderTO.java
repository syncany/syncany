/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
import org.syncany.operations.watch.WatchOperation;
import org.syncany.operations.watch.WatchOperationOptions;

/**
 * This class is the access object to configure a folder
 * managed by the daemon. It defines whether a folder is enabled/disabled,
 * and with which {@link WatchOperationOptions} to start the {@link WatchOperation}
 * in the daemon. This class is part of the daemon configuration in {@link DaemonConfigTO}. 
 * 
 * <p>It uses the Simple framework for XML serialization, and its corresponding
 * annotation-based configuration.
 *
 * @see <a href="http://simple.sourceforge.net/">Simple framework</a>
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
@Root(strict = false)
public class FolderTO {
	@Element(name="path")
	private String path;

	@Element(name="enabled", required=false) 
	private boolean enabled = true;
	
	@Element(name="watch", required = false)
	private WatchOperationOptions watchOptions;

	public FolderTO() {
		// Nothing!
	}
	
	public FolderTO(String path) {
		this.path = path;
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}	

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public WatchOperationOptions getWatchOptions() {
		return watchOptions;
	}

	public void setWatchOptions(WatchOperationOptions watchOptions) {
		this.watchOptions = watchOptions;
	}
}