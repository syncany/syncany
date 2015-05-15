/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
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
package org.syncany.plugins.transfer.features;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.plugins.transfer.FileType;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.StorageTestResult;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.files.MultichunkRemoteFile;
import org.syncany.plugins.transfer.files.RemoteFile;
import org.syncany.plugins.transfer.files.RemoteFileAttributes;
import org.syncany.util.ReflectionUtil;
import org.syncany.util.StringUtil;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;

/**
 * The path aware transfer manager can be used to extend a backend storage
 * with the ability to add subfolders to the folders with many files (e.g. multichunks
 * or temporary files). This is especially critical if the backend storage has a limit
 * on how many files can be stored in a single folder (e.g. the Dropbox plugin). 
 * 
 * <p>To enable subfoldering in {@link TransferPlugin}s, the plugin's {@link TransferManager}
 * has to be annotated with the {@link PathAware} annotation, and a {@link PathAwareFeatureExtension}
 * has to be provided.
 * 
 * <p>The sub-path for a {@link RemoteFile} can then be accessed via the
 * {@link PathAwareRemoteFileAttributes} using the {@link RemoteFile#getAttributes(Class)} method.
 * 
 * @see PathAware
 * @see PathAwareFeatureExtension
 * @see PathAwareRemoteFileAttributes
 * @author Christian Roth <christian.roth@port17.de>
 */
public class PathAwareFeatureTransferManager implements FeatureTransferManager {
	private static final Logger logger = Logger.getLogger(PathAwareFeatureTransferManager.class.getSimpleName());

	private final TransferManager underlyingTransferManager;
	
	private final int subfolderDepth;
	private final int bytesPerFolder;
	private final char folderSeparator;
	private final List<Class<? extends RemoteFile>> affectedFiles;
	private final PathAwareFeatureExtension pathAwareFeatureExtension;

	public PathAwareFeatureTransferManager(TransferManager originalTransferManager, TransferManager underlyingTransferManager, Config config, PathAware pathAwareAnnotation) {
		this.underlyingTransferManager = underlyingTransferManager;

		this.subfolderDepth = pathAwareAnnotation.subfolderDepth();
		this.bytesPerFolder = pathAwareAnnotation.bytesPerFolder();
		this.folderSeparator = pathAwareAnnotation.folderSeparator();
		this.affectedFiles = ImmutableList.copyOf(pathAwareAnnotation.affected());

		this.pathAwareFeatureExtension = getPathAwareFeatureExtension(originalTransferManager, pathAwareAnnotation);
	}

	@SuppressWarnings("unchecked")
	private PathAwareFeatureExtension getPathAwareFeatureExtension(TransferManager originalTransferManager, PathAware pathAwareAnnotation) {
		Class<? extends TransferManager> originalTransferManagerClass = originalTransferManager.getClass();
		Class<PathAwareFeatureExtension> pathAwareFeatureExtensionClass = (Class<PathAwareFeatureExtension>) pathAwareAnnotation.extension();

		try {
			Constructor<?> constructor = ReflectionUtil.getMatchingConstructorForClass(pathAwareFeatureExtensionClass, originalTransferManagerClass);

			if (constructor != null) {
				return (PathAwareFeatureExtension) constructor.newInstance(originalTransferManager);
			}

			return pathAwareFeatureExtensionClass.newInstance();
		}
		catch (InvocationTargetException | InstantiationException | IllegalAccessException | NullPointerException e) {
			throw new RuntimeException("Cannot instantiate PathAwareFeatureExtension (perhaps " + pathAwareFeatureExtensionClass + " does not exist?)", e);
		}
	}

	@Override
	public void connect() throws StorageException {
		underlyingTransferManager.connect();
	}

	@Override
	public void disconnect() throws StorageException {
		underlyingTransferManager.disconnect();
	}

	@Override
	public void init(final boolean createIfRequired) throws StorageException {
		underlyingTransferManager.init(createIfRequired);
	}

	@Override
	public void download(final RemoteFile remoteFile, final File localFile) throws StorageException {
		underlyingTransferManager.download(createPathAwareRemoteFile(remoteFile), localFile);
	}

	@Override
	public void move(final RemoteFile sourceFile, final RemoteFile targetFile) throws StorageException {
		final RemoteFile pathAwareSourceFile = createPathAwareRemoteFile(sourceFile);
		final RemoteFile pathAwareTargetFile = createPathAwareRemoteFile(targetFile);

		if (!createFolder(pathAwareTargetFile)) {
			throw new StorageException("Unable to create path for " + pathAwareTargetFile);
		}

		underlyingTransferManager.move(pathAwareSourceFile, pathAwareTargetFile);
		removeFolder(pathAwareSourceFile);
	}

	@Override
	public void upload(final File localFile, final RemoteFile remoteFile) throws StorageException {
		final RemoteFile pathAwareRemoteFile = createPathAwareRemoteFile(remoteFile);

		if (!createFolder(pathAwareRemoteFile)) {
			throw new StorageException("Unable to create path for " + pathAwareRemoteFile);
		}

		underlyingTransferManager.upload(localFile, pathAwareRemoteFile);
	}

	@Override
	public boolean delete(final RemoteFile remoteFile) throws StorageException {
		RemoteFile pathAwareRemoteFile = createPathAwareRemoteFile(remoteFile);
		
		boolean fileDeleted = underlyingTransferManager.delete(pathAwareRemoteFile);
		boolean folderDeleted = removeFolder(pathAwareRemoteFile);

		return fileDeleted && folderDeleted;
	}

	@Override
	public <T extends RemoteFile> Map<String, T> list(final Class<T> remoteFileClass) throws StorageException {
		Map<String, T> filesInFolder = Maps.newHashMap();
		String remoteFilePath = getRemoteFilePath(remoteFileClass);

		list(remoteFilePath, filesInFolder, remoteFileClass);

		return filesInFolder;
	}

	private <T extends RemoteFile> void list(String remoteFilePath, Map<String, T> remoteFiles, Class<T> remoteFileClass) throws StorageException {
		logger.log(Level.INFO, "Listing folder for files matching " + remoteFileClass.getSimpleName() + ": " + remoteFilePath);
		Map<String, FileType> folderList = pathAwareFeatureExtension.listFolder(remoteFilePath);
		
		for (Map.Entry<String, FileType> folderListEntry : folderList.entrySet()) {
			String fileName = folderListEntry.getKey();
			FileType fileType = folderListEntry.getValue();
			
			if (fileType == FileType.FILE) {
				try {
					remoteFiles.put(fileName, RemoteFile.createRemoteFile(fileName, remoteFileClass));
					logger.log(Level.INFO, "- File: " + fileName);					
				}
				catch (StorageException e) {
					// We don't care and ignore non-matching files!
				}
			}
			else if (fileType == FileType.FOLDER) {
				logger.log(Level.INFO, "- Folder: " + fileName);

				String newRemoteFilePath = remoteFilePath + folderSeparator + fileName;
				list(newRemoteFilePath, remoteFiles, remoteFileClass);
			}
		}
	}

	@Override
	public String getRemoteFilePath(Class<? extends RemoteFile> remoteFileClass) {
		return underlyingTransferManager.getRemoteFilePath(remoteFileClass);
	}

	@Override
	public StorageTestResult test(boolean testCreateTarget) {
		return underlyingTransferManager.test(testCreateTarget);
	}

	@Override
	public boolean testTargetExists() throws StorageException {
		return underlyingTransferManager.testTargetExists();
	}

	@Override
	public boolean testTargetCanWrite() throws StorageException {
		return underlyingTransferManager.testTargetCanWrite();
	}

	@Override
	public boolean testTargetCanCreate() throws StorageException {
		return underlyingTransferManager.testTargetCanCreate();
	}

	@Override
	public boolean testRepoFileExists() throws StorageException {
		return underlyingTransferManager.testRepoFileExists();
	}

	private boolean isFolderizable(Class<? extends RemoteFile> remoteFileClass) {
		return affectedFiles.contains(remoteFileClass);
	}

	private RemoteFile createPathAwareRemoteFile(RemoteFile remoteFile) throws StorageException {		
		PathAwareRemoteFileAttributes pathAwareRemoteFileAttributes = new PathAwareRemoteFileAttributes();
		remoteFile.setAttributes(pathAwareRemoteFileAttributes);

		if (isFolderizable(remoteFile.getClass())) {
			// If remote file is folderizable, i.e. an 'affected file', 
			// get the sub-path for it
			
			String subPathId = getSubPathId(remoteFile);	
			String subPath = getSubPath(subPathId);
			
			pathAwareRemoteFileAttributes.setPath(subPath);
		}
		
		return remoteFile;
	}

	/**
	 * Returns the subpath identifier for this file. For {@link MultichunkRemoteFile}s, this is the
	 * hex string of the multichunk identifier. For all other files, this is the 128-bit murmur3 hash
	 * of the full filename (fast algorithm!).
	 */
	private String getSubPathId(RemoteFile remoteFile) {
		if (remoteFile.getClass() == MultichunkRemoteFile.class) {
			return StringUtil.toHex(((MultichunkRemoteFile) remoteFile).getMultiChunkId());
		}
		else {
			return StringUtil.toHex(Hashing.murmur3_128().hashString(remoteFile.getName(), Charsets.UTF_8).asBytes());
		}
	}

	private String getSubPath(String fileId) {
		StringBuilder path = new StringBuilder();

		for (int i = 0; i < subfolderDepth; i++) {
			String subPathPart = fileId.substring(i * bytesPerFolder * 2, (i + 1) * bytesPerFolder * 2); 
			
			path.append(subPathPart);
			path.append(folderSeparator);
		}
		
		return path.toString();
	}

	private String pathToString(Path path) {
		return path.toString().replaceAll(File.separator, String.valueOf(folderSeparator));
	}

	private boolean createFolder(RemoteFile remoteFile) throws StorageException {
		PathAwareRemoteFileAttributes pathAwareRemoteFileAttributes = remoteFile.getAttributes(PathAwareRemoteFileAttributes.class);		
		boolean notAPathAwareRemoteFile = pathAwareRemoteFileAttributes == null || !pathAwareRemoteFileAttributes.hasPath();
		
		if (notAPathAwareRemoteFile) {
			return true;
		} 
		else {
			String remoteFilePath = pathToString(Paths.get(underlyingTransferManager.getRemoteFilePath(remoteFile.getClass()), pathAwareRemoteFileAttributes.getPath()));
	
			logger.log(Level.INFO, "Remote file is path aware, creating folder " + remoteFilePath);
			boolean success = pathAwareFeatureExtension.createPath(remoteFilePath);
			
			return success;
		}
	}

	private boolean removeFolder(RemoteFile remoteFile) throws StorageException {
		PathAwareRemoteFileAttributes pathAwareRemoteFileAttributes = remoteFile.getAttributes(PathAwareRemoteFileAttributes.class);
		boolean notAPathAwareRemoteFile = pathAwareRemoteFileAttributes == null || !pathAwareRemoteFileAttributes.hasPath();
		
		if (notAPathAwareRemoteFile) {
			return true;
		}
		else {
			String remoteFilePath = pathToString(Paths.get(underlyingTransferManager.getRemoteFilePath(remoteFile.getClass()), pathAwareRemoteFileAttributes.getPath()));

			logger.log(Level.INFO, "Remote file is path aware, cleaning empty folders at " + remoteFilePath);
			boolean success = removeFolder(remoteFilePath);
		
			return success;
		}
	}

	private boolean removeFolder(String folder) throws StorageException {
		for(int i = 0; i < subfolderDepth; i++) {
			logger.log(Level.FINE, "Removing folder " + folder);

			if (pathAwareFeatureExtension.listFolder(folder).size() != 0) {
				return true;
			}

			if (!pathAwareFeatureExtension.removeFolder(folder)) {
				return false;
			}

			folder = folder.substring(0, folder.lastIndexOf(folderSeparator));
		}

		return true;
	}

	public static class PathAwareRemoteFileAttributes extends RemoteFileAttributes {
		private String path;

		public boolean hasPath() {
			return path != null;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}
	}

}