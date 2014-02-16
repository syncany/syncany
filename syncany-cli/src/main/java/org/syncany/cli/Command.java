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

import java.io.File;
import java.io.PrintStream;

import org.syncany.Client;

/**
 * Commands are the central part of Syncany's command line client. Each implementation 
 * of this abstract class represents a command that a user can run on the command line.
 * 
 * <p>The purpose of a command is to read required and optional arguments from the command
 * line, run the corresponding operation, and display the results on the console.
 * Implementations are not supposed to actually run any detailed logic, but are merely
 * the user interface to collection options and print operation output. 
 * 
 * <p>Implementations must implement the {@link #execute(String[]) execute()} method and
 * the {@link #getRequiredCommandScope()} method. While the former actually implements
 * the logic, the latter specifies whether a command must be called inside (or outside) a 
 * local Syncany directory. 
 * 
 * <p>Commands are automatically mapped from their camel case name on the command line
 * to a class name using the {@link CommandFactory}. The command 'ls-remote', for instance,
 * is mapped to the <tt>LsRemoteCommand</tt>.
 *  
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class Command {
	protected File localDir;
	protected Client client;
	protected PrintStream out;

	/**
	 * This method implements the command-specific option-parsing, operation calling 
	 * and output printing. To do so, the method must read and evaluate the given 
	 * arguments, prepare a corresponding operation, call it and display the results
	 * according to a well-defined format.
	 * 
	 * <p>Implementations should not move any business logic in the execute method
	 * (or any other parts of the command).  
	 * 
	 * @param operationArgs Command-specific arguments (might also contain global options) 
	 * @return Returns a return code
	 * @throws Exception If the command or the corresponding operation fails 
	 */
	// TODO [low] Return code of commands not used right now
	public abstract int execute(String[] operationArgs) throws Exception;

	/**
	 * A command can either be executed within an initialized local directory or 
	 * in a regular (non-Syncany) directory. Syncany determines this by searching for
	 * a .syncany folder.
	 * 
	 * <p>The required command scope resembles whether or not a command must be executed 
	 * inside a .syncany directory or not -- or whether it does not matter.
	 *  
	 * @return Returns the required command scope of the command 
	 */
	public abstract CommandScope getRequiredCommandScope();

	public void setLocalDir(File localDir) {
		this.localDir = localDir;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public void setOut(PrintStream out) {
		this.out = out;
	}
}
