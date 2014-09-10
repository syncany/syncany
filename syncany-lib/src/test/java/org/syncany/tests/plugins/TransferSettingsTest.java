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
package org.syncany.tests.plugins;

import com.google.common.collect.Maps;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.to.ConfigTO;
import org.syncany.plugins.Plugins;
import org.syncany.plugins.dummy.DummyTransferManager;
import org.syncany.plugins.dummy.DummyTransferSettings;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.TransferSettings;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.Assert.*;

public class TransferSettingsTest {
	private File tmpFile;

	@Before
	public void before() throws Exception {
		tmpFile = File.createTempFile("syncany-transfer-settings-test", "tmp");
		assertNotNull(Plugins.get("dummy"));
	}

	@After
	public void after() throws Exception {
		tmpFile.delete();
	}

	@Test
	public void testRestore() throws Exception {

		final String fooTest = "foo-test";
		final String bazTest = "baz-test";
		final int numberTest = 1234;

		final DummyTransferSettings ts = new DummyTransferSettings();
		final DummyTransferSettings nts = new DummyTransferSettings();
		final ConfigTO conf = new ConfigTO();

		conf.setConnectionTO(ts);
		conf.setMachineName("test");

		ts.foo = fooTest;
		ts.baz = bazTest;
		ts.number = numberTest;
		nts.foo = fooTest;
		nts.baz = bazTest;
		ts.subsettings = nts;

		Serializer serializer = new Persister();
		serializer.write(conf, tmpFile);

		System.out.println(new String(Files.readAllBytes(Paths.get(tmpFile.toURI()))));

		ConfigTO confRestored = ConfigTO.load(tmpFile);
		TransferPlugin plugin = Plugins.get(confRestored.getConnectionTO().getType(), TransferPlugin.class);

		TransferSettings tsRestored = (TransferSettings) confRestored.getConnectionTO();
		DummyTransferManager transferManager = plugin.createTransferManager(tsRestored);

		DummyTransferSettings dts = transferManager.getConnection();

		assertEquals(dts.foo, fooTest);
		assertEquals(dts.baz, bazTest);
		assertEquals(dts.number, numberTest);

	}

	@Test
	public void createNewConnectionTOfromMap() throws Exception {

		final Map<String, String> settings = Maps.newHashMap();
		settings.put("foo", "foo-value");
		settings.put("number", "5");

		TransferPlugin p = Plugins.get("dummy", TransferPlugin.class);
		TransferSettings ts = p.createEmptySettings();
		ts.parseKeyValueMap(settings);

    assertTrue(ts.isValid());

		DummyTransferManager dtm = p.createTransferManager(ts);
		DummyTransferSettings dts = dtm.getConnection();

		assertEquals(dts.foo, "foo-value");
		assertEquals(dts.number, 5);
		assertNull(dts.baz);

	}

}
