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

import org.syncany.operations.init.GenlinkOperationResult;

public class GenlinkCommand extends AbstractInitCommand {
	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.INITIALIZED_LOCALDIR;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		GenlinkCommandOptions commandOptions = parseGenlinkOptions(operationArgs);
		GenlinkOperationResult operationResult = client.genlink();		
		printResults(operationResult, commandOptions);
		
		return 0;		
	}
	
	private GenlinkCommandOptions parseGenlinkOptions(String[] operationArgs) {
		GenlinkCommandOptions commandOptions = new GenlinkCommandOptions();

		OptionParser parser = new OptionParser();			
		OptionSpec<Void> optionShort = parser.acceptsAll(asList("s", "short"));
		
		OptionSet options = parser.parse(operationArgs);

		// --short
		commandOptions.setShortOutput(options.has(optionShort));
		
		return commandOptions;
	}
	
	private void printResults(GenlinkOperationResult operationResult, GenlinkCommandOptions commandOptions) {
		if (!commandOptions.isShortOutput()) {
			out.println();
			out.println("To share the same repository with others, you can share this link:");
		}
		
		printLink(operationResult, commandOptions.isShortOutput());			
	}
	
	private class GenlinkCommandOptions {
		private boolean shortOutput = false;

		public boolean isShortOutput() {
			return shortOutput;
		}

		public void setShortOutput(boolean shortOutput) {
			this.shortOutput = shortOutput;
		}				
	}
}
