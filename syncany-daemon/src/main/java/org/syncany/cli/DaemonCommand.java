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
package org.syncany.cli;

import java.util.logging.Logger;

import org.syncany.daemon.Launcher;
import org.syncany.operations.LogOperation;

public class DaemonCommand extends Command {
	private static final Logger logger = Logger.getLogger(LogOperation.class.getSimpleName());

	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.ANY;
	}

	@Override
	public int execute(String[] operationArgs) throws Exception {
		new Launcher().start();		
		return 0;
	}
}
