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

import java.io.File;
import java.util.Map;

import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;

/**
 *
 * @author Philipp C. Heckel
 */
public class LocalConnection implements Connection {
	private File repositoryPath;

	@Override
	public void init(Map<String, String> map) throws StorageException {
		String path = map.get("path");
		
		if (path == null) {
			throw new StorageException("Config does not contain 'path' setting.");
		}
		
		setRepositoryPath(new File(path));
	}

    @Override
    public Plugin getPlugin() {
        return Plugins.get(LocalPlugin.ID);
    }

    @Override
    public TransferManager createTransferManager() {
        return new LocalTransferManager(this);
    }

    public File getRepositoryPath() {
        return repositoryPath;
    }

    public void setRepositoryPath(File repositoryPath) {
        this.repositoryPath = repositoryPath;
    }
}
