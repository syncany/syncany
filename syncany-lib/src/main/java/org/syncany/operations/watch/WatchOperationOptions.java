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
package org.syncany.operations.watch;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.syncany.operations.OperationOptions;
import org.syncany.operations.cleanup.CleanupOperationOptions;
import org.syncany.operations.down.DownOperationOptions;
import org.syncany.operations.up.UpOperationOptions;

/**
 * The watch operation options represent the configuration parameters
 * of the {@link WatchOperation}. They are used to alter the behavior of the
 * operation, change interval times and enable/disable certain behaviors.
 * 
 * <p>The options can also be stored as XML within the daemon configuration.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
@Root(name="watch")
public class WatchOperationOptions implements OperationOptions {
	@Element(required = false)
	private int interval = 2*60*1000;
	
	@Element(required = false)
	private boolean announcements = true;
	
	@Element(required = false)
	private String announcementsHost = "notify.syncany.org";
	
	@Element(required = false)
	private int announcementsPort = 8080;
	
	@Element(required = false)
	private int settleDelay = 3000;
	
	@Element(required = false)
	private int cleanupInterval = 1*60*60*1000;
	
	@Element(required = false)
	private boolean watcher = true;
	
	@Element(name = "up", required = false) 
	private UpOperationOptions upOptions = new UpOperationOptions();
	
	@Element(name = "down", required = false)
	private DownOperationOptions downOptions = new DownOperationOptions();
	
	@Element(name = "clean", required = false) 
	private CleanupOperationOptions cleanupOptions = new CleanupOperationOptions();
	
	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public boolean announcementsEnabled() {
		return announcements;
	}

	public void setAnnouncements(boolean announcements) {
		this.announcements = announcements;
	}

	public String getAnnouncementsHost() {
		return announcementsHost;
	}

	public void setAnnouncementsHost(String announcementsHost) {
		this.announcementsHost = announcementsHost;
	}

	public int getAnnouncementsPort() {
		return announcementsPort;
	}

	public void setAnnouncementsPort(int announcementsPort) {
		this.announcementsPort = announcementsPort;
	}

	public int getSettleDelay() {
		return settleDelay;
	}

	public void setSettleDelay(int settleDelay) {
		this.settleDelay = settleDelay;
	}

	public boolean watcherEnabled() {
		return watcher;
	}

	public void setWatcher(boolean watcher) {
		this.watcher = watcher;
	}

	public int getCleanupInterval() {
		return cleanupInterval;
	}

	public void setCleanupInterval(int cleanupInterval) {
		this.cleanupInterval = cleanupInterval;
	}

	public UpOperationOptions getUpOptions() {
		return upOptions;
	}

	public void setUpOptions(UpOperationOptions upOptions) {
		this.upOptions = upOptions;
	}

	public CleanupOperationOptions getCleanupOptions() {
		return cleanupOptions;
	}

	public void setCleanupOptions(CleanupOperationOptions cleanupOptions) {
		this.cleanupOptions = cleanupOptions;
	}

	public DownOperationOptions getDownOptions() {
		return downOptions;
	}

	public void setDownOptions(DownOperationOptions downOptions) {
		this.downOptions = downOptions;
	}
}