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

import static java.util.Arrays.asList;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.operations.OperationResult;
import org.syncany.operations.genlink.GenlinkOperationOptions;
import org.syncany.operations.init.GenlinkOperationResult;

public class GenlinkCommand extends AbstractInitCommand {
	private GenlinkOperationOptions commandOptions;
	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.INITIALIZED_LOCALDIR;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		commandOptions = parseOptions(operationArgs);
		GenlinkOperationResult operationResult = client.genlink();		
		printResults(operationResult);
		
		return 0;		
	}
	
	public GenlinkOperationOptions parseOptions(String[] operationArgs) {
		GenlinkOperationOptions commandOptions = new GenlinkOperationOptions();

		OptionParser parser = new OptionParser();			
		OptionSpec<Void> optionShort = parser.acceptsAll(asList("s", "short"));
		
		OptionSet options = parser.parse(operationArgs);

		// --short
		commandOptions.setShortOutput(options.has(optionShort));
		
		return commandOptions;
	}
	
	public void printResults(OperationResult operationResult) {
		GenlinkOperationResult concreteOperationResult = (GenlinkOperationResult) operationResult;
		
		if (!commandOptions.isShortOutput()) {
			out.println();
			out.println("To share the same repository with others, you can share this link:");
		}
		
		printLink(concreteOperationResult, commandOptions.isShortOutput());			
	}
	
	@Override
	public boolean canExecuteInDaemonScope() {
		return false;
	}
}
