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
package org.syncany.operations;

import org.syncany.config.Config;
import org.syncany.operations.DownOperation.DownOperationOptions;
import org.syncany.operations.DownOperation.DownOperationResult;
import org.syncany.operations.UpOperation.UpOperationOptions;
import org.syncany.operations.UpOperation.UpOperationResult;
import org.syncany.operations.listener.WatchOperationListener;

/**
 * The sync operation combines the {@link DownOperation} and the {@link UpOperation}
 * by subsequently running the two operations. Consequently, it takes arguments
 * for both operations and returns results for both of them. 
 *   
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class SyncOperation extends Operation {
	private SyncOperationOptions options;
	private WatchOperationListener watchOperationListener;
	
	public SyncOperation(Config config) {
		this(config, new SyncOperationOptions(), null);
	}	
	
	public SyncOperation(Config config, WatchOperationListener watchOperationListener) {
		this(config, new SyncOperationOptions(), watchOperationListener);
	}	
	
	public SyncOperation(Config config, SyncOperationOptions options, WatchOperationListener watchOperationListener) {
		super(config);	

		this.watchOperationListener = watchOperationListener;
		this.options = options;
	}		
	
	@Override
	public SyncOperationResult execute() throws Exception {
		DownOperation syncDown = new DownOperation(config, options.getSyncDownOptions(), watchOperationListener);
		UpOperation syncUp = new UpOperation(config, options.getSyncUpOptions(), watchOperationListener);
		
		DownOperationResult syncDownResults = syncDown.execute();
		UpOperationResult syncUpResults = syncUp.execute();
		
		return new SyncOperationResult(syncDownResults, syncUpResults);
	}
	
	public static class SyncOperationOptions implements OperationOptions {
		private UpOperationOptions syncUpOptions = new UpOperationOptions();
		private DownOperationOptions syncDownOptions = new DownOperationOptions();

		public DownOperationOptions getSyncDownOptions() {
			return syncDownOptions;
		}
		
		public void setSyncDownOptions(DownOperationOptions syncDownOptions) {
			this.syncDownOptions = syncDownOptions;
		}				
		
		public UpOperationOptions getSyncUpOptions() {
			return syncUpOptions;
		}
		
		public void setSyncUpOptions(UpOperationOptions syncUpOptions) {
			this.syncUpOptions = syncUpOptions;
		}		
	}
	
	public class SyncOperationResult implements OperationResult {
		private DownOperationResult syncDownResult;
		private UpOperationResult syncUpResult;
		
		public SyncOperationResult(DownOperationResult syncDownResult, UpOperationResult syncUpResult) {
			this.syncDownResult = syncDownResult;
			this.syncUpResult = syncUpResult;
		}

		public DownOperationResult getSyncDownResult() {
			return syncDownResult;
		}		
		
		public UpOperationResult getSyncUpResult() {
			return syncUpResult;
		}				
	}
}
