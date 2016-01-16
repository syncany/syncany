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
package org.syncany.operations.cleanup;

import java.util.SortedMap;
import java.util.TreeMap;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.syncany.operations.OperationOptions;
import org.syncany.operations.status.StatusOperationOptions;

public class CleanupOperationOptions implements OperationOptions {
	@Element(name = "status", required = false)
	private StatusOperationOptions statusOptions = new StatusOperationOptions();

	@Element(required = false)
	private boolean force = false;

	@Element(required = false)
	private boolean removeOldVersions = true;

	@Element(required = false)
	private boolean removeVersionsByInterval = true;

	@Element(required = false)
	private boolean removeUnreferencedTemporaryFiles = true;

	@Element(required = false)
	private long minKeepDeletedSeconds = 30 * 24 * 60 * 60; // 30 days

	@Element(required = false)
	private int maxDatabaseFiles = 15;

	@Element(required = false)
	private long minSecondsBetweenCleanups = 3 * 60 * 60; // 3 hours

	@ElementMap(entry = "fromTime", key = "truncateDateFormat", required = false, attribute = true, inline = true)
	private SortedMap<Long, TimeUnit> purgeFileVersionSettings;

	public CleanupOperationOptions() {
		purgeFileVersionSettings = createDefaultPurgeFileVersionSettings();	
	}

	public StatusOperationOptions getStatusOptions() {
		return statusOptions;
	}

	public void setStatusOptions(StatusOperationOptions statusOptions) {
		this.statusOptions = statusOptions;
	}

	public boolean isRemoveOldVersions() {
		return removeOldVersions;
	}

	public void setRemoveOldVersions(boolean removeOldVersions) {
		this.removeOldVersions = removeOldVersions;
	}

	public boolean isRemoveVersionsByInterval() {
		return removeVersionsByInterval;
	}

	public void setRemoveVersionsByInterval(boolean removeVersionsByInterval) {
		this.removeVersionsByInterval = removeVersionsByInterval;
	}

	public boolean isRemoveUnreferencedTemporaryFiles() {
		return removeUnreferencedTemporaryFiles;
	}

	public void setRemoveUnreferencedTemporaryFiles(boolean removeUnreferencedTemporaryFiles) {
		this.removeUnreferencedTemporaryFiles = removeUnreferencedTemporaryFiles;
	}

	public void setMaxDatabaseFiles(int maxDatabaseFiles) {
		this.maxDatabaseFiles = maxDatabaseFiles;
	}

	public int getMaxDatabaseFiles() {
		return maxDatabaseFiles;
	}

	public void setMinSecondsBetweenCleanups(long minSecondsBetweenCleanups) {
		this.minSecondsBetweenCleanups = minSecondsBetweenCleanups;
	}

	public long getMinSecondsBetweenCleanups() {
		return minSecondsBetweenCleanups;
	}

	public void setMinKeepSeconds(long minKeepDeletedSeconds) {
		this.minKeepDeletedSeconds = minKeepDeletedSeconds;
	}

	public long getMinKeepDeletedSeconds() {
		return minKeepDeletedSeconds;
	}
	
	public SortedMap<Long, TimeUnit> getPurgeFileVersionSettings() {
		return purgeFileVersionSettings;
	}

	public void setPurgeFileVersionSettings(SortedMap<Long, TimeUnit> newSettings) {
		purgeFileVersionSettings = newSettings;
	}

	public boolean isForce() {
		return force;
	}

	public void setForce(boolean force) {
		this.force = force;
	}
	
	/** 
	 * This function returns a Map which describes how to purge file versions.
	 * 
	 * Each key-value pair has a long and a string, representing the following:
	 * The string determines the behavior we use up to long seconds in the past.
	 * ie. If the first pair is (3600, TimeUnit.MINUTES), we keep 1 version every minute for the last hour.
	 * If the second pair is (3*24*3600, TimeUnit.HOURS), we keep 1 version every hour for the last three days,  
	 * except the last hour, for which the above policy holds.
	 */
	private SortedMap<Long, TimeUnit> createDefaultPurgeFileVersionSettings() {
		TreeMap<Long, TimeUnit> purgeSettings = new TreeMap<Long, TimeUnit>();

		purgeSettings.put(30L * 24L * 3600L, TimeUnit.DAYS);
		purgeSettings.put(3L * 24L * 3600L, TimeUnit.HOURS);
		purgeSettings.put(3600L, TimeUnit.MINUTES);
		
		return purgeSettings;
	}

	public enum TimeUnit {
		YEARS, MONTHS, WEEKS, DAYS, HOURS, MINUTES, SECONDS
	}
}