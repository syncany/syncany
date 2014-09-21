/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.plugins.transfer;

import java.util.Map;

import org.syncany.plugins.Plugin;
import org.syncany.plugins.PluginOptionSpecs;
import org.syncany.plugins.UserInteractionListener;

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
public abstract class TransferSettings {
	protected UserInteractionListener userInteractionListener;

	public UserInteractionListener getUserInteractionListener() {
		return userInteractionListener;
	}

	public void setUserInteractionListener(UserInteractionListener userInteractionListener) {
		this.userInteractionListener = userInteractionListener;
	}

	public abstract PluginOptionSpecs getOptionSpecs();    
    public abstract void init(Map<String, String> optionValues) throws StorageException;    
}

