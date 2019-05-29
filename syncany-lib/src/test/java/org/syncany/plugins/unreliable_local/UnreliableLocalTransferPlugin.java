/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com>
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
package org.syncany.plugins.unreliable_local;

import org.syncany.plugins.transfer.TransferPlugin;

/**
 * The unreliable local plugin can be used for test purposes to
 * test connection issues with the backend storage. Each operation of the
 * plugin (e.g upload, download, ...) can be failed on purpose through
 * regular expressions on the operation signature.
 *
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
public class UnreliableLocalTransferPlugin extends TransferPlugin {
	public UnreliableLocalTransferPlugin() {
		super("unreliable_local");
	}
}
