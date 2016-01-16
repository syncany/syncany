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
package org.syncany.cli;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.util.StringUtil;

/**
 * The command factory can be used to instantiate a new command from a  
 * command name. The {@link CommandLineClient} uses this class to create
 * and run commands by mapping a command argument to a corresponding
 * {@link Command} class.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class CommandFactory {
	private static final Logger logger = Logger.getLogger(CommandFactory.class.getSimpleName());
	
	/**
	 * Maps the given command name to a corresponding {@link Command} class and 
	 * instantiates it. The command name is camel-cased and mapped to a FQCN.
	 * 
	 * <p>Example: The command 'ls-remote' is mapped to the FQCN
	 * <tt>org.syncany.cli.LsRemoteCommand</tt>.
	 * 
	 * @param commandName Command name, e.g. ls-remote or init
	 * @return Returns a <tt>Command</tt> instance, or <tt>null</tt> if the command name cannot be mapped to a class
	 */
	public static Command getInstance(String commandName) {
		String thisPackage = CommandFactory.class.getPackage().getName();
		String camelCaseCommandName = StringUtil.toCamelCase(commandName);
		String fqCommandClassName = thisPackage+"."+camelCaseCommandName+Command.class.getSimpleName();
		
		// Try to load!
		try {
			Class<?> commandClass = Class.forName(fqCommandClassName);
			return (Command) commandClass.newInstance();
		} 
		catch (Exception ex) {
			logger.log(Level.INFO, "Could not find operation FQCN " + fqCommandClassName, ex);
			return null;
		}		
	}
}
