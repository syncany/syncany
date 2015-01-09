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

import java.util.List;

import org.syncany.operations.OperationOptions;
import org.syncany.operations.OperationResult;
import org.syncany.operations.daemon.messages.LsRemoteStartSyncExternalEvent;
import org.syncany.operations.ls_remote.LsRemoteOperationResult;
import org.syncany.plugins.transfer.files.DatabaseRemoteFile;
import org.syncany.plugins.transfer.files.RemoteFile;

import com.google.common.eventbus.Subscribe;

public class LsRemoteCommand extends Command {
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
		LsRemoteOperationResult operationResult = client.lsRemote();
		printResults(operationResult);		
		
		return 0;
	}

	@Override
	public OperationOptions parseOptions(String[] operationArgs) throws Exception {		
		return null;
	}

	@Override
	public void printResults(OperationResult operationResult) {
		LsRemoteOperationResult concreteOperationResult = (LsRemoteOperationResult) operationResult;
		List<DatabaseRemoteFile> remoteStatus = concreteOperationResult.getUnknownRemoteDatabases();
		
		if (remoteStatus.size() > 0) {
			for (RemoteFile unknownRemoteFile : remoteStatus) {
				out.println("? "+unknownRemoteFile.getName());
			}
		}
		else {
			out.println("No remote changes.");
		}
	}	
	
	@Subscribe
	public void onLsRemoteStartEventReceived(LsRemoteStartSyncExternalEvent syncEvent) {
		out.printr("Checking remote changes ...");
	}
}
