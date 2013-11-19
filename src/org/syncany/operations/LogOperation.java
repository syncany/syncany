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
package org.syncany.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.PartialFileHistory;

public class LogOperation extends Operation {
	private static final Logger logger = Logger.getLogger(LogOperation.class.getSimpleName());	
	private LogOperationOptions options;
	
	public LogOperation() {
		super(null);
		this.options = new LogOperationOptions();
	}	

	public LogOperation(Config config) {
		this(config, null);
	}	
	
	public LogOperation(Config config, LogOperationOptions options) {
		super(config);		
		this.options = options;
	}	
		
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Log' at client "+config.getMachineName()+" ...");
		logger.log(Level.INFO, "--------------------------------------------");
		
		Database database = loadLocalDatabase();		
		DatabaseVersion currentDatabaseVersion = database.getLastDatabaseVersion();
		
		if (currentDatabaseVersion == null) {
			throw new Exception("No database versions yet locally. Nothing to show here.");
		}

		List<PartialFileHistory> fileHistories = null;
		
		if (options.getPaths().isEmpty()) {
			fileHistories = new ArrayList<PartialFileHistory>(database.getFileHistories());			
		}
		else {
			fileHistories = getFileHistoriesByPath(options.getPaths(), database);
		}
		
		return new LogOperationResult(fileHistories,options.getFormat());
	}			
	
	private List<PartialFileHistory> getFileHistoriesByPath(List<String> filePaths, Database database) {				
		List<PartialFileHistory> fileHistories = new ArrayList<PartialFileHistory>();

		for (String filePath : filePaths) {
			PartialFileHistory fileHistory = database.getFileHistory(filePath);
			
			if (fileHistory != null) {
				fileHistories.add(fileHistory);
			}
			else {
				logger.log(Level.INFO, "Cannot find file history for file "+filePath);
			}
		}
		
		return fileHistories;
	}
	
	public static class LogOperationOptions implements OperationOptions {
		private List<String> paths;		
		
		private String format;
		
		public List<String> getPaths() {
			return paths;
		}
		
		public void setPaths(List<String> paths) {
			this.paths = paths;
		}

		public String getFormat() {
			return format;
		}

		public void setFormat(String format) {
			this.format = format;
		}
		
		
	}
	
	public class LogOperationResult implements OperationResult {
		private List<PartialFileHistory> fileHistories;
		
		private String format;
		
		public LogOperationResult(List<PartialFileHistory> fileHistories, String format) {
			this.fileHistories = fileHistories;
			this.format =format;
		}

		public List<PartialFileHistory> getFileHistories() {
			return fileHistories;
		}	
		
		public String getFormat() {
			return format;
		}
		
	}
}
