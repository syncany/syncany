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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.syncany.plugins.transfer.features.PathAwareFeatureTransferManager.PathAwareRemoteFileAttributes;
import org.syncany.plugins.transfer.files.MultichunkRemoteFile;
import org.syncany.plugins.transfer.files.RemoteFile;
import org.syncany.plugins.transfer.files.TempRemoteFile;

/**
 * Feature annotation to mark a transfer manager of transfer plugins
 * that need to be sub-foldered. This feature adds sub folders to some {@link RemoteFile}
 * storage paths in the backend for the purpose of limiting the per-folder file 
 * count. 
 * 
 * <p>This is especially critical if the backend storage has a limit
 * on how many files can be stored in a single folder, e.g. the Dropbox plugin can only
 * store 25,000 files in one folder.  
 * 
 * <p>This annotation is only recognized if used on a {@link TransferManager}. If 
 * applied, it wraps the original transfer manager in a {@link PathAwareFeatureTransferManager},
 * which defines details for the subfoldering, such as the depths of the subfolders, the path
 * separator or other relevant settings. 
 * 
 * <p>The only mandatory configuration for this feature is specifying a
 * {@link PathAwareFeatureExtension} to manage the required subfolders (creating, 
 * deleting, listing). It must be implemented by the transfer plugin.
 * 
 * <p>Example: If the {@link #subfolderDepth()} is 2, the {@link #bytesPerFolder()} is 1,
 * and the {@link #folderSeparator()} is '/', a multichunk file might be located at
 * 'multichunks/e3/5f/multichunk-e35f0d48060...' instead of 
 * 'multichunks/multichunk-e35f0d4806...'.
 * 
 * @see PathAwareFeatureTransferManager
 * @see PathAwareFeatureExtension
 * @see PathAwareRemoteFileAttributes 
 * @author Christian Roth <christian.roth@port17.de>
 */
@Feature(required = false)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PathAware {
	/**
	 * The path aware extension must be specified to manage the required
	 * subfolders (creating, deleting, listing). 
	 */
	Class<? extends PathAwareFeatureExtension> extension();

	/**
	 * Defines the depth of the subfolders to be created. The higher the depth, the 
	 * bigger the overhead required for managing the subfolders. 
	 */
	int subfolderDepth() default 2;

	/**
	 * Defines the bytes of the subfolder identifier used for the 
	 * subfolders. If the bytes per folder is 2 (and the subfolder depth is 2), 
	 * a multichunk might be located at 'multichunks/e35f/0d48/multichunk-e35f0d4...'.
	 */
	int bytesPerFolder() default 1;

	/** 
	 * Defines the separator used to complete the folder paths.
	 */
	char folderSeparator() default '/';

	/**
	 * Defines the remote file classes to be affected by the subfoldering. Do not
	 * change the default unless you know what you are doing.
	 */
	Class<? extends RemoteFile>[] affected() default { MultichunkRemoteFile.class, TempRemoteFile.class };
}
