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
package org.syncany.connection.plugins;

import java.util.Map;
import java.util.TreeMap;

/**
 * A connection represents the configuration settings of a storage/connection
 * plugin. It is created through the concrete implementation of a {@link Plugin}.
 *  
 * <p>A connection must be initialized by calling the {@link #init(Map) init()} method,
 * using plugin specific configuration parameters. 
 * 
 * <p>Once initialized, a {@link TransferManager} can be created through the {@link #createTransferManager()}
 * method. The transfer manager can then be used to upload/download files.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class Connection {
    public abstract TransferManager createTransferManager();
    public void validate() throws StorageException {
    	for (String name : getSettings().keySet()) {
    		PluginSetting setting = getSettings().get(name);
    		if (setting.isMandatory()) {
    			if (!setting.validate()) {
    				throw new StorageException("Mandatory setting " + name + " is not set.");
    			}
    		}
    		else {
    			//Set default value if it exists and none is given
    			if (!setting.validate()) {
    				if (setting.getDefaultValue() != null) {
    					setting.setValue(setting.getDefaultValue());
    				}
    			}
    		}
    	}
    }
    
    public abstract Map<String,PluginSetting> getSettings();
    public void setSettings(Map<String, String> map) throws StorageException {
    	for (String name : map.keySet()) {
    		if (getSettings().get(name) == null) {
    			throw new StorageException("No such setting: " + name);
    		}
    		try {
    			getSettings().get(name).setValue(map.get(name));
    		}
    		catch (Exception e) {
    			throw new StorageException(e);
    		}
    	}
    }
    
    public Map<String, String> getSettingsStrings() {
    	Map<String, String> map = new TreeMap<String, String>();
    	for (String name : getSettings().keySet()) {
    		map.put(name, getSettings().get(name).getValue());
    	}
    	return map;
    }
}

