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
package org.syncany.operations.daemon;

import java.util.ArrayList;

import org.syncany.config.to.FolderTO;
import org.syncany.operations.OperationResult;

public class DaemonOperationResult implements OperationResult {
	public enum DaemonResultCode {
		OK, OK_PARTIAL, NOK, NOK_PARTIAL
	}
	
	private DaemonResultCode resultCode;
	private ArrayList<FolderTO> watchList;
	
	public DaemonOperationResult() {
		// Nothing
	}
	
	public DaemonOperationResult(DaemonResultCode resultCode) {
		this.resultCode = resultCode;
	}
	
	public DaemonOperationResult(DaemonResultCode resultCode, ArrayList<FolderTO> watchList) {
		this.resultCode = resultCode;
		this.watchList = watchList;
	}

	public DaemonResultCode getResultCode() {
		return resultCode;
	}
	
	public void setResultCode(DaemonResultCode resultCode) {
		this.resultCode = resultCode;
	}
	
	public ArrayList<FolderTO> getWatchList() {
		return watchList;
	}
	
	public void setWatchList(ArrayList<FolderTO> watchList) {
		this.watchList = watchList;
	}
}
