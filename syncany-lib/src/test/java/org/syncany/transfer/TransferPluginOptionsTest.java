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
package org.syncany.transfer;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.simpleframework.xml.Element;
import org.syncany.plugins.dummy.DummyTransferSettings;
import org.syncany.plugins.transfer.Setup;
import org.syncany.plugins.transfer.TransferPluginOption;
import org.syncany.plugins.transfer.TransferPluginOptionCallback;
import org.syncany.plugins.transfer.TransferPluginOptions;
import org.syncany.plugins.transfer.TransferSettings;

public class TransferPluginOptionsTest {
	@Test
	public void testGetOrderedOptionsWithDummyPlugin() {
		List<TransferPluginOption> orderedOptions = TransferPluginOptions.getOrderedOptions(DummyTransferSettings.class);
		
		assertEquals(6, orderedOptions.size());
		assertEquals("foo", orderedOptions.get(0).getName());
		assertEquals("number", orderedOptions.get(1).getName());
		assertEquals("baz", orderedOptions.get(2).getName());
		assertEquals("nest", orderedOptions.get(3).getName());
		assertEquals("nest2", orderedOptions.get(4).getName());
		assertEquals("enumField", orderedOptions.get(5).getName());
	}
	
	@Test
	public void testGetOrderedOptionsWithAnotherDummyPlugin() {
		List<TransferPluginOption> orderedOptions = TransferPluginOptions.getOrderedOptions(AnotherDummyTransferSettings.class);
		
		assertEquals(4, orderedOptions.size());
		assertEquals("noSetupAnnotation", orderedOptions.get(0).getName());
		assertEquals("singularNonVisible", orderedOptions.get(1).getName());
		assertEquals("someCallback", orderedOptions.get(2).getName());		
		assertEquals("someInvalidCallback", orderedOptions.get(3).getName());		
	}
	
	public static class AnotherDummyTransferSettings extends TransferSettings {
		@Element(required = true)
		public String noSetupAnnotation;
		
		@Element(required = true)
		@Setup(singular = true, visible = false)
		public String singularNonVisible;
		
		@Element(required = true)
		@Setup(callback = TransferPluginOptionCallback.class)
		public String someCallback;
		
		@Element(required = true)
		@Setup(callback = InvalidTransferPluginOptionCallback.class)
		public String someInvalidCallback;		
	}

	public static class AnotherDummyTransferPluginOptionCallback implements TransferPluginOptionCallback {
		@Override
		public String preQueryCallback() {
			return "hi there";
		}

		@Override
		public String postQueryCallback(String optionValue) {
			return "bye there";
		}		
	}
	
	public static interface InvalidTransferPluginOptionCallback extends TransferPluginOptionCallback {
		// This should be a class
	}
}
