/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import java.io.File;
import java.io.PrintStream;

import org.syncany.Client;

public abstract class Command {
	protected File localDir;
	protected Client client;
	protected PrintStream out;	
	
	public void setLocalDir(File localDir) {
		this.localDir = localDir;
	}

	public void setClient(Client client) {
		this.client = client;
	}
	
	public void setOut(PrintStream out) {
		this.out = out;
	}
	
	public abstract int execute(String[] operationArgs) throws Exception;
	public abstract boolean initializedLocalDirRequired();
}
