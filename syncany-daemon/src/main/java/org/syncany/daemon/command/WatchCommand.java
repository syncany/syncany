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
package org.syncany.daemon.command;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config;
import org.syncany.config.Config.ConfigException;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.RepoTO;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.daemon.websocket.DaemonWebSocketServer;
import org.syncany.daemon.websocket.messages.DaemonWatchEvent;
import org.syncany.operations.WatchOperation;
import org.syncany.operations.WatchOperation.WatchOperationOptions;
import org.syncany.operations.listener.WatchOperationListener;

public class WatchCommand extends Command {
	public static final Pattern ANNOUNCEMENTS_PATTERN = Pattern.compile("([^:]+):(\\d+)");
	public static final int ANNOUNCEMENTS_PATTERN_GROUP_HOST = 1;
	public static final int ANNOUNCEMENTS_PATTERN_GROUP_PORT = 2;
	
	private String localFolder;
	private Integer interval;
	private boolean watcher;
	
	private AtomicBoolean started = new AtomicBoolean(false);
	private WatchOperation watchOperation;
	
	public WatchCommand(String localFolder, Integer interval, boolean watcher){
		this.localFolder = localFolder;
		this.interval = interval;
		this.watcher = watcher;
	}
	
	/**
	 * @return the localFolder
	 */
	public String getLocalFolder() {
		return localFolder;
	}
	
	public int execute() {
		setStatus(CommandStatus.STARTING);
		final WatchOperationOptions operationOptions = new WatchOperationOptions();
		
		// --interval
		if (interval != null) {
			operationOptions.setInterval(interval.intValue());
		}
		
		operationOptions.setWatcher(watcher);
		
		// Conflicting options: --no-announcements and --announce=<..>
//		if (options.has(optionNoAnnouncements) && options.has(optionAnnouncements)) {
//			throw new Exception("Options --no-announcements and --announce in conflict with one another.");
//		}
//		
//		// --no-announcements
//		if (options.has(optionNoAnnouncements)) {
//			operationOptions.setAnnouncements(false);
//		}
		
		// --announce=<host>:<port>
//		if (options.has(optionAnnouncements)) {
//			operationOptions.setAnnouncements(true);
//			
//			String announcementsStr = options.valueOf(optionAnnouncements);
//			Matcher matcher = ANNOUNCEMENTS_PATTERN.matcher(announcementsStr);
//			
//			if (!matcher.matches()) {
//				throw new Exception("Invalid argument for --announcements, expected pattern: "+ANNOUNCEMENTS_PATTERN.pattern());
//			}
//			
//			String announcementsHost = matcher.group(ANNOUNCEMENTS_PATTERN_GROUP_HOST);
//			int announcementsPort = Integer.parseInt(matcher.group(ANNOUNCEMENTS_PATTERN_GROUP_PORT));
//			
//			operationOptions.setAnnouncementsHost(announcementsHost);
//			operationOptions.setAnnouncementsPort(announcementsPort);
//		}
		
		// --delay=<sec>
//		if (options.has(optionSettleDelay)) {
//			operationOptions.setSettleDelay(options.valueOf(optionSettleDelay)*1000);
//		}
		
		// --no-watcher
//		if (options.has(optionNoWatcher)) {
//			operationOptions.setWatcher(false);
//		}

		// Run!
		final WatchOperationListener wl = new WatchOperationListener() {

			@Override
			public void batchIndexStart(int fileCount) {
				DaemonWatchEvent dwe = new DaemonWatchEvent();
				dwe.setAction("daemon_watch_event");
				//dwe.setEvent(event);
				DaemonWebSocketServer.sendToAll(dwe);
			}

			@Override
			public void batchIndexUpdate(String fileName, int fileNumber) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void batchUploadStart(int fileCount) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void batchUploadUpdate(String fileName, int fileNumber) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void batchDownloadStart(int fileCount) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void batchDownloadUpdate(String fileName, int fileNumber) {
				// TODO Auto-generated method stub
				
			}
		};
		
		new Thread(new Runnable() {
			public void run() {
				try {
					Config config = initConfigOption(localFolder);
					watchOperation = new WatchOperation(config, operationOptions, wl);
					setStatus(CommandStatus.SYNCING);
					started.set(true);
					watchOperation.execute();
					setStatus(CommandStatus.STOPPED);
				}
				catch (Exception e) {
					setStatus(CommandStatus.STOPPED);
				}
			}
		}, "watching "+ localFolder).start();
		
		return 0;
	}
	
	private Config initConfigOption(String localDir) throws ConfigException, Exception {
		// Load config
		File appDir = new File(localDir+"/" + Config.DIR_APPLICATION);
		
		if (appDir.exists()) {
			ConfigTO configTO = loadConfigTO(new File(localDir));
			RepoTO repoTO = loadRepoTO(new File(localDir), configTO);
			
			return new Config(new File(localDir), configTO, repoTO);
		}
		return null;		
	}		
	
	private ConfigTO loadConfigTO(File localDir) throws Exception {
		File configFile = new File(localDir+"/"+Config.DIR_APPLICATION+"/"+Config.FILE_CONFIG);
		
		if (!configFile.exists()) {
			throw new Exception("Cannot find config file at "+configFile+". Try connecting to a repository using 'connect', or 'init' to create a new one.");
		}
		
		return ConfigTO.load(configFile);
	}

	private RepoTO loadRepoTO(File localDir, ConfigTO configTO) throws Exception {
		File repoFile = new File(localDir+"/"+Config.DIR_APPLICATION+"/"+Config.FILE_REPO);
		
		if (!repoFile.exists()) {
			throw new Exception("Cannot find repository file at "+repoFile+". Try connecting to a repository using 'connect', or 'init' to create a new one.");
		}
		
		if (CipherUtil.isEncrypted(repoFile)) {
			SaltedSecretKey masterKey = configTO.getMasterKey();
			
			if (masterKey == null) {
				throw new Exception("Repo file is encrypted, but master key not set in config file.");
			}
			
			String repoFileStr = new String(CipherUtil.decrypt(new FileInputStream(repoFile), masterKey));
			
			Serializer serializer = new Persister();
			return serializer.read(RepoTO.class, repoFileStr);			
		}
		else {
			Serializer serializer = new Persister();
			return serializer.read(RepoTO.class, repoFile);
		}
	}

	public void pause() {
		watchOperation.pause();
		setStatus(CommandStatus.PAUSED);
	}
	
	public void resume() {
		watchOperation.resume();
		setStatus(CommandStatus.SYNCING);
	}
	
	public void stop() {
		watchOperation.stop();
		setStatus(CommandStatus.STOPPED);
	}

	@Override
	public void disposeCommand() {
		stop();
	}
}
