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
package org.syncany.watch.remote.files;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.syncany.config.Repository;
import org.syncany.exceptions.InvalidRepositoryException;

/**
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class DatedClientRemoteFile extends RemoteFile {
    protected Repository repository;
    protected String prefix;
    protected String machineName;
    protected Date lastUpdate;   

    public DatedClientRemoteFile(Repository repository, String prefix, String clientName, Date lastUpdate) {
        super();

        this.repository = repository;
        this.prefix = prefix;
        this.machineName = clientName;
        this.lastUpdate = lastUpdate;

        this.setName(prefix + "-" + clientName + "-" + lastUpdate.getTime());
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String clientName) {
        this.machineName = clientName;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
    
    public abstract void read(File file) throws IOException, InvalidRepositoryException, ClassNotFoundException;
    public abstract void write(File file) throws IOException, InvalidRepositoryException;
}
