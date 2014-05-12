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

import java.util.List;

import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.operations.ls_remote.LsRemoteOperation.LsRemoteOperationResult;

public class LsRemoteCommand extends Command {
	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.INITIALIZED_LOCALDIR;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		List<DatabaseRemoteFile> remoteStatus = ((LsRemoteOperationResult) client.lsRemote()).getUnknownRemoteDatabases();
		
		if (remoteStatus.size() > 0) {
			for (RemoteFile unknownRemoteFile : remoteStatus) {
				out.println("? "+unknownRemoteFile.getName());
			}
		}
		else {
			out.println("No remote changes.");
		}
		
		return 0;
	}
}
