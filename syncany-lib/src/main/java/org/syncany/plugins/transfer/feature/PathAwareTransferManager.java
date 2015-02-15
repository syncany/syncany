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
package org.syncany.plugins.transfer.feature;

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
import org.syncany.plugins.transfer.files.RemoteFile;
import org.syncany.plugins.transfer.files.RemoteFileAttributes;
import org.syncany.util.ReflectionUtil;
import org.syncany.util.StringUtil;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public class PathAwareTransferManager implements TransferManager {
	private static final Logger logger = Logger.getLogger(PathAwareTransferManager.class.getSimpleName());

	private final TransferManager underlyingTransferManager;
	private final Config config;
	private final int subfolderDepth;
	private final int bytesPerFolder;
	private final char folderSeparator;
	private final List<Class<? extends RemoteFile>> affectedFiles;
	private final PathAwareFeatureExtension pathAwareFeatureExtension;

	public PathAwareTransferManager(TransferManager underlyingTransferManager, Config config, PathAware pathAwareAnnotation, TransferManager originTransferManager) {
		this.underlyingTransferManager = underlyingTransferManager;
		this.config = config;

		subfolderDepth = pathAwareAnnotation.subfolderDepth();
		bytesPerFolder = pathAwareAnnotation.bytesPerFolder();
		folderSeparator = pathAwareAnnotation.folderSeparator();
		affectedFiles = ImmutableList.copyOf(pathAwareAnnotation.affected());

		pathAwareFeatureExtension = getPathAwareFeatureExtension(originTransferManager, pathAwareAnnotation);
	}

	@SuppressWarnings("unchecked")
	private PathAwareFeatureExtension getPathAwareFeatureExtension(TransferManager originTransferManager, PathAware pathAwareAnnotation) {
		Class<? extends TransferManager> originTransferManagerClass = originTransferManager.getClass();
		Class<PathAwareFeatureExtension> pathAwareFeatureExtensionClass = (Class<PathAwareFeatureExtension>) pathAwareAnnotation.extension();

		try {
			Constructor<?> constructor = ReflectionUtil.getMatchingConstructorForClass(pathAwareFeatureExtensionClass, originTransferManagerClass);

			if (constructor != null) {
				return (PathAwareFeatureExtension) constructor.newInstance(originTransferManagerClass.cast(originTransferManager));
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

		underlyingTransferManager.move(createPathAwareRemoteFile(sourceFile), pathAwareTargetFile);
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
		final RemoteFile pathAwareRemoteFile = createPathAwareRemoteFile(remoteFile);
		boolean delete = underlyingTransferManager.delete(pathAwareRemoteFile);
		boolean folder = removeFolder(pathAwareRemoteFile);

		return delete && folder;
	}

	@Override
	public <T extends RemoteFile> Map<String, T> list(final Class<T> remoteFileClass) throws StorageException {
		Map<String, T> filesInFolder = Maps.newHashMap();
		String remoteFilePath = getRemoteFilePath(remoteFileClass);

		list(remoteFilePath, filesInFolder, remoteFileClass);

		return filesInFolder;
	}

	private <T extends RemoteFile> void list(String remoteFilePath, Map<String, T> remoteFiles, Class<T> remoteFileClass) throws StorageException {
		logger.log(Level.SEVERE, "Listing folder: " + remoteFilePath);

		for (Map.Entry<FileType, String> item : pathAwareFeatureExtension.listFolder(remoteFilePath).entrySet()) {
			String itemName = item.getValue();

			switch (item.getKey()) {
				case FILE:
					remoteFiles.put(itemName, RemoteFile.createRemoteFile(itemName, remoteFileClass));
					break;

				case FOLDER:
					String newRemoteFilePath = remoteFilePath + folderSeparator + itemName;
					list(newRemoteFilePath, remoteFiles, remoteFileClass);
					break;

				default:
					break;
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
		// a plugin dev can access a pathaware remotefile's path using
		//    remoteFile.getAttributes(PathAwareRemoteFileAttributes.class).getPath();
		// which follows the java.nio files style.
		PathAwareRemoteFileAttributes pathAwareRemoteFileAttributes = new PathAwareRemoteFileAttributes();
		remoteFile.addAttributes(pathAwareRemoteFileAttributes);

		if (!isFolderizable(remoteFile.getClass())) {
			return remoteFile;
		}

		// we need to use the hash value of a file's name because some files aren't folderizable by default
		String fileId = StringUtil.toHex(Hashing.murmur3_128().hashString(remoteFile.getName(), Charsets.UTF_8).asBytes());
		StringBuilder path = new StringBuilder();

		for (int i = 0; i < subfolderDepth; i++) {
			path.append(fileId.substring(i * bytesPerFolder, (i + 1) * bytesPerFolder));
			path.append(folderSeparator);
		}

		pathAwareRemoteFileAttributes.setPath(path.toString());

		return remoteFile;
	}

	private String pathToString(Path path) {
		return path.toString().replaceAll(File.pathSeparator, String.valueOf(folderSeparator));
	}

	private boolean createFolder(RemoteFile remoteFile) throws StorageException {
		boolean success = true;
		PathAwareRemoteFileAttributes pathAwareRemoteFileAttributes;

		try {
			pathAwareRemoteFileAttributes = remoteFile.getAttributes(PathAwareRemoteFileAttributes.class);
		}
		catch (NoSuchFieldException e) {
			return true;
		}

		if (!pathAwareRemoteFileAttributes.hasPath()) {
			return true;
		}

		String remoteFilePath = pathToString(Paths.get(underlyingTransferManager.getRemoteFilePath(remoteFile.getClass()), pathAwareRemoteFileAttributes.getPath()));

		if (remoteFilePath != null) {
			logger.log(Level.INFO, "Remote file is path aware, creating folder " + remoteFilePath);
			success = pathAwareFeatureExtension.createPath(remoteFilePath);
		}

		return success;
	}

	private boolean removeFolder(RemoteFile remoteFile) throws StorageException {
		boolean success = true;
		PathAwareRemoteFileAttributes pathAwareRemoteFileAttributes;

		try {
			pathAwareRemoteFileAttributes = remoteFile.getAttributes(PathAwareRemoteFileAttributes.class);
		}
		catch (NoSuchFieldException e) {
			return true;
		}

		if (!pathAwareRemoteFileAttributes.hasPath()) {
			return true;
		}

		String remoteFilePath = pathToString(Paths.get(underlyingTransferManager.getRemoteFilePath(remoteFile.getClass()), pathAwareRemoteFileAttributes.getPath()));

		if (remoteFilePath != null) {
			logger.log(Level.INFO, "Remote file is path aware, cleaning empty folders at " + remoteFilePath);
			success = removeFolder(remoteFilePath);
		}

		return success;
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