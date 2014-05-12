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
package org.syncany.operations.log;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.Operation;
import org.syncany.operations.OperationOptions;
import org.syncany.operations.OperationResult;

/*
 * TODO [high] The log operation is experimental and needs refactoring #86 
 */
public class LogOperation extends Operation {
	private static final Logger logger = Logger.getLogger(LogOperation.class.getSimpleName());	
	private LogOperationOptions options;
	private SqlDatabase localDatabase;
		
	public LogOperation(Config config, LogOperationOptions options) {
		super(config);		
		
		this.options = options;
		this.localDatabase = new SqlDatabase(config);
	}	
		
	@Override
	public LogOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Log' at client "+config.getMachineName()+" ...");
		logger.log(Level.INFO, "--------------------------------------------");		

		List<PartialFileHistory> fileHistories = null;
		
		if (options.getPaths().isEmpty()) {
			fileHistories = new ArrayList<PartialFileHistory>(localDatabase.getFileHistoriesWithFileVersions());			
		}
		else {
			throw new Exception("Not supported yet.");
			//fileHistories = getFileHistoriesByPath(options.getPaths(), database);
		}
		
		return new LogOperationResult(fileHistories, options.getFormat());
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
