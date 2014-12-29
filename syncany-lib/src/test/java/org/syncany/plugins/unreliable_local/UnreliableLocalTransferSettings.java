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
package org.syncany.plugins.unreliable_local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.simpleframework.xml.ElementList;
import org.syncany.plugins.local.LocalTransferSettings;

public class UnreliableLocalTransferSettings extends LocalTransferSettings {
	@ElementList(required = false)
	private List<String> failingOperationPatterns;

	private int totalOperationCounter;
	private Map<String, Integer> typeOperationCounters;

	public UnreliableLocalTransferSettings() {
		super();

		this.totalOperationCounter = 0;
		this.typeOperationCounters = new HashMap<String, Integer>();
		this.failingOperationPatterns = new ArrayList<String>();
	}

	public List<String> getFailingOperationPatterns() {
		return failingOperationPatterns;
	}

	public void setFailingOperationPatterns(List<String> failingOperationPatterns) {
		this.failingOperationPatterns = failingOperationPatterns;
	}

	public int getTotalOperationCounter() {
		return totalOperationCounter;
	}

	public void setTotalOperationCounter(int totalOperationCounter) {
		this.totalOperationCounter = totalOperationCounter;
	}

	public Map<String, Integer> getTypeOperationCounters() {
		return typeOperationCounters;
	}

	public void setTypeOperationCounters(Map<String, Integer> typeOperationCounters) {
		this.typeOperationCounters = typeOperationCounters;
	}

	public void increaseTotalOperationCounter() {
		totalOperationCounter++;
	}
}
