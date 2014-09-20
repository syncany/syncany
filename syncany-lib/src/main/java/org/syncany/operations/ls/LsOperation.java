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
package org.syncany.operations.ls;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.Operation;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

public class LsOperation extends Operation {
	private static final Logger logger = Logger.getLogger(LsOperation.class.getSimpleName());	
	private LsOperationOptions options;
	private SqlDatabase localDatabase;
		
	public LsOperation(Config config, LsOperationOptions options) {
		super(config);		
		
		this.options = options;
		this.localDatabase = new SqlDatabase(config);
	}	
		
	@Override
	public LsOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Ls' at client "+config.getMachineName()+" ...");
		logger.log(Level.INFO, "--------------------------------------------");		

		String pathExpression = parsePathExpression(options.getPathExpression());
		HashSet<FileType> fileTypes = options.getFileTypes();

		Map<String, FileVersion> fileTree = localDatabase.getFileTree(pathExpression, options.getDate(), options.isRecursive(), fileTypes);
		Map<FileHistoryId, PartialFileHistory> fileHistories = null;
		
		if (options.isFetchHistories()) {
			fileHistories = fetchFileHistories(fileTree);
		}
		
		return new LsOperationResult(fileTree, fileHistories);
	}

	private Map<FileHistoryId, PartialFileHistory> fetchFileHistories(Map<String, FileVersion> fileTree) {
		// Get file history IDs
		List<FileHistoryId> fileHistoryIds = new ArrayList<>(Collections2.transform(fileTree.values(), new Function<FileVersion, FileHistoryId>() {
			public FileHistoryId apply(FileVersion fileVersion) {
				return fileVersion.getFileHistoryId();
			}			
		}));

		return localDatabase.getFileHistories(fileHistoryIds);
	}

	private String parsePathExpression(String filter) {
		if (filter != null) {
			 if (filter.contains("^") || filter.contains("*")) {
				 return filter.replace('^', '%').replace('*', '%');
			 }
			 else {
				 return filter + "%";
			 }
		}
		else {
			return null;
		}
	}
}
