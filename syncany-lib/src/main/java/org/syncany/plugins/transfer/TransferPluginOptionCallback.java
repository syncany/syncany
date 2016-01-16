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
package org.syncany.plugins.transfer;

/**
 * Option callbacks are called during initialization before and after the
 * corresponding setting is queried.
 *
 * @see org.syncany.plugins.transfer.TransferPluginOptions
 * @author Christian Roth <christian.roth@port17.de>
 */
public interface TransferPluginOptionCallback {
	/**
	 * Called before a setting value is queried.
	 *
	 * @return The message to display.
	 */
	public String preQueryCallback();

	/**
	 * Called after a setting value is queried.
	 *
	 * @param optionValue The value of the field
	 * @return The message to display.
	 */
	public String postQueryCallback(String optionValue);
}
