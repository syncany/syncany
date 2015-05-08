/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

/**
 * Wrapper for exceptions that can be thrown when executing operations.
 * 
 * @author Stefan Hugtenburg
 */
public class OperationException extends Exception {
	private static final long serialVersionUID = -4277673078529131874L;

	/**
	 * @param e Exception that caused it.
	 */
	public OperationException(Exception e) {
		super(e);
	}
}
