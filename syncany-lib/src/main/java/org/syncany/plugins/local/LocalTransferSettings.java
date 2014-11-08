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
package org.syncany.plugins.local;

import java.io.File;

import org.simpleframework.xml.Element;
import org.syncany.plugins.transfer.Setup;
import org.syncany.plugins.transfer.TransferSettings;

/**
 * The local connection represents the settings required to create to a
 * backend based on a local (or mounted network) folder. It can be used to
 * initialize/create a {@link LocalTransferManager} and is part of
 * the {@link LocalTransferPlugin}.
 *
 * @author Philipp C. Heckel
 */
public class LocalTransferSettings extends TransferSettings {
	@Element(required = true)
	@Setup(order = 1, description = "Path to local repository")
	public File path;	

	public File getPath() {
		return path;
	}

	public void setPath(File path) {
		this.path = path;
	}
}
