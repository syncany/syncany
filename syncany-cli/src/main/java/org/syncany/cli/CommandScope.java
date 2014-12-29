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
package org.syncany.cli;

/**
 * Commands can be run either in a repository scope or outside a repository
 * scope. Repository-initializing commands such as 'init' or 'connect' must be
 * run outside a repo dir.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public enum CommandScope {
	/**
	 * Indicates that the local folder must be initialized
	 * to run this command. If it is not, the command will not run.
	 */
	INITIALIZED_LOCALDIR,

	/**
	 * Indicates that the local folder must not be initialized
	 * to run this command. If it is, the command will not run.
	 */
	UNINITIALIZED_LOCALDIR,

	/**
	 * Indicates that for this command to run, it does not matter
	 * whether the local folder is initialized or not.
	 */
	ANY
}
