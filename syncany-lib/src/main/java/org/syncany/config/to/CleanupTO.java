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
package org.syncany.config.to;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * CleanupTO represents an xml file containing a single long value:
 * the timestamp of the moment at which a cleanup was last performed
 * by the relevant client.
 * 
 * @author Pim Otte
 */
@Root(name="cleanup", strict=false)
public class CleanupTO {
	@Element(name="lastTimeCleaned", required = false)
	private long lastTimeCleaned = 0;
	
	public long getLastTimeCleaned() {
		return lastTimeCleaned;
	}
	
	public void setLastTimeCleaned(long lastTimeCleanup) {
		this.lastTimeCleaned = lastTimeCleanup;
	}
}
