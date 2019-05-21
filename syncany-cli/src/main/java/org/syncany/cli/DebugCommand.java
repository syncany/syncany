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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.commons.io.IOUtils;
import org.syncany.operations.OperationOptions;
import org.syncany.operations.OperationResult;

/**
 * Intentionally undocumented command to help debugging the application. Implements various
 * helpers for the repository and the local directory.
 * 
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
public class DebugCommand extends Command {
	private static final Logger logger = Logger.getLogger(DebugCommand.class.getSimpleName());

	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.INITIALIZED_LOCALDIR;
	}

	@Override
	public boolean canExecuteInDaemonScope() {
		return false;
	}

	@Override
	public int execute(String[] operationArgs) throws Exception {
		OptionParser parser = new OptionParser();
		OptionSet options = parser.parse(operationArgs);	

		// Files
		List<?> nonOptionArgs = options.nonOptionArguments();
		
		if (nonOptionArgs.size() > 0) {
			String debugCommand = nonOptionArgs.get(0).toString();		
			List<?> newNonOptionArgs = nonOptionArgs.subList(1, nonOptionArgs.size());
			
			if ("decrypt".equals(debugCommand)) {
				runDebugCommand(newNonOptionArgs);
			}
		}
		
		throw new Exception("Invalid syntax. No command given or command unknown.");
	}

	private void runDebugCommand(List<?> nonOptionArgs) throws Exception {
		logger.log(Level.INFO, "Running 'decrypt' command with arguments: "+nonOptionArgs);
		
		if (nonOptionArgs.size() != 1) {
			throw new Exception("Invalid syntax for 'debug' command. Argument expected.");
		}
		
		if (!isInitializedScope()) {
			throw new Exception("Command 'debug' can only be run in initialized local dir.");
		}

		File decryptFile = new File(nonOptionArgs.get(0).toString());

		if (!decryptFile.exists()) {
			throw new Exception("Given file does not exist: "+decryptFile);			
		}
		
		InputStream fileInputStream = config.getTransformer().createInputStream(new FileInputStream(decryptFile));
		
		IOUtils.copy(fileInputStream, System.out);		
		System.exit(0);
	}
	
	private boolean isInitializedScope() {
		return config != null;
	}

	@Override
	public OperationOptions parseOptions(String[] operationArgs) throws Exception {
		return null;
	}

	@Override
	public void printResults(OperationResult result) {
		// Nothing.
	}	
}
