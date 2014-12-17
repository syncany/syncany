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

import java.util.ArrayList;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.syncany.cli.util.CliTableUtil;
import org.syncany.config.to.FolderTO;
import org.syncany.operations.OperationResult;
import org.syncany.operations.daemon.DaemonOperation;
import org.syncany.operations.daemon.DaemonOperationOptions;
import org.syncany.operations.daemon.DaemonOperationOptions.DaemonAction;
import org.syncany.operations.daemon.DaemonOperationResult;

public class DaemonCommand extends Command {
	private DaemonAction action;
	
	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.ANY;
	}

	@Override
	public boolean canExecuteInDaemonScope() {
		return false;
	}

	@Override
	public int execute(String[] operationArgs) throws Exception {
		DaemonOperationOptions operationOptions = parseOptions(operationArgs);
		DaemonOperationResult operationResult = new DaemonOperation(operationOptions).execute();

		printResults(operationResult);

		return 0;		
	}
	
	@Override
	public DaemonOperationOptions parseOptions(String[] operationArgs) throws Exception {
		DaemonOperationOptions operationOptions = new DaemonOperationOptions();

		OptionParser parser = new OptionParser();
		OptionSet options = parser.parse(operationArgs);

		// Files
		List<?> nonOptionArgs = options.nonOptionArguments();

		if (nonOptionArgs.size() == 0) {
			throw new Exception("Invalid syntax, no action given (start, stop, reload, restart, status, force-stop, list, add, remove).");
		}

		// <action>
		String actionStr = nonOptionArgs.get(0).toString();
		action = parseDaemonAction(actionStr);

		operationOptions.setAction(action);

		// add|remove <folder-path>
		if (action == DaemonAction.ADD || action == DaemonAction.REMOVE) {
			if (nonOptionArgs.size() != 2) {
				throw new Exception("Invalid syntax, please specify a folder path.");
			}

			// <folder-path>
			String watchPath = nonOptionArgs.get(1).toString();
			operationOptions.setWatchRoot(watchPath);
		}

		return operationOptions;
	}

	private DaemonAction parseDaemonAction(String actionStr) throws Exception {
		try {
			return DaemonAction.valueOf(actionStr.toUpperCase());
		}
		catch (Exception e) {
			throw new Exception("Invalid syntax, unknown action '" + actionStr + "'");
		}
	}

	@Override
	public void printResults(OperationResult operationResult) {
		DaemonOperationResult concreteOperationResult = (DaemonOperationResult) operationResult;
		
		switch (action) {
		case LIST:
			printResultList(concreteOperationResult);
			return;

		case ADD:
			printResultAdd(concreteOperationResult);
			return;

		case REMOVE:
			printResultRemove(concreteOperationResult);
			return;

		default:
			// Nothing.
		}
	}

	private void printResultList(DaemonOperationResult operationResult) {
		switch (operationResult.getResultCode()) {
		case OK:
			List<String[]> tableValues = new ArrayList<String[]>();
			tableValues.add(new String[] { "#", "Enabled", "Path" });

			for (int i=0; i<operationResult.getWatchList().size(); i++) {
				FolderTO folderTO = operationResult.getWatchList().get(i);		
				
				String number = Integer.toString(i+1);
				String enabledStr = folderTO.isEnabled() ? "yes" : "no";
				
				tableValues.add(new String[] { number, enabledStr, folderTO.getPath()  });
			}

			CliTableUtil.printTable(out, tableValues, "No managed folders found.");			
			break;
						
		default:
			throw new RuntimeException("Invalid result code for this action: " + operationResult.getResultCode());
		}		
	}
	
	private void printResultAdd(DaemonOperationResult operationResult) {
		switch (operationResult.getResultCode()) {
		case OK:
			out.println("Folder successfully added to daemon config.");
			out.println("Run 'sy daemon restart' to apply the changes.");
			out.println();
			break;
			
		case NOK_FOLDER_EXISTS:
			out.println("Folder was NOT added, because it already exists in the daemon configuration.");
			out.println();
			break;
			
		default:
			throw new RuntimeException("Invalid result code for this action: " + operationResult.getResultCode());
		}
	}	

	private void printResultRemove(DaemonOperationResult operationResult) {
		switch (operationResult.getResultCode()) {
		case OK:
			out.println("Folder successfully removed from the daemon config.");
			out.println("Run 'sy daemon restart' to apply the changes.");
			out.println();
			break;
			
		case NOK_FOLDER_DOESNT_EXIST:
			out.println("Folder was NOT removed, because it does not exist in the daemon config.");
			out.println();
			break;
			
		default:
			throw new RuntimeException("Invalid result code for this action: " + operationResult.getResultCode());
		}		
	}
}
