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
package org.syncany.config;

/**
 * The local event bus is used to pass messages and events between
 * operations and commands, as well as to replace traditional listeners.
 * 
 * <p>It is heavily used by the daemon to distribute requests and responses
 * into the application; and to pass responses back to the daemon. 
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class LocalEventBus extends InternalEventBus {	
	public static LocalEventBus getInstance() {
		return InternalEventBus.getInstance(LocalEventBus.class);
	}
}
