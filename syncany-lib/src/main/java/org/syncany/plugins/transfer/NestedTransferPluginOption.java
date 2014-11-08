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

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;

/**
 * A nested plugin option is a special {@link TransferPluginOption} -- namely an
 * option that contains a complex object rather than just a simple value.
 *
 * <p>Nested plugin options are typically used to represent/use sub-plugins
 * within a certain plugin, e.g. to allow building a RAID0/1 plugin.
 *
 * @author Christian Roth <christian.roth@port17.de>
 */
public class NestedTransferPluginOption extends TransferPluginOption {
	private final List<TransferPluginOption> options;

	public NestedTransferPluginOption(Field field, String name, String description, Type type, boolean encrypted, boolean sensitive,
			boolean singular, boolean required, Class<? extends TransferPluginOptionCallback> callback,
			Class<? extends TransferPluginOptionConverter> converter, List<TransferPluginOption> nestedOptions) {

		super(field, name, description, type, encrypted, sensitive, singular, required, callback, converter);
		this.options = nestedOptions;
	}

	public List<TransferPluginOption> getOptions() {
		return options;
	}
}
