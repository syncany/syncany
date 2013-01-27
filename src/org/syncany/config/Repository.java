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
package org.syncany.config;

import org.syncany.Constants;
import org.syncany.chunk.chunking.Chunker;
import org.syncany.chunk.chunking.FixedOffsetChunker;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.exceptions.CacheException;
import org.syncany.exceptions.InvalidRepositoryException;
import org.syncany.exceptions.NoRepositoryFoundException;
import org.syncany.exceptions.RepositoryFoundException;
import org.syncany.exceptions.StorageConnectException;
import org.syncany.exceptions.StorageException;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.syncany.watch.remote.files.StructuredFileList;

/**
 *
 * @author Philipp C. Heckel
 */
public final class Repository{
	private Connection connection;
    private Encryption encryption;
    private Chunker chunker;
    private int chunkSize;
    
    private Date lastUpdate;
    private boolean changed;    
    private boolean connected;

    // New
    public Repository() {
        // Fressen
        connection = null; // Loaded or set dynamically!
        encryption = new Encryption();
        chunker = new FixedOffsetChunker(16*1024);
        lastUpdate = null;
        changed = false;
        connected = false;
    }
 
    /**
     * Returns the chunk size in kilobytes
     * @return
     */
    public int getChunkSize() {
        return chunkSize;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Encryption getEncryption() {
        return encryption;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public void setEncryption(Encryption encryption) {
        this.encryption = encryption;
    }

    public Chunker getChunker() {
        return chunker;
    }

    public void setChunker(Chunker chunker) {
        this.chunker = chunker;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

//    public void setLastUpdate(Date lastUpdate) {
//        this.lastUpdate = lastUpdate;
//    }          
    
}
