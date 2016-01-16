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
package org.syncany.tests.integration.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.syncany.plugins.dummy.DummyTransferSettings;
import org.syncany.plugins.transfer.NestedTransferPluginOption;
import org.syncany.plugins.transfer.TransferPluginOption;
import org.syncany.plugins.transfer.TransferPluginOptions;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.util.ReflectionUtil;

public class PluginOptionsTest {

	@Test
	@Ignore
	// TODO rewrite to make this test work again with generic nested transfer settings
	public void nestedSettingsTest() throws Exception {
		DummyTransferSettings dts = new DummyTransferSettings();

		for (TransferPluginOption option : TransferPluginOptions.getOrderedOptions(DummyTransferSettings.class)) {
			askNestedPluginSettings(dts, option, 0);
		}

		assertNotNull(dts.baz);
		assertNotNull(dts.foo);
		assertNotNull(dts.number);
		assertNotNull(dts.subsettings);
	}

	private void askNestedPluginSettings(TransferSettings settings, TransferPluginOption option, int wrap) throws Exception {

		if (option instanceof NestedTransferPluginOption) {
			assertNotNull(ReflectionUtil.getClassFromType(option.getType()));
			System.out.println(new String(new char[wrap]).replace("\0", "\t") + ReflectionUtil.getClassFromType(option.getType()) + "#"
					+ option.getField().getName() + " (nested)");
			TransferSettings nestedSettings = (TransferSettings) ReflectionUtil.getClassFromType(option.getType()).newInstance();
			settings.setField(option.getField().getName(), nestedSettings);

			for (TransferPluginOption nItem : ((NestedTransferPluginOption) option).getOptions()) {
				askNestedPluginSettings(nestedSettings, nItem, ++wrap);
			}
		}
		else {
			System.out.println(new String(new char[wrap]).replace("\0", "\t") + settings.getClass() + "#" + option.getField().getName());
			settings.setField(option.getField().getName(), String.valueOf(settings.hashCode()));
		}
	}

	@Test
	@Ignore
	public void testOrderingOfOptions() throws Exception {
		final String[] expectedOrder = new String[] { "foo", "number", "baz", "nest" };
		List<TransferPluginOption> items = TransferPluginOptions.getOrderedOptions(DummyTransferSettings.class);

		int i = 0;
		for (TransferPluginOption item : items) {
			assertEquals(expectedOrder[i++], item.getName());
		}
	}

}
