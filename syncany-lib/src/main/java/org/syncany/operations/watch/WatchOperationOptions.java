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

import org.syncany.operations.OperationOptions;

public class WatchOperationOptions implements OperationOptions {
	private int interval = 2*60*1000;
	private boolean announcements = true;
	private String announcementsHost = "notify.syncany.org";
	private int announcementsPort = 8080;
	private int settleDelay = 3000;
	private int cleanupInterval = 1*60*60*1000;
	private boolean watcher = true;

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
}