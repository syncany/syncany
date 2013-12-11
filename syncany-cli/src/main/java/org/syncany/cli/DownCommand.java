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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.syncany.operations.ChangeSet;
import org.syncany.operations.DownOperation.DownOperationOptions;
import org.syncany.operations.DownOperation.DownOperationResult;

public class DownCommand extends Command {
	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.INITIALIZED_LOCALDIR;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		DownOperationOptions operationOptions = parseOptions(operationArgs);		
		DownOperationResult operationResult = client.down(operationOptions);		
		
		printResults(operationResult);
		
		return 0;
	}

	public DownOperationOptions parseOptions(String[] operationArguments) {
		return new DownOperationOptions();
	}

	public void printResults(DownOperationResult operationResult) {
		ChangeSet changeSet = operationResult.getChangeSet();
		
		if (changeSet.hasChanges()) {
			List<String> newFiles = new ArrayList<String>(changeSet.getNewFiles());
			List<String> changedFiles = new ArrayList<String>(changeSet.getChangedFiles());
			List<String> deletedFiles = new ArrayList<String>(changeSet.getDeletedFiles());
			
			Collections.sort(newFiles);
			Collections.sort(changedFiles);
			Collections.sort(deletedFiles);
			
			for (String newFile : newFiles) {
				out.println("A "+newFile);
			}
	
			for (String changedFile : changedFiles) {
				out.println("M "+changedFile);
			}
			
			for (String deletedFile : deletedFiles) {
				out.println("D "+deletedFile);
			}	
			
			out.println("Sync down finished.");
		}
		else {
			out.println("Sync down skipped, no remote changes.");
		}
	}
}
