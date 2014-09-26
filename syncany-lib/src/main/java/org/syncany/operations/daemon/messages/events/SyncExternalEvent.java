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
package org.syncany.operations.daemon.messages.events;

import org.syncany.operations.daemon.messages.api.ExternalEvent;

public abstract class SyncExternalEvent extends ExternalEvent {
	public enum Type {
		 UP_INDEX_START, UP_INDEX_END, UP_UPLOAD_FILE, UP_UPLOAD_FILE_IN_TX,

		DOWN_START, DOWN_END, DOWN_DOWNLOAD_FILE,

		STATUS_START, STATUS_END, 
	};
}
