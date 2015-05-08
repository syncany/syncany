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
package org.syncany.tests.integration.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.syncany.operations.update.UpdateOperation;
import org.syncany.operations.update.UpdateOperationAction;
import org.syncany.operations.update.UpdateOperationOptions;
import org.syncany.operations.update.UpdateOperationResult;
import org.syncany.operations.update.UpdateOperationResult.UpdateResultCode;

public class UpdateOperationTest {
	@Test
	public void testUpdateCheck() throws Exception {
		UpdateOperationOptions updateOptions = new UpdateOperationOptions();
		updateOptions.setAction(UpdateOperationAction.CHECK);

		// Run
		UpdateOperationResult updateResult = new UpdateOperation(null, updateOptions).execute();

		// Test
		assertNotNull(updateResult);
		assertEquals(UpdateResultCode.OK, updateResult.getResultCode());
		assertEquals(UpdateOperationAction.CHECK, updateResult.getAction());
		
		assertNotNull(updateResult.getAppInfo());
		assertNotNull(updateResult.getAppInfo().getAppVersion());
		assertNotNull(updateResult.getAppInfo().getArchitecture());
		assertNotNull(updateResult.getAppInfo().getDate());
		assertNotNull(updateResult.getAppInfo().getDist());
		assertNotNull(updateResult.getAppInfo().getDownloadUrl());
		assertNotNull(updateResult.getAppInfo().getOperatingSystem());
		assertNotNull(updateResult.getAppInfo().getType());		
	}
}
