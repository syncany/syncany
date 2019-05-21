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
package org.syncany.operations;

import org.simpleframework.xml.Root;

/**
 * Marker interface to indicate a result for a given {@link Operation}.
 * 
 * <p>{@link OperationOptions} are passed to an operation (similar to method parameters).
 * Onve an operation returns, it returns an instance of an operation result.
 * 
 * @see Operation
 * @see OperationOptions
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
@Root(strict = false)
public interface OperationResult {
	// Marker interface for type safety
}
