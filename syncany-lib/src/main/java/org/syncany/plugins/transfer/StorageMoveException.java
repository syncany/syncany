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
package org.syncany.plugins.transfer;

/**
 * The storage move exception is thrown if moving a file on 
 * the remote storage fails. This usually happens if the original file
 * does not exist.
 *  
 * @author Pim Otte
 */
public class StorageMoveException extends StorageException {
	private static final long serialVersionUID = 8929643336708862710L;

	public StorageMoveException(Throwable cause) {
		super(cause);
	}

	public StorageMoveException(String message, Throwable cause) {
		super(message, cause);
	}

	public StorageMoveException(String message) {
		super(message);
	}
}
