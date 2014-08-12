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

import org.syncany.operations.OperationResult;
import org.syncany.plugins.StorageTestResult;

public class InitOperationResult implements OperationResult {
	public enum InitResultCode {
		OK, NOK_TEST_FAILED
	}

	private InitResultCode resultCode = InitResultCode.OK;
	private StorageTestResult testResult = null;
	private GenlinkOperationResult genLinkResult = null;

	public InitOperationResult(InitResultCode resultCode, StorageTestResult testResult) {
		this.resultCode = resultCode;
		this.testResult = testResult;
	}

	public InitOperationResult(InitResultCode resultCode, GenlinkOperationResult genLinkResult) {
		this.resultCode = resultCode;
		this.genLinkResult = genLinkResult;
	}

	public InitResultCode getResultCode() {
		return resultCode;
	}

	public GenlinkOperationResult getGenLinkResult() {
		return genLinkResult;
	}

	public StorageTestResult getTestResult() {
		return testResult;
	}
}
