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
package org.syncany.plugins.transfer;

import java.util.List;

import org.syncany.plugins.transfer.files.MultichunkRemoteFile;
import org.syncany.plugins.transfer.files.RemoteFile;
import org.syncany.plugins.transfer.files.TempRemoteFile;
import com.google.common.collect.ImmutableList;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public interface FolderizableTransferManager extends TransferManager {

	public static final int BYTES_PER_FOLDER = 2;
	public static final int SUBFOLDER_DEPTH = 2;
	public static final List<Class<? extends RemoteFile>> FOLDERIZABLE_FILES = ImmutableList.<Class<? extends RemoteFile>>builder().add(MultichunkRemoteFile.class).add(TempRemoteFile.class).build();

	public int getBytesPerFolder();

	public int getSubfolderDepth();

	public List<Class<? extends RemoteFile>> getFolderizableFiles();

	public boolean createPathIfRequired(RemoteFile remoteFile);
}
