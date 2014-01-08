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
package org.syncany.gui;

import java.util.HashMap;

import org.syncany.util.SyncanyParameters;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class UserInput extends HashMap<SyncanyParameters, String>{
	private static final long serialVersionUID = 7692703262033066027L;

	@Override
	public String put(SyncanyParameters key, String value) {
		if (key.containsValue(value))
			return super.put(key, value);

		throw new RuntimeException(String.format("Value [%s] not compatible with key [%s]", value, key.toString()));
	}
	
	@Override
	public String get(Object key) {
		if (!(key instanceof SyncanyParameters))
			throw new RuntimeException(String.format("Key should be of type SyncanyParameters"));
		return super.get(key);
	}
}
