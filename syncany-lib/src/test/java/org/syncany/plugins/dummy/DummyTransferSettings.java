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
package org.syncany.plugins.dummy;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Validate;
import org.syncany.plugins.annotations.Encrypted;
import org.syncany.plugins.setup.Setup;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransferSettings;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public class DummyTransferSettings extends TransferSettings {

	@Element(required = true)
	@Encrypted
	@Setup(order = 1, description = "A foo field")
	public String foo;

	@Element(name = "baz", required = false)
	@Setup(order = 3, description = "A baz field")
	public String baz;

	@Element(name = "number")
	@Setup(order = 2)
	public int number;

	@Element(name = "nest", required = false)
	@Setup(order = 4, description = "Some nested settings")
	public DummyTransferSettings subsettings;

	@Validate
	public void validate() throws StorageException {
		if (baz != null && !baz.equalsIgnoreCase("baz")) {
			throw new StorageException("Only allowed value for baz field is 'baz'");
		}
	}
}
