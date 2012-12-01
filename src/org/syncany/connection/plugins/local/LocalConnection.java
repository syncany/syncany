/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.connection.plugins.local;

import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.PluginInfo;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.util.FileUtil;
import java.io.File;
import java.util.Map;

/**
 *
 * @author Philipp C. Heckel
 */
public class LocalConnection implements Connection {
    /**
	 * 
	 */
	private File folder;
    private int throttleKbps;    

    public LocalConnection() { }
    
	@Override
	public void init(Map<String, String> map) {
		String path = map.get("path");
		File folder = new File(path);
		
		// create local repository directory if not present
		FileUtil.mkdirsVia(folder);
		
		setFolder(folder);
	}

    @Override
    public PluginInfo getPluginInfo() {
        return Plugins.get(LocalPluginInfo.ID);
    }

    @Override
    public TransferManager createTransferManager() {
        return new LocalTransferManager(this);
    }

    public File getFolder() {
        return folder;
    }

    public int getThrottleKbps() {
        return throttleKbps;
    }

    public void setFolder(File folder) {
        this.folder = folder;
    }

    public void setThrottleKbps(int throttleKbps) {
        this.throttleKbps = throttleKbps;
    }              
}
