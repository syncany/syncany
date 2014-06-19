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

import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.operations.versions.VersionsOperationOptions;
import org.syncany.operations.versions.VersionsOperationResult;

public class VersionsCommand extends AbstractHistoryCommand {
	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.INITIALIZED_LOCALDIR;
	}

	@Override
	public int execute(String[] operationArgs) throws Exception {
		VersionsOperationOptions operationOptions = new VersionsOperationOptions();
		VersionsOperationResult operationResult = client.versions(operationOptions);

		printResults(operationResult);

		return 0;
	}	

	private void printResults(VersionsOperationResult operationResult) {		
		for (PartialFileHistory fileHistory : operationResult.getFileHistories()) {			
			FileVersion lastVersion = fileHistory.getLastVersion();
	
			out.printf("%s %s\n", lastVersion.getPath(), fileHistory.getFileHistoryId());
	
			for (FileVersion fileVersion : fileHistory.getFileVersions().values()) {
				out.print('\t');
				printOneVersion(fileVersion);
	
				if (fileVersion.getPath().equals(lastVersion.getPath())) {
					out.println();
				}
				else {
					out.println(" " + fileVersion.getPath());
				}
			}
		}
	}
}
