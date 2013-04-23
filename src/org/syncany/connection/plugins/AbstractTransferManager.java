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
package org.syncany.connection.plugins;

import java.util.HashMap;
import java.util.Map;

import org.syncany.connection.RemoteFile;
import org.syncany.exceptions.StorageException;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class AbstractTransferManager implements TransferManager {
    private Connection connection;

    public AbstractTransferManager(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }
    
    @Override
    public Map<String, RemoteFile> list(String namePrefix) throws StorageException {
        Map<String, RemoteFile> result = new HashMap<String, RemoteFile>();

        for (Map.Entry<String, RemoteFile> entry : list().entrySet()) {
            if (!entry.getKey().startsWith(namePrefix)) {
                continue;
            }

            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }    
    
    @Override
    public void clean() throws StorageException {
        throw new UnsupportedOperationException("clean() not implemented yet.");
    }    
}
