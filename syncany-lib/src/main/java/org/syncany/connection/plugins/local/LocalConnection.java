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
package org.syncany.connection.plugins.local;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.PluginSetting;
import org.syncany.connection.plugins.PluginSetting.ValueType;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;

/**
 * The local connection represents the settings required to create to a
 * backend based on a local (or mounted network) folder. It can be used to
 * initialize/create a {@link LocalTransferManager} and is part of
 * the {@link LocalPlugin}.  
 *  
 * @author Philipp C. Heckel
 */
public class LocalConnection extends Connection {
	private File repositoryPath;

	@Override
	public void init(Map<String, String> map) throws StorageException {
		List<PluginSetting> settings = getFilledSettings(map);
		validateSettings(settings);
		for (PluginSetting setting : settings) {
			if (setting.getName().equals("path")) {
				setRepositoryPath(new File(setting.getValue()));
			}
		}		
	}
	
	@Override
	public void validateSettings(List<PluginSetting> settings) throws StorageException {
		for (PluginSetting setting : settings) {
			if (setting.getName().equals("path")) {
				if (!setting.validate()) {
					throw new StorageException("Config does not contain 'path' setting.");
				}
			}
		}
		
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

	@Override
	public List<PluginSetting> getSettings() {
		return Arrays.asList(new PluginSetting[] {
				new PluginSetting("path", ValueType.STRING, true, false)
		});
	}




	
}
