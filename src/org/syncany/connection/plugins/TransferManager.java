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

import java.io.File;
import java.util.Map;

import org.syncany.connection.RemoteFile;
import org.syncany.exceptions.LocalFileNotFoundException;
import org.syncany.exceptions.RemoteFileNotFoundException;
import org.syncany.exceptions.StorageConnectException;
import org.syncany.exceptions.StorageException;

/**
 * The transfer manager synchronously connects to the remote storage. It is
 * responsible for file upload, download and deletion. 
 * 
 * <p>All its operations are strictly <b>synchronous</b> and throw a
 * {@code StorageException} if they fail. The implementations have to make sure
 * that 
 * <ul>
 * <li>the repository is not corrupted, e.g. duplicate files or corrupt files
 * <li>files matching the specified file format are complete, i.e. fully uploaded
 * <li>methods that need an established connections re-connect if necessary
 * </ul>
 *
 * @author Philipp C. Heckel
 */
public interface TransferManager {
    /**
     * Establish a connection with the remote storage and initialize the repository
     * if necessary (e.g. create folders).
     *
     * @throws StorageException If the connection fails due to no internet connection,
     *         authentication errors, etc.
     */
    void connect() throws StorageConnectException;

    /**
     * Disconnect from the remote storage.
     * 
     * @throws StorageException If the connection fails due to no internet connection,
     *         authentication errors, etc.
     */
    void disconnect() throws StorageException;

    /**
     * Download an existing remote file to the local disk.
     *
     * <p>The file is either downloaded completely or nothing at all. In the latter
     * case, a {@code StorageException} is thrown.
     *
     * <p>Implementations must make sure that if a file matches the specified name
     * schema, it must be complete and consistent.
     *
     * @param remoteFile Existing source file on the remote storage.
     *        The only required property of the remote file is the name.
     * @param localFile Not existing local file to which the remote file is
     *        going to be downloaded.
     * @throws StorageException If the connection fails due to no internet connection,
     *         authentication errors, etc.
     */
    void download(RemoteFile remoteFile, File localFile) throws RemoteFileNotFoundException, StorageException;

    /**
     * Update an existing local file to the online storage.
     *
     * <p>The file is either uploaded completely or nothing at all. In the latter
     * case, a {@code StorageException} is thrown.
     * 
     * <p>Implementations must make sure that if a file matches the specified name
     * schema, it must be complete and consistent.
     *
     * @param localFile Existing local file that is going to be uploaded.
     * @param remoteFile Not existing destination file on the remote storage.
     *        The only required property of the remote file is the name.
     * @throws StorageException If the connection fails due to no internet connection,
     *         authentication errors, etc.
     */
    void upload(File localFile, RemoteFile remoteFile) throws LocalFileNotFoundException, StorageException;

    /**
     * Deletes an existing file from the remote storage permanently.
     *
     * <p>In case the remote file does not exist, it returns immediately without
     * any notice. If the file cannot be deleted or the connection breaks,
     * a {@code StorageException} is thrown.
     *
     * @param remoteFile Existing remote file to be deleted.
     *        The only required property of the remote file is the name.
     * @throws StorageException If the connection fails due to no internet connection,
     *         authentication errors, etc
     */
    void delete(RemoteFile remoteFile) throws RemoteFileNotFoundException, StorageException;

    /**
     * Retrieves a list of all files in the remote repository.
     *
     * @return Returns a list of remote files. In the map, the key is the file name,
     *         the value the entire {@link RemoteFile} object.
     * @throws StorageException If the connection fails due to no internet connection,
     *         authentication errors, etc
     */
    Map<String, RemoteFile> list() throws StorageException;

    /**
     * Retrieves a list of selected files in the remote repository, filtered by
     * a given prefix. 
     *
     * @param namePrefix Prefix of the files to be return by the method. Can be used
     *        for updates, chunks, etc.
     * @return Returns a list of remote files. In the map, the key is the file name,
     *         the value the entire {@link RemoteFile} object.
     * @throws StorageException If the connection fails due to no internet connection,
     *         authentication errors, etc
     */
    Map<String, RemoteFile> list(String namePrefix) throws StorageException;

    /**
     * Deletes unused old files from the remote storage, especially temporary files.
     *
     * @throws StorageException If the connection fails due to no internet connection,
     *         authentication errors, etc
     */
    void clean() throws StorageException;
}
