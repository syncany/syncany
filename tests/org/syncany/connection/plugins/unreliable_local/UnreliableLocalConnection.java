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
package org.syncany.connection.plugins.unreliable_local;

import java.util.List;
import java.util.Map;

import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.connection.plugins.local.LocalConnection;

/**
 *
 * @author Philipp C. Heckel
 */
public class UnreliableLocalConnection extends LocalConnection {
	private List<UnreliableLocalOperationStatus> operationStatusList;
	
	@Override
	public void init(Map<String, String> map) throws StorageException {
		throw new StorageException("This plugin is for test purposes only.");
	}

    @Override
    public TransferManager createTransferManager() {
        return new UnreliableLocalTransferManager(this);
    }

	public List<UnreliableLocalOperationStatus> getOperationStatusList() {
		return operationStatusList;
	}

	public void setOperationStatusList(List<UnreliableLocalOperationStatus> operationStatusList) {
		this.operationStatusList = operationStatusList;
	}

	public static enum UnreliableLocalOperationStatus {
		SUCCESS,
		FAILURE
	}
}
