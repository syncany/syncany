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
package org.syncany.operations.init;

import org.syncany.connection.plugins.StorageTestResult;
import org.syncany.operations.OperationResult;

public class ConnectOperationResult implements OperationResult {
	public enum ConnectResultCode {
		OK, NOK_DECRYPT_ERROR, NOK_TEST_FAILED
	}

    private ConnectResultCode resultCode = ConnectResultCode.OK;
    private StorageTestResult testResult = null;

    public ConnectOperationResult(ConnectResultCode resultCode) {
		this.resultCode = resultCode;
	}
    
    public ConnectOperationResult(ConnectResultCode resultCode, StorageTestResult testResult) {
		this.resultCode = resultCode;
		this.testResult = testResult;
	}

	public ConnectResultCode getResultCode() {
		return resultCode;
	}

	public void setResultCode(ConnectResultCode resultCode) {
		this.resultCode = resultCode;
	}

	public StorageTestResult getTestResult() {
		return testResult;
	}                
}