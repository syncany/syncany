/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import java.io.PrintStream;

import org.syncany.Client;
import org.syncany.operations.DownOperation.DownOperationOptions;
import org.syncany.operations.SyncOperation.SyncOperationOptions;
import org.syncany.operations.SyncOperation.SyncOperationResult;
import org.syncany.operations.UpOperation.UpOperationOptions;

public class SyncCommand extends Command {
	private DownCommand downCommand;
	private UpCommand upCommand;
	
	public SyncCommand() {
		downCommand = new DownCommand();
		upCommand = new UpCommand();
	}
	
	@Override
	public void setClient(Client client) {
		super.setClient(client);
		
		downCommand.setClient(client);
		upCommand.setClient(client);
	}
	
	@Override
	public void setOut(PrintStream out) {
		super.setOut(out);
		
		downCommand.setOut(out);
		upCommand.setOut(out);
	}
	
	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.INITIALIZED_LOCALDIR;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		SyncOperationOptions operationOptions = parseSyncOptions(operationArgs);		
		SyncOperationResult operationResult = client.sync(operationOptions);
		
		printResults(operationResult);
		
		return 0;
	}
	
	public SyncOperationOptions parseSyncOptions(String[] operationArguments) throws Exception {
		DownOperationOptions syncDownOptions = downCommand.parseOptions(operationArguments);
		UpOperationOptions syncUpOptions = upCommand.parseOptions(operationArguments);

		SyncOperationOptions syncOptions = new SyncOperationOptions();
		syncOptions.setSyncDownOptions(syncDownOptions);
		syncOptions.setSyncUpOptions(syncUpOptions);
		
		return syncOptions;
	}
	
	public void printResults(SyncOperationResult operationResult) {
		downCommand.printResults(operationResult.getSyncDownResult());		
		upCommand.printResults(operationResult.getSyncUpResult());
	}	
}
