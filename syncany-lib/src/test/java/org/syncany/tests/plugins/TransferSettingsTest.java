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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.to.ConfigTO;
import org.syncany.plugins.PluginOptionSpecs;
import org.syncany.plugins.annotations.Encrypted;
import org.syncany.plugins.annotations.TransferPluginDefinition;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.TransferSettings;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class TransferSettingsTest {
	private File tmpFile;

	@Before
	public void before() throws Exception {
		tmpFile = File.createTempFile("syncany-transfer-settings-test", "tmp");
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

		ConfigTO confRestored = serializer.read(ConfigTO.class, tmpFile);
		DummyTransferSettings tsRestored = (DummyTransferSettings) confRestored.getConnectionTO();

		assertEquals(tsRestored.foo, fooTest);
		assertEquals(tsRestored.baz, bazTest);
		assertEquals(tsRestored.number, numberTest);

	}

	@TransferPluginDefinition(settings = DummyTransferSettings.class)
	public static class DummyTransferPlugin extends TransferPlugin {

		public DummyTransferPlugin() {
			super("test");
		}

		@Override
		public TransferSettings createSettings() {
			return null;
		}

		@Override
		public TransferManager createTransferManager(TransferSettings connection) {
			return null;
		}
	}

	public static class DummyTransferSettings extends TransferSettings {

		@Element(required = true)
		@Encrypted
		public String foo;

		@Element(name = "baz")
		public String baz;

		@Element(name = "number")
		public int number;

		@Element(name = "nest", required = false)
		public DummyTransferSettings subsettings;

		@Override
		public PluginOptionSpecs getOptionSpecs() {
			return null;
		}

	}

}
