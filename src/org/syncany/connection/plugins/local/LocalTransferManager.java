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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.syncany.connection.plugins.AbstractTransferManager;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;

/**
 *
 * @author Philipp C. Heckel
 */
public class LocalTransferManager extends AbstractTransferManager {
    private File repoPath; 
    private File dataPath;

    public LocalTransferManager(LocalConnection connection) {
        super(connection);
        
        this.repoPath = connection.getRepositoryPath();
        this.dataPath = new File(connection.getRepositoryPath().getAbsolutePath()+File.separator+"data");
    }

    @Override
    public void connect() throws StorageException {
        if (repoPath == null || !repoPath.exists() || !repoPath.canRead() || !repoPath.canWrite() || !repoPath.isDirectory()) {
            throw new StorageException("Repository folder '"+repoPath+"' does not exist or is not writable.");
        }
    }

    @Override
    public void disconnect() throws StorageException {
        // Nothing.
    }

    @Override
    public void init() throws StorageException {
    	connect();
    	
		if (!dataPath.mkdir()) {
			throw new StorageException("Cannot create data directory: "+dataPath);	
		}
    }
    
    @Override
    public void download(RemoteFile remoteFile, File localFile) throws StorageException {
        connect();

        File repoFile = getRemoteFile(remoteFile);

        if (!repoFile.exists()) {
            throw new StorageException("No such file in local repository: "+repoFile);
        }  

        try {
            File tempLocalFile = createTempFile("local-tm-download");
            tempLocalFile.deleteOnExit();

            copyLocalFile(repoFile, tempLocalFile);

            localFile.delete();            
            FileUtils.moveFile(tempLocalFile, localFile);            
            tempLocalFile.delete();
        }
        catch (IOException ex) {
            throw new StorageException("Unable to copy file "+repoFile+" from local repository to "+localFile, ex);
        }
    }

    @Override
    public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
        connect();

        File repoFile = getRemoteFile(remoteFile);
        File tempRepoFile = new File(getAbsoluteParentDirectory(repoFile)+File.separator+".temp-"+repoFile.getName());

        // Do not overwrite files with same size!
        if (repoFile.exists() && repoFile.length() == localFile.length()) {
            return;
        }

        // No such local file
        if (!localFile.exists()) {
            throw new StorageException("No such file on local disk: "+localFile);
        }

        try {
            copyLocalFile(localFile, tempRepoFile);
            FileUtils.moveFile(tempRepoFile, repoFile);            
        }
        catch (IOException ex) {
            throw new StorageException("Unable to copy file "+localFile+" to local repository "+repoFile, ex);
        }
    }

    @Override
    public boolean delete(RemoteFile remoteFile) throws StorageException {
        connect();

        File repoFile = getRemoteFile(remoteFile);

        if (!repoFile.exists()) {
            return false;
        }

        return repoFile.delete();
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
            files = repoPath.listFiles();
        }
        else {
            files = repoPath.listFiles(new FilenameFilter() {
            @Override public boolean accept(File dir, String name) {
                return name.startsWith(namePrefix); }
            });
        }

        if (files == null) {
            throw new StorageException("Unable to read local respository "+repoPath);
        }

        Map<String, RemoteFile> remoteFiles = new HashMap<String, RemoteFile>();

        for (File file : files) {
            remoteFiles.put(file.getName(), new RemoteFile(file.getName(), file));
        }

        return remoteFiles;
    }

    private File getRemoteFile(RemoteFile remoteFile) {
    	if (remoteFile instanceof MultiChunkRemoteFile) {
    		return new File(dataPath+File.separator+remoteFile.getName());
    	}
    	else {
    		return new File(repoPath+File.separator+remoteFile.getName());
    	}
    }
    
    public void copyLocalFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        
        byte[] buf = new byte[4096];

        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }

        in.close();
        out.close();
    }
    
    public String getAbsoluteParentDirectory(File file) {
        return file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(File.separator));
    }
    
    public File createTempFile(String name) throws IOException {
        return File.createTempFile(String.format("temp-%s-", name), ".tmp", repoPath);
    }    
}
