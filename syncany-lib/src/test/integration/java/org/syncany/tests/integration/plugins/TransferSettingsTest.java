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
package org.syncany.tests.integration.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.ElementException;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config;
import org.syncany.config.to.ConfigTO;
import org.syncany.operations.init.InitOperationOptions;
import org.syncany.plugins.Plugins;
import org.syncany.plugins.dummy.DummyTransferManager;
import org.syncany.plugins.dummy.DummyTransferPlugin;
import org.syncany.plugins.dummy.DummyTransferSettings;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.TransferPluginUtil;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestConfigUtil;

public class TransferSettingsTest {
	private File tmpFile;
	private Config config;

	@Before
	public void before() throws Exception {
		tmpFile = File.createTempFile("syncany-transfer-settings-test", "tmp");
		config = TestConfigUtil.createTestLocalConfig();
		assertNotNull(Plugins.get("dummy"));
		assertNotNull(config);
	}

	@After
	public void after() throws Exception {
		tmpFile.delete();
		FileUtils.deleteDirectory(((LocalTransferSettings) config.getConnection()).getPath());
		FileUtils.deleteDirectory(config.getLocalDir());
		config = null;
	}

	@Test
	public void testRestore() throws Exception {

		final String fooTest = "foo-test";
		final String bazTest = "baz";
		final int numberTest = 1234;

		final DummyTransferSettings ts = new DummyTransferSettings();
		final LocalTransferSettings lts = new LocalTransferSettings();
		final InitOperationOptions initOperationOptions = TestConfigUtil.createTestInitOperationOptions("syncanytest");
		final ConfigTO conf = initOperationOptions.getConfigTO();

		File repoDir = ((LocalTransferSettings) initOperationOptions.getConfigTO().getTransferSettings()).getPath();
		File localDir = initOperationOptions.getLocalDir();

		conf.setTransferSettings(ts);

		ts.foo = fooTest;
		ts.baz = bazTest;
		ts.number = numberTest;
		lts.setPath(File.createTempFile("aaa", "bbb"));
		ts.subsettings = lts;

		assertTrue(ts.isValid());
		Serializer serializer = new Persister();
		serializer.write(conf, tmpFile);

		System.out.println(new String(Files.readAllBytes(Paths.get(tmpFile.toURI()))));

		ConfigTO confRestored = ConfigTO.load(tmpFile);
		TransferPlugin plugin = Plugins.get(confRestored.getTransferSettings().getType(), TransferPlugin.class);
		assertNotNull(plugin);

		TransferSettings tsRestored = confRestored.getTransferSettings();
		assertNotNull(tsRestored);

		DummyTransferManager transferManager = plugin.createTransferManager(tsRestored, config);
		assertNotNull(transferManager);

		// Tear down
		FileUtils.deleteDirectory(localDir);
		FileUtils.deleteDirectory(repoDir);
	}

	@Test
	public void createNewValidConnectionTO() throws Exception {
		TransferPlugin p = Plugins.get("dummy", TransferPlugin.class);
		DummyTransferSettings ts = p.createEmptySettings();
		ts.foo = "foo-value";
		ts.number = 5;

		assertTrue(ts.isValid());
	}

	@Test
	public void createNewInvalidConnectionTO() throws Exception {
		TransferPlugin p = Plugins.get("dummy", TransferPlugin.class);
		DummyTransferSettings ts = p.createEmptySettings();

		assertFalse(ts.isValid());
	}

	@Test
	public void testDeserializeCorrectClass() throws Exception {
		Serializer serializer = new Persister();
		InitOperationOptions initOperationOptions = TestConfigUtil.createTestInitOperationOptions("syncanytest");
		// Always LocalTransferSettings
		serializer.write(initOperationOptions.getConfigTO(), tmpFile);

		ConfigTO confRestored = ConfigTO.load(tmpFile);

		assertEquals(LocalTransferSettings.class, confRestored.getTransferSettings().getClass());

		// Tear down
		FileUtils.deleteDirectory(initOperationOptions.getLocalDir());
		FileUtils.deleteDirectory(((LocalTransferSettings) initOperationOptions.getConfigTO().getTransferSettings()).getPath());
	}

	@Test(expected = ElementException.class)
	public void testDeserializeWrongClass() throws Exception {

		LocalTransferSettings lts = new LocalTransferSettings();
		lts.setPath(tmpFile);

		Serializer serializer = new Persister();
		serializer.write(lts, tmpFile);

		// This shouldn't blow up!
		serializer.read(DummyTransferSettings.class, tmpFile);
	}

	@Test
	public void testGetSettingsAndManagerFromPlugin() throws Exception {
		Class<? extends TransferSettings> settingsClass = TransferPluginUtil.getTransferSettingsClass(DummyTransferPlugin.class);
		assertEquals(DummyTransferSettings.class, settingsClass);
	}
}
