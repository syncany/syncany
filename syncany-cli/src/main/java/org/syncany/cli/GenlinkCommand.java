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

import static java.util.Arrays.asList;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.operations.OperationResult;
import org.syncany.operations.init.GenlinkOperationOptions;
import org.syncany.operations.init.GenlinkOperationResult;

public class GenlinkCommand extends AbstractInitCommand {
	private boolean machineReadableOutput;
	
	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.INITIALIZED_LOCALDIR;
	}
	
	@Override
	public boolean canExecuteInDaemonScope() {
		return true;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		GenlinkOperationOptions operationOptions = parseOptions(operationArgs);		
		GenlinkOperationResult operationResult = client.genlink(operationOptions);		
		
		printResults(operationResult);
		
		return 0;		
	}
	
	@Override
	public GenlinkOperationOptions parseOptions(String[] operationArgs) {
		GenlinkOperationOptions operationOptions = new GenlinkOperationOptions();
		
		OptionParser parser = new OptionParser();
		OptionSpec<Void> optionMachineReadable = parser.acceptsAll(asList("m", "machine-readable"));		
		OptionSpec<Void> optionShort = parser.acceptsAll(asList("s", "short"));		

		parser.allowsUnrecognizedOptions();		
		OptionSet options = parser.parse(operationArgs);

		// --machine-readable, -m
		machineReadableOutput = options.has(optionMachineReadable);
		
		// --short, -s
		operationOptions.setShortUrl(options.has(optionShort));

		return operationOptions;
	}
	
	@Override
	public void printResults(OperationResult operationResult) {
		GenlinkOperationResult concreteOperationResult = (GenlinkOperationResult) operationResult;
		
		if (!machineReadableOutput) {
			out.println();
			out.println("To share the same repository with others, you can share this link:");
		}
		
		printLink(concreteOperationResult, machineReadableOutput);			
	}	
}
