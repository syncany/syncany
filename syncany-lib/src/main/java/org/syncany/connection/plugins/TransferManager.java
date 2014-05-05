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
package org.syncany.connection.plugins;

import java.io.File;
import java.util.Map;

/**
 * The transfer manager synchronously connects to the remote storage. It is
 * responsible for file upload, download and deletion. 
 * 
 * <p>All its operations are strictly <b>synchronous</b> and throw a
 * {@code StorageException} if they fail. The implementations have to make sure
 * that 
 * <ul>
 *   <li>the repository is not corrupted, e.g. duplicate files or corrupt files
 *   <li>files matching the specified file format are complete, i.e. fully uploaded
 *   <li>methods that need an established connections re-connect if necessary
 * </ul>
 * 
 * <p>A transfer manager may organize files according to their type or name as
 * it is optimal for the given storage. {@link RemoteFile}s can be classified
 * by their sub-type. For network-transfer optimization reasons, it might be
 * useful to place {@link MultiChunkRemoteFile}s and {@link DatabaseRemoteFile}s
 * in a separate sub-folder on the remote storage.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public interface TransferManager {

	/**
	 * Establish a connection with the remote storage. 
	 * 
	 * <p>This method does not validate the correctness of the repository and 
	 * it does not create any folders. The former is done by {@link #test()}, the
	 * latter is done by {@link #init(boolean)}.
	 *
	 * @throws StorageException If the connection fails due to no Internet connection,
	 *         authentication errors, etc.
	 */
	public void connect() throws StorageException;

	/**
	 * Disconnect from the remote storage.
	 * 
	 * @throws StorageException If the connection fails due to no Internet connection,
	 *         authentication errors, etc.
	 */
	public void disconnect() throws StorageException;

	/**
	 * Initialize remote storage. This method is called to set up a new repository.
	 * 
	 * @param  createIfRequired true if the method should handle repo creation
	 * 	       if it does not exists
	 * @throws StorageException If the repository is already initialized, or any other
	 *         exception occurs. 
	 */
	public void init(boolean createIfRequired) throws StorageException;

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
	 * @throws StorageException If the connection fails due to no Internet connection,
	 *         authentication errors, etc.
	 */
	public void download(RemoteFile remoteFile, File localFile) throws StorageException;

	/**
	 * Update an existing local file to the online storage.
	 *
	 * <p>The file is either uploaded completely or nothing at all. In the latter
	 * case, a {@code StorageException} is thrown.
	 * 
	 * <p>Implementations must make sure that if a file matches the specified name
	 * schema, it must be complete and consistent.
	 * 
	 * <p>Implementations must NOT upload a file if it already exists and has
	 * the same file size as the local file.
	 *
	 * @param localFile Existing local file that is going to be uploaded.
	 * @param remoteFile Not existing destination file on the remote storage.
	 *        The only required property of the remote file is the name.
	 * @throws StorageException If the connection fails due to no Internet connection,
	 *         authentication errors, etc.
	 */
	public void upload(File localFile, RemoteFile remoteFile) throws StorageException;

	/**
	 * Deletes an existing file from the remote storage permanently.
	 *
	 * <p>In case the remote file does not exist, it returns immediately without
	 * any notice. If the file cannot be deleted or the connection breaks,
	 * a {@code StorageException} is thrown.
	 *
	 * @param remoteFile Existing remote file to be deleted.
	 *        The only required property of the remote file is the name.
	 * @throws StorageException If the connection fails due to no Internet connection,
	 *         authentication errors, etc
	 */
	public boolean delete(RemoteFile remoteFile) throws StorageException;

	/**
	 * Retrieves a list of all files in the remote repository, filtered by
	 * the type of the desired file, i.e. by a sub-class of {@link RemoteFile}.
	 * 
	 * @param remoteFileClass Filter class: <tt>RemoteFile</tt> or a sub-type thereof  
	 * @return Returns a list of remote files. In the map, the key is the file name,
	 *         the value the entire {@link RemoteFile} object.
	 * @throws StorageException If the connection fails due to no Internet connection,
	 *         authentication errors, etc
	 */
	public <T extends RemoteFile> Map<String, T> list(Class<T> remoteFileClass) throws StorageException;

	/**
	 * Tests whether the repository parameters are valid. In particular, the method tests
	 * whether a target (folder, bucket, etc.) exists or, if not, whether it can be created.
	 * It furthermore tests whether a repository at the target already exists by checking if the
	 * {@link RepoRemoteFile} exists.
	 * 
	 * <p>The relevant result is determined by the following methods:
	 * 
	 * <ul>
	 *  <li>{@link #testTargetExists()}: Tests whether the target exists.</li>
	 *  <li>{@link #testTargetCanWrite()}: Tests whether the target is writable.</li>
	 *  <li>{@link #testTargetCanCreate()}: Tests whether the target can be created if it does not 
	 *      exist already. This is only called if <tt>testCreateTarget</tt> is set.</li>
	 *  <li>{@link #testRepoFileExists()}: Tests whether the repo file exists.</li>
	 * </ul>
	 * 
	 * @return Returns the result of testing the repository. 
	 * @param testCreateTarget If <tt>true</tt>, the test will test if the target can be created in case
	 *        it does not exist. If <tt>false</tt>, this test will be skipped.
	 * @see {@link StorageTestResult}
	 */
	public StorageTestResult test(boolean testCreateTarget);

	/**
	 * Tests whether the target path/folder <b>exists</b>. This might be done by listing the parent path/folder
	 * or by retrieving metadata about the target. The method returns <tt>true</tt> if the target exists,
	 * <tt>false</tt> otherwise.
	 * 
	 * <p>This method is called by the {@link #test(boolean)} method (only during repository initialization 
	 * or initial connection).
	 * 
	 * @return Returns <tt>true</tt> if the target exists, <tt>false</tt> otherwise
	 * @throws StorageException If the test cannot be performed, e.g. due to a connection failure 
	 */
	public boolean testTargetExists() throws StorageException;
	
	/**
	 * Tests whether the target path/folder is <b>writable</b> by the application. This method may either
	 * check the write permissions of the target or actually write a test file to check write access. If the
	 * target does not exist, <tt>false</tt> is returned. If the target exists and is writable, <tt>true</tt>
	 * is returned.
	 * 
	 * <p>This method is called by the {@link #test(boolean)} method (only during repository initialization 
	 * or initial connection).
	 * 
	 * @return Returns <tt>true</tt> if the target can be written to, <tt>false</tt> otherwise
	 * @throws StorageException If the test cannot be performed, e.g. due to a connection failure 
	 */
	public boolean testTargetCanWrite() throws StorageException;

	/**
	 * Tests whether the target path/folder <b>can be created</b> (if it <b>does not exist already</b>). This method 
	 * may either check the permissions of the parent path/folder or actually create and delete the target to 
	 * determine create permissions. 
	 * 
	 * <p>If the target already exists, the method returns <tt>true</tt>. If it does not, but it can be created 
	 * (according to tests of this method), it also returns <tt>true</tt>. In all other cases, <tt>false</tt> is returned.
	 * 
	 * <p>This method is called by the {@link #test(boolean)} method, <b>but only if</b> the <tt>testCreateTarget</tt> flag
	 * is set to <tt>true</tt>!
	 * 
	 * @return Returns <tt>true</tt> if the target can be created or already exists, <tt>false</tt> otherwise 
	 * @throws StorageException If the test cannot be performed, e.g. due to a connection failure 
	 */
	public boolean testTargetCanCreate() throws StorageException; 

	/**
	 * Tests whether the <b>repository file exists</b> (see {@link RepoRemoteFile}). This method is called by the {@link #test()} method 
	 * (only during repository initialization (or initial connection).
	 * 
	 * <p>This method is called by the {@link #test(boolean)} method (only during repository initialization 
	 * or initial connection).
	 * 
	 * @return Returns <tt>true</tt> if the repository is valid, <tt>false</tt> otherwise 
	 * @throws StorageException If the test cannot be performed, e.g. due to a connection failure 
	 */
	public boolean testRepoFileExists() throws StorageException;
}
