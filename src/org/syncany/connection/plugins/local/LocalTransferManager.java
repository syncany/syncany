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

import org.syncany.connection.plugins.AbstractTransferManager;
import org.syncany.exceptions.CacheException;
import org.syncany.exceptions.LocalFileNotFoundException;
import org.syncany.exceptions.RemoteFileNotFoundException;
import org.syncany.exceptions.StorageConnectException;
import org.syncany.exceptions.StorageException;
import org.syncany.watch.remote.files.RemoteFile;
import org.syncany.util.FileUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Philipp C. Heckel
 */
public class LocalTransferManager extends AbstractTransferManager {
    private File folder; 

    public LocalTransferManager(LocalConnection connection) {
        super(connection);
        folder = connection.getFolder();
    }

    @Override
    public LocalConnection getConnection() {
        return (LocalConnection) super.getConnection();
    }

    @Override
    public void connect() throws StorageConnectException {
        if (folder == null || !folder.exists() || !folder.canRead() || !folder.canWrite() || !folder.isDirectory()) {
            throw new StorageConnectException("Repository folder '"+folder+"' does not exist or is not writable.");
        }
    }

    @Override
    public void disconnect() throws StorageException {
        // Nothing.
    }
    
    
    // create a tempfile
    public File createTempFile(String name) throws CacheException {
        try {
            return File.createTempFile(
                 String.format("temp-%s-", name),
                 ".tmp", folder);
        }
        catch (IOException e) {
            throw new CacheException("Unable to create temporary file in cache.", e);
        }
    }

    @Override
    public void download(RemoteFile remoteFile, File localFile) throws RemoteFileNotFoundException, StorageException {
        connect();

        File repoFile = getRepoFile(remoteFile);

        if (!repoFile.exists()) {
            throw new RemoteFileNotFoundException("No such file in local repository: "+repoFile);
        }  

        try {
            File tempLocalFile = createTempFile("local-tm-download");
            tempLocalFile.deleteOnExit();

            FileUtil.copy(repoFile, tempLocalFile, getConnection().getThrottleKbps());

            // SNM 6/01/11 windows doesn't support rename *onto* another file
            if (localFile.exists()){
                localFile.delete();
            }
            
            if (!tempLocalFile.renameTo(localFile)) {
                throw new StorageException("Unable to move temp local file "+tempLocalFile+" to "+localFile);
            }
            
            tempLocalFile.delete();
        }
        catch (IOException ex) {
            throw new StorageException("Unable to copy file "+repoFile+" from local repository to "+localFile, ex);
        }
    }

    @Override
    public void upload(File localFile, RemoteFile remoteFile) throws LocalFileNotFoundException, StorageException {
        connect();

        File repoFile = getRepoFile(remoteFile);
        File tempRepoFile = new File(FileUtil.getAbsoluteParentDirectory(repoFile)+File.separator+".temp-"+repoFile.getName());

        // Do not overwrite files!
        if (repoFile.exists()) {
            return;
        }

        // No such local file
        if (!localFile.exists()) {
            throw new LocalFileNotFoundException("No such file on local disk: "+localFile);
        }

        try {
            FileUtil.copy(localFile, tempRepoFile, getConnection().getThrottleKbps());
            tempRepoFile.renameTo(repoFile);
        }
        catch (IOException ex) {
            throw new StorageException("Unable to copy file "+localFile+" to local repository "+repoFile, ex);
        }
    }

    @Override
    public void delete(RemoteFile remoteFile) throws RemoteFileNotFoundException, StorageException {
        connect();

        File repoFile = getRepoFile(remoteFile);

        if (!repoFile.exists()) {
            return;
        }

        repoFile.delete();
    }

    @Override
    public Map<String, RemoteFile> list() throws StorageException {
        return list(null);
    }

    @Override
    public Map<String, RemoteFile> list(final String namePrefix) throws StorageException {
        connect();

        File[] files;

        if (namePrefix == null) {
            files = folder.listFiles();
        }
        else {
            files = folder.listFiles(new FilenameFilter() {
            @Override public boolean accept(File dir, String name) {
                return name.startsWith(namePrefix); }
            });
        }

        if (files == null) {
            throw new StorageException("Unable to read local respository "+folder);
        }

        Map<String, RemoteFile> remoteFiles = new HashMap<String, RemoteFile>();

        for (File file : files) {
            remoteFiles.put(file.getName(), new RemoteFile(file.getName(), file.length(), file));
        }

        return remoteFiles;
    }

    private File getRepoFile(RemoteFile remoteFile) {
        return new File(folder+File.separator+remoteFile.getName());
    }
}
