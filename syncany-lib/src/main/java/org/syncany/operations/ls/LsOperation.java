/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com>
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
import java.util.List;
import java.util.Map;
import java.util.Set;
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
		logger.log(Level.INFO, "Running 'Ls' at client " + config.getMachineName() + " ...");
		logger.log(Level.INFO, "--------------------------------------------");

		String pathExpression = parsePathExpression(options.getPathExpression(), options.isFileHistoryId());
		Set<FileType> fileTypes = options.getFileTypes();

		List<FileVersion> fileList = localDatabase.getFileList(pathExpression, options.getDate(), options.isFileHistoryId(), options.isRecursive(), options.isDeleted(), fileTypes);
		Map<FileHistoryId, PartialFileHistory> fileHistories = null;

		if (options.isFetchHistories()) {
			fileHistories = fetchFileHistories(fileList);
		}

		return new LsOperationResult(fileList, fileHistories);
	}

	private Map<FileHistoryId, PartialFileHistory> fetchFileHistories(List<FileVersion> fileTree) {
		// Get file history IDs
		List<FileHistoryId> fileHistoryIds = new ArrayList<>(Collections2.transform(fileTree, new Function<FileVersion, FileHistoryId>() {
			@Override
			public FileHistoryId apply(FileVersion fileVersion) {
				return fileVersion.getFileHistoryId();
			}
		}));

		return localDatabase.getFileHistories(fileHistoryIds);
	}

	private String parsePathExpression(String pathExpression, boolean isFileHistoryId) {
		if (pathExpression != null) {
			if (isFileHistoryId) {
				String randomFileHistoryId = FileHistoryId.secureRandomFileId().toString();
				boolean isFullLength = pathExpression.length() == randomFileHistoryId.length();
				
				if (isFullLength) {
					return pathExpression;
				}
				else {
					return pathExpression + "%";
				}
			}
			else {
				if (pathExpression.contains("^") || pathExpression.contains("*")) {
					return pathExpression.replace('^', '%').replace('*', '%');
				}
				else {
					return pathExpression + "%";
				}
			}
		}
		else {
			return null;
		}
	}
}
