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
package org.syncany.operations.daemon.handlers;

/**
 * Wrapper for vague Exceptions of the HTTP library.
 * 
 * @author Stefan Hugtenburg
 *
 */
public class HttpExchangeException extends Exception {
	private static final long serialVersionUID = 435046193187597016L;

	/**
	 * @param e Exception that caused it.
	 */
	public HttpExchangeException(Exception e) {
		super(e);
	}
}
