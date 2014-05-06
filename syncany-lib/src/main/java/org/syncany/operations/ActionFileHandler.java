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

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.connection.plugins.ActionRemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.operations.cleanup.CleanupOperation;

/**
 * The action handler manages the {@link ActionRemoteFile}s written during an {@link Operation}.
 * 
 * <p>In particular, it uploads an initial action file when the operation is started, deletes it
 * when it is finished/terminated, and renews the operation's action file in a given interval.
 * 
 * <p>The renewal is necessary to show other clients that the operation is still running. To ensure 
 * action file renewal, the {@link #start()} method starts a timer that uploads a new {@link ActionRemoteFile}
 * every {@link #ACTION_RENEWAL_INTERVAL} milliseconds. The {@link #finish()} method stops this timer.
 * 
 * @see CleanupOperation
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class ActionFileHandler {	
	private static final Logger logger = Logger.getLogger(ActionFileHandler.class.getSimpleName());

	/**
	 * Defines the time that the action files updated while an operation is running. 
	 * 
	 * This time period must be (significantly) smaller than the ignore time defined in 
	 * {@link CleanupOperation#ACTION_FILE_DELETE_TIME}.
	 */
	public static final int ACTION_RENEWAL_INTERVAL = 2*60*1000; // Minutes

	protected TransferManager transferManager;
	protected ActionRemoteFile actionFile;
	protected Timer actionRenewalTimer;

	public ActionFileHandler(TransferManager transferManager, String operationName, String machineName) {		
		try {
			this.transferManager = transferManager;
			this.actionFile = new ActionRemoteFile(operationName, machineName, System.currentTimeMillis());
			this.actionRenewalTimer = new Timer();			
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}

	public void start() throws Exception {
		logger.log(Level.INFO, "Starting action for " + actionFile + " ...");
		
		uploadActionFile(actionFile);
		scheduleActionRenewalTask();
	}

	public void finish() throws StorageException {
		logger.log(Level.INFO, "Finishing action for " + actionFile + " ...");
		
		cancelActionRenewalTask();
		deleteActionFile(actionFile);
	}

	private void deleteActionFile(ActionRemoteFile actionFile) throws StorageException {
		logger.log(Level.INFO, "Deleting action file: " + actionFile);
		transferManager.delete(actionFile);
	}

	private void uploadActionFile(ActionRemoteFile actionFile) throws Exception {
		logger.log(Level.INFO, "Uploading action file: " + actionFile);

		File tempActionFile = File.createTempFile("syncany-action-", ".tmp");
		tempActionFile.deleteOnExit();
		
		transferManager.upload(tempActionFile, actionFile);
		
		tempActionFile.delete();
	}
	
	private void scheduleActionRenewalTask() {
		logger.log(Level.INFO, "Scheduling action renewal task for every " + (ACTION_RENEWAL_INTERVAL/60/1000) + " minutes, for " + actionFile + " ...");
		
		actionRenewalTimer.schedule(new TimerTask() {			
			@Override
			public void run() {
				renewActionFile();				
			}
		}, ACTION_RENEWAL_INTERVAL, ACTION_RENEWAL_INTERVAL);
	}

	private void cancelActionRenewalTask() {
		actionRenewalTimer.cancel();
		actionRenewalTimer = new Timer();
	}
	
	private synchronized void renewActionFile() {
		try {
			logger.log(Level.INFO, "Scheduling action renewal task for every " + (ACTION_RENEWAL_INTERVAL/60/1000) + " minutes, for " + actionFile + " ...");

			ActionRemoteFile oldActionFile = actionFile;			
			ActionRemoteFile newActionFile = new ActionRemoteFile(oldActionFile.getOperationName(), oldActionFile.getClientName(), System.currentTimeMillis());
			
			uploadActionFile(newActionFile);
			deleteActionFile(oldActionFile);
			
			actionFile = newActionFile;
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "ERROR: Cannot renew action file!", e);
		}
	}
}
