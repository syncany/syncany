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
package org.syncany.operations.log;

import org.simpleframework.xml.Element;
import org.syncany.operations.OperationOptions;

public class LogOperationOptions implements OperationOptions {
	@Element(required = false)
	private int maxDatabaseVersionCount;
	
	@Element(required = false)
	private int startDatabaseVersionIndex;
	
	@Element(required = false)
	private int maxFileHistoryCount;

	public LogOperationOptions() {
		this.maxDatabaseVersionCount = 10;
		this.startDatabaseVersionIndex = 0;
		this.maxFileHistoryCount = 100;
	}

	public int getMaxDatabaseVersionCount() {
		return maxDatabaseVersionCount;
	}

	public void setMaxDatabaseVersionCount(int maxDatabaseVersionCount) {
		this.maxDatabaseVersionCount = maxDatabaseVersionCount;
	}

	public int getStartDatabaseVersionIndex() {
		return startDatabaseVersionIndex;
	}

	public void setStartDatabaseVersionIndex(int startDatabaseVersionIndex) {
		this.startDatabaseVersionIndex = startDatabaseVersionIndex;
	}

	public int getMaxFileHistoryCount() {
		return maxFileHistoryCount;
	}

	public void setMaxFileHistoryCount(int maxFileHistoryCount) {
		this.maxFileHistoryCount = maxFileHistoryCount;
	}
}
