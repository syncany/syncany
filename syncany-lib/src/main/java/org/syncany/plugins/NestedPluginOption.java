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
package org.syncany.plugins;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */
public class NestedPluginOption extends PluginOption {
	private final List<PluginOption> nestedOptions;

	public NestedPluginOption(Field field, String name, String description, Type type, boolean encrypted, boolean sensitive, boolean required,
			Class<? extends PluginOptionCallback> callback, List<PluginOption> nestedOptions) {

		super(field, name, description, type, encrypted, sensitive, required, callback);
		this.nestedOptions = nestedOptions;
	}

	public List<PluginOption> getNestedOptions() {
		return nestedOptions;
	}
}
