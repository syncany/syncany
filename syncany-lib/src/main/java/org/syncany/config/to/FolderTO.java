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
package org.syncany.config.to;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.syncany.operations.watch.WatchOperationOptions;

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