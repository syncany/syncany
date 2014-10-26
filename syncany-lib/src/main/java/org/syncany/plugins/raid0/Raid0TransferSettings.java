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
package org.syncany.plugins.raid0;

import org.simpleframework.xml.Element;
import org.syncany.plugins.Setup;
import org.syncany.plugins.transfer.TransferSettings;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */
public class Raid0TransferSettings extends TransferSettings {	
	@Element(name = "storage1", required = true)
	@Setup(order = 1, description = "First storage settings")
	public TransferSettings storage1;

	@Element(name = "storage2", required = true)
	@Setup(order = 2, description = "Second storage settings")
	public TransferSettings storage2;

	public TransferSettings getTransferSettings1() {
		return storage1;
	}

	public TransferSettings getTransferSettings2() {
		return storage2;
	}
}
