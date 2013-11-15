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
package org.syncany;

import java.util.List;

import org.syncany.config.Config;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.operations.ConnectOperation;
import org.syncany.operations.ConnectOperation.ConnectOperationListener;
import org.syncany.operations.ConnectOperation.ConnectOperationOptions;
import org.syncany.operations.ConnectOperation.ConnectOperationResult;
import org.syncany.operations.DownOperation;
import org.syncany.operations.DownOperation.DownOperationOptions;
import org.syncany.operations.DownOperation.DownOperationResult;
import org.syncany.operations.InitOperation;
import org.syncany.operations.InitOperation.InitOperationOptions;
import org.syncany.operations.InitOperation.InitOperationResult;
import org.syncany.operations.LogOperation;
import org.syncany.operations.LogOperation.LogOperationOptions;
import org.syncany.operations.LogOperation.LogOperationResult;
import org.syncany.operations.LsRemoteOperation;
import org.syncany.operations.LsRemoteOperation.RemoteStatusOperationResult;
import org.syncany.operations.Operation;
import org.syncany.operations.OperationOptions;
import org.syncany.operations.OperationResult;
import org.syncany.operations.RestoreOperation;
import org.syncany.operations.RestoreOperation.RestoreOperationOptions;
import org.syncany.operations.RestoreOperation.RestoreOperationResult;
import org.syncany.operations.StatusOperation;
import org.syncany.operations.StatusOperation.ChangeSet;
import org.syncany.operations.StatusOperation.StatusOperationOptions;
import org.syncany.operations.StatusOperation.StatusOperationResult;
import org.syncany.operations.SyncOperation;
import org.syncany.operations.SyncOperation.SyncOperationOptions;
import org.syncany.operations.SyncOperation.SyncOperationResult;
import org.syncany.operations.UpOperation;
import org.syncany.operations.UpOperation.UpOperationOptions;
import org.syncany.operations.UpOperation.UpOperationResult;
import org.syncany.operations.WatchOperation;
import org.syncany.operations.WatchOperation.WatchOperationOptions;

/**
 * The client class is a convenience class to call the application's {@link Operation}s
 * using a central entry point. The class offers wrapper methods around the operations.
 * 
 * <p>The methods typically take an {@link OperationOptions} instance as an argument, 
 * and return an instance of the {@link OperationResult} class.
 *  
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Client {
	protected Config config;
	
	public Client() {
		// Fressen
	}
	
	public void setConfig(Config config) {
		this.config = config;
	}
	
	public Config getConfig() {
		return config;
	}

	public UpOperationResult up() throws Exception {
		return up(new UpOperationOptions());
	}
	
	public UpOperationResult up(UpOperationOptions options) throws Exception {
		return (UpOperationResult) new UpOperation(config, null, options).execute();
	}
	
	public DownOperationResult down() throws Exception {
		return down(new DownOperationOptions());
	}
	
	public DownOperationResult down(DownOperationOptions options) throws Exception {
		return (DownOperationResult) new DownOperation(config, null, options).execute();
	}
	
	public SyncOperationResult sync() throws Exception {
		return sync(new SyncOperationOptions());
	}
	
	public SyncOperationResult sync(SyncOperationOptions options) throws Exception {
		return (SyncOperationResult) new SyncOperation(config, null, options).execute();
	}

	public ChangeSet status() throws Exception {
		return status(new StatusOperationOptions());		
	}
	
	public ChangeSet status(StatusOperationOptions options) throws Exception {
		return ((StatusOperationResult) new StatusOperation(config, null, options).execute()).getChangeSet();		
	}	

	public List<RemoteFile> remoteStatus() throws Exception {
		return ((RemoteStatusOperationResult) new LsRemoteOperation(config).execute()).getUnknownRemoteDatabases();
	}

	public RestoreOperationResult restore(RestoreOperationOptions options) throws Exception {
		return (RestoreOperationResult) new RestoreOperation(config, options).execute();		
	}
	
	public LogOperationResult log(LogOperationOptions options) throws Exception {
		return (LogOperationResult) new LogOperation(config, options).execute();		
	}

	public void watch(WatchOperationOptions options) throws Exception {
		new WatchOperation(config, options).execute();		
	}	
	
	public InitOperationResult init(InitOperationOptions options) throws Exception {
        return (InitOperationResult) new InitOperation(options).execute();                
	}
	
	public ConnectOperationResult connect(ConnectOperationOptions options) throws Exception {
		return connect(options, null);
	}
	
	public ConnectOperationResult connect(ConnectOperationOptions options, ConnectOperationListener listener) throws Exception {
        return (ConnectOperationResult) new ConnectOperation(options, listener).execute();                
	}
}
