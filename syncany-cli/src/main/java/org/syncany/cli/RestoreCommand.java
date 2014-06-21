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

import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.operations.restore.RestoreOperationOptions;
import org.syncany.operations.restore.RestoreOperationResult;

public class RestoreCommand extends AbstractHistoryCommand {
	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.INITIALIZED_LOCALDIR;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		RestoreOperationOptions operationOptions = parseOptions(operationArgs);
		RestoreOperationResult operationResult = client.restore(operationOptions);
		
		printResults(operationResult);
		
		return 0;		
	}
	
	public RestoreOperationOptions parseOptions(String[] operationArgs) throws Exception {
		RestoreOperationOptions operationOptions = new RestoreOperationOptions();

		OptionParser parser = new OptionParser();	
		OptionSpec<Integer> optionRevision = parser.acceptsAll(asList("r", "revision")).withRequiredArg().ofType(Integer.class);
		OptionSpec<String> optionTarget = parser.acceptsAll(asList("t", "target")).withRequiredArg().ofType(String.class);
		
		OptionSet options = parser.parse(operationArgs);	
		
		// --revision=<n>
		if (options.has(optionRevision)) {
			operationOptions.setFileVersion(options.valueOf(optionRevision));
		}
		
		// --target=<file>
		if (options.has(optionTarget)) {
			operationOptions.setRelativeTargetPath(options.valueOf(optionTarget));
		}
		
		// <file-history-id>
		List<?> nonOptionArgs = options.nonOptionArguments();
		
		if (nonOptionArgs.size() != 1) {
			throw new Exception("Invalid Syntax: File history ID must be specified.");
		}
				
		FileHistoryId restoreFileHistory = FileHistoryId.parseFileId(nonOptionArgs.get(0).toString());
		operationOptions.setFileHistoryId(restoreFileHistory);	
		
		return operationOptions;
	}
	
	private void printResults(RestoreOperationResult operationResult) {
		switch (operationResult.getResultCode()) {
		case ACK:
			out.println("File restored to " + operationResult.getTargetFile());
			break;
			
		case NACK_INVALID_FILE:
			out.println("Could not restore file. File entry is present but invalid (Folder?).");
			break;
			
		case NACK_NO_FILE:
			out.println("Could not restore file. No file by that ID or version found, or file ID prefix matches more than one file.");
			break;
			
		default:
			throw new RuntimeException("Invalid result code: " + operationResult.getResultCode());	
		}
	}	
}
