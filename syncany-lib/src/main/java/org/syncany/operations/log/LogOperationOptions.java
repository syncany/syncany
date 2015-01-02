/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
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

import org.simpleframework.xml.Element;
import org.syncany.operations.OperationOptions;

public class LogOperationOptions implements OperationOptions {
	@Element(required = false)
	private int maxCount;
	
	@Element(required = false)
	private boolean excludeChunkData;

	public LogOperationOptions() {
		this.maxCount = 10;
		this.excludeChunkData = true;
	}

	public int getMaxCount() {
		return maxCount;
	}

	public void setMaxCount(int maxCount) {
		this.maxCount = maxCount;
	}

	public boolean isExcludeChunkData() {
		return excludeChunkData;
	}

	public void setExcludeChunkData(boolean excludeChunkData) {
		this.excludeChunkData = excludeChunkData;
	}
}
