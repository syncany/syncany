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

import org.syncany.operations.StatusOperation.StatusOperationOptions;
import org.syncany.operations.StatusOperation.StatusOperationResult;

public class StatusCommand extends Command {
	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.INITIALIZED_LOCALDIR;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		StatusOperationOptions operationOptions = parseOptions(operationArgs);
		StatusOperationResult operationResult = client.status(operationOptions);
		
		printResults(operationResult);
		
		return 0;
	}

	public StatusOperationOptions parseOptions(String[] operationArgs) {
		StatusOperationOptions operationOptions = new StatusOperationOptions();

		OptionParser parser = new OptionParser();	
		parser.allowsUnrecognizedOptions();
		
		OptionSpec<Void> optionForceChecksum = parser.acceptsAll(asList("f", "force-checksum"));
		
		OptionSet options = parser.parse(operationArgs);	
		
		// --force-checksum
		operationOptions.setForceChecksum(options.has(optionForceChecksum));
		
		return operationOptions;
	}	

	public void printResults(StatusOperationResult operationResult) {
		if (operationResult.getChangeSet().hasChanges()) {
			for (String newFile : operationResult.getChangeSet().getNewFiles()) {
				out.println("? "+newFile);
			}

			for (String changedFile : operationResult.getChangeSet().getChangedFiles()) {
				out.println("M "+changedFile);
			}
			
			for (String deletedFile : operationResult.getChangeSet().getDeletedFiles()) {
				out.println("D "+deletedFile);
			}						
		}
		else {
			out.println("No local changes.");
		}
	}

}
