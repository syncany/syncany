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

import org.syncany.operations.CleanupOperation.CleanupOperationOptions;
import org.syncany.operations.CleanupOperation.CleanupOperationResult;

public class CleanupCommand extends Command {
	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.INITIALIZED_LOCALDIR;
	}

	@Override
	public int execute(String[] operationArgs) throws Exception {
		CleanupOperationOptions operationOptions = parseOptions(operationArgs);
		CleanupOperationResult operationResult = client.cleanup(operationOptions);

		printResults(operationResult);

		return 0;
	}

	public CleanupOperationOptions parseOptions(String[] operationArgs) throws Exception {
		CleanupOperationOptions operationOptions = new CleanupOperationOptions();

		OptionParser parser = new OptionParser();
		OptionSpec<Void> optionMergeDatabases = parser.acceptsAll(asList("m", "merge-databases"));
		OptionSpec<Integer> optionKeepVersions = parser.acceptsAll(asList("k", "keep-versions")).withRequiredArg().ofType(Integer.class);

		OptionSet options = parser.parse(operationArgs);

		// Cross-checks
		if (!options.has(optionMergeDatabases) && !options.has(optionKeepVersions)) {
			throw new Exception("No cleanup option given. Please choose at least one option.");
		}
		
		// --merge-databases
		operationOptions.setMergeRemoteFiles(options.has(optionMergeDatabases));
		
		// -k=<count>, --keep-versions=<count>
		if (options.has(optionKeepVersions)) {
			int keepVersionCount = options.valueOf(optionKeepVersions);
			
			if (keepVersionCount < 1) {
				throw new Exception("Invalid value for --keep-versions="+keepVersionCount+"; must be >= 1");
			}
			
			operationOptions.setRemoveOldVersions(true);
			operationOptions.setKeepVersionsCount(options.valueOf(optionKeepVersions));			
		}
		
		return operationOptions;
	}

	private void printResults(CleanupOperationResult operationResult) {
		// Nothing to print (yet)
	}
}
