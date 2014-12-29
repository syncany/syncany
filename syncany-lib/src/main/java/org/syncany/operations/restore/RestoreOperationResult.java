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
package org.syncany.operations.restore;

import java.io.File;

import org.syncany.operations.OperationResult;

public class RestoreOperationResult implements OperationResult {
	public enum RestoreResultCode {
		ACK, NACK_INVALID_FILE, NACK_NO_FILE
	}
	
	private RestoreResultCode resultCode;
	private File targetFile;
	
	public RestoreOperationResult() {
		// Nothing.
	}
	
	public RestoreOperationResult(RestoreResultCode resultCode) {
		this(resultCode, null);
	}
	
	public RestoreOperationResult(RestoreResultCode resultCode, File targetFile) {
		this.resultCode = resultCode;
		this.targetFile = targetFile;
	}
	
	public RestoreResultCode getResultCode() {
		return resultCode;
	}
	
	public void setResultCode(RestoreResultCode resultCode) {
		this.resultCode = resultCode;
	}
	
	public File getTargetFile() {
		return targetFile;
	}
	
	public void setTargetFile(File targetFile) {
		this.targetFile = targetFile;
	}
}