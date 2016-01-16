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
package org.syncany.tests.unit.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.junit.Ignore;
import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.config.ConfigException;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.RepoTO;
import org.syncany.config.to.RepoTO.TransformerTO;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.tests.util.TestAssertUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.util.StringUtil;

public class ConfigTest {
	@Test
	public void testConfigValid() throws Exception {
		// Setup
		File localDir = new File("/some/folder");
		ConfigTO configTO = new ConfigTO();
		RepoTO repoTO = new RepoTO();

		configTO.setMachineName("somevalidmachinename"); // <<< valid

		repoTO.setChunkerTO(TestConfigUtil.createFixedChunkerTO()); // <<< valid
		repoTO.setMultiChunker(TestConfigUtil.createZipMultiChunkerTO()); // <<< valid
		repoTO.setRepoId(new byte[] { 0x01, 0x02 }); // <<< valid
		repoTO.setTransformers(null); // <<< valid

		// Run!
		Config config = new Config(localDir, configTO, repoTO);

		// Test
		assertEquals("/some/folder/.syncany", config.getAppDir().getAbsolutePath());
		assertEquals("/some/folder/.syncany/cache", config.getCacheDir().getAbsolutePath());
		assertEquals("/some/folder/.syncany/db", config.getDatabaseDir().getAbsolutePath());
		assertEquals("/some/folder/.syncany/db/local.db", config.getDatabaseFile().getAbsolutePath());

		assertNotNull(config.getChunker());
		assertEquals("FixedChunker", config.getChunker().getClass().getSimpleName());
		assertEquals("SHA1", config.getChunker().getChecksumAlgorithm());

		assertNotNull(config.getMultiChunker());
		assertEquals("ZipMultiChunker", config.getMultiChunker().getClass().getSimpleName());

		assertNotNull(config.getTransformer());
		assertEquals("NoTransformer", config.getTransformer().getClass().getSimpleName());

		assertNotNull(config.getCache());
	}

	@Test(expected = ConfigException.class)
	public void testConfigInitLocalDirNull() throws Exception {
		File localDir = null;
		ConfigTO configTO = new ConfigTO();
		RepoTO repoTO = new RepoTO();

		new Config(localDir, configTO, repoTO);
	}

	@Test(expected = ConfigException.class)
	public void testConfigInitConfigTONull() throws Exception {
		File localDir = new File("/some/folder");
		ConfigTO configTO = null;
		RepoTO repoTO = new RepoTO();

		new Config(localDir, configTO, repoTO);
	}

	@Test(expected = ConfigException.class)
	public void testConfigInitRepoTONull() throws Exception {
		File localDir = new File("/some/folder");
		ConfigTO configTO = new ConfigTO();
		RepoTO repoTO = null;

		new Config(localDir, configTO, repoTO);
	}

	@Test
	public void testConfigMachineNameInvalidChars() throws Exception {
		File localDir = new File("/some/folder");
		ConfigTO configTO = new ConfigTO();
		RepoTO repoTO = new RepoTO();

		configTO.setMachineName("invalid machine name");

		// Run!
		try {
			new Config(localDir, configTO, repoTO);
			fail("Machine name should not have been accepted.");
		}
		catch (ConfigException e) {
			TestAssertUtil.assertErrorStackTraceContains("Machine name", e);
		}
	}

	@Test
	public void testConfigMachineNameInvalidNull() throws Exception {
		File localDir = new File("/some/folder");
		ConfigTO configTO = new ConfigTO();
		RepoTO repoTO = new RepoTO();

		configTO.setMachineName(null); // <<< Invalid

		// Run!
		try {
			new Config(localDir, configTO, repoTO);
			fail("Machine name should not have been accepted.");
		}
		catch (ConfigException e) {
			TestAssertUtil.assertErrorStackTraceContains("Machine name", e);
		}
	}

	@Test
	public void testConfigMultiChunkerNull() throws Exception {
		// Setup
		File localDir = new File("/some/folder");
		ConfigTO configTO = new ConfigTO();
		RepoTO repoTO = new RepoTO();

		configTO.setMachineName("somevalidmachinename"); // <<< valid

		repoTO.setChunkerTO(TestConfigUtil.createFixedChunkerTO()); // <<< valid
		repoTO.setRepoId(new byte[] { 0x01, 0x02 }); // <<< valid
		repoTO.setTransformers(null); // <<< valid

		repoTO.setMultiChunker(null); // <<< INVALID !!

		// Run!
		try {
			new Config(localDir, configTO, repoTO);
			fail("Config should not been have initialized.");
		}
		catch (ConfigException e) {
			TestAssertUtil.assertErrorStackTraceContains("No multichunker", e);
		}
	}

	@Test
	@Ignore
	// TODO [low] ChunkerTO is not used yet; so no test for it.
	public void testConfigChunkerNull() throws Exception {
		// Setup
		File localDir = new File("/some/folder");
		ConfigTO configTO = new ConfigTO();
		RepoTO repoTO = new RepoTO();

		configTO.setMachineName("somevalidmachinename"); // <<< valid

		repoTO.setMultiChunker(TestConfigUtil.createZipMultiChunkerTO()); // <<< valid
		repoTO.setRepoId(new byte[] { 0x01, 0x02 }); // <<< valid
		repoTO.setTransformers(null); // <<< valid

		repoTO.setChunkerTO(null); // <<< INVALID !!

		// Run!
		try {
			new Config(localDir, configTO, repoTO);
			fail("Config should not been have initialized.");
		}
		catch (ConfigException e) {
			TestAssertUtil.assertErrorStackTraceContains("No multichunker", e);
		}
	}

	@Test
	public void testConfigCipherTransformersInvalidType() throws Exception {
		// Setup
		File localDir = new File("/some/folder");
		ConfigTO configTO = new ConfigTO();
		RepoTO repoTO = new RepoTO();

		configTO.setMachineName("somevalidmachinename"); // <<< valid

		repoTO.setChunkerTO(TestConfigUtil.createFixedChunkerTO()); // <<< valid
		repoTO.setMultiChunker(TestConfigUtil.createZipMultiChunkerTO()); // <<< valid
		repoTO.setRepoId(new byte[] { 0x01, 0x02 }); // <<< valid

		// Set invalid transformer
		TransformerTO invalidTransformerTO = new TransformerTO();
		invalidTransformerTO.setType("invalid-typeXXX");
		invalidTransformerTO.setSettings(new HashMap<String, String>());

		List<TransformerTO> transformers = new ArrayList<TransformerTO>();
		transformers.add(invalidTransformerTO);

		repoTO.setTransformers(transformers); // <<< INVALID !

		// Run!
		try {
			new Config(localDir, configTO, repoTO);
			fail("Transformer should NOT have been found.");
		}
		catch (ConfigException e) {
			TestAssertUtil.assertErrorStackTraceContains("invalid-typeXXX", e);
		}
	}

	@Test
	@SuppressWarnings("serial")
	public void testConfigCipherTransformersCipherFound() throws Exception {
		// Setup
		File localDir = new File("/some/folder");
		ConfigTO configTO = new ConfigTO();
		RepoTO repoTO = new RepoTO();

		configTO.setMachineName("somevalidmachinename"); // <<< valid

		repoTO.setChunkerTO(TestConfigUtil.createFixedChunkerTO()); // <<< valid
		repoTO.setMultiChunker(TestConfigUtil.createZipMultiChunkerTO()); // <<< valid
		repoTO.setRepoId(new byte[] { 0x01, 0x02 }); // <<< valid
		configTO.setMasterKey(createDummyMasterKey()); // <<< valid

		// Set invalid transformer
		TransformerTO invalidTransformerTO = new TransformerTO();
		invalidTransformerTO.setType("cipher");
		invalidTransformerTO.setSettings(new HashMap<String, String>() {
			{
				put("cipherspecs", "1,2");
			}
		});

		List<TransformerTO> transformers = new ArrayList<TransformerTO>();
		transformers.add(invalidTransformerTO);

		repoTO.setTransformers(transformers); // <<< valid

		// Run!
		Config config = new Config(localDir, configTO, repoTO);

		// Test
		assertNotNull(config.getChunker());
		assertNotNull(config.getMultiChunker());
		assertNotNull(config.getTransformer());
		assertEquals("CipherTransformer", config.getTransformer().getClass().getSimpleName());
	}

	@Test
	@SuppressWarnings("serial")
	public void testConfigCipherTransformersCipherNotFound() throws Exception {
		// Setup
		File localDir = new File("/some/folder");
		ConfigTO configTO = new ConfigTO();
		RepoTO repoTO = new RepoTO();

		configTO.setMachineName("somevalidmachinename"); // <<< valid

		repoTO.setChunkerTO(TestConfigUtil.createFixedChunkerTO()); // <<< valid
		repoTO.setMultiChunker(TestConfigUtil.createZipMultiChunkerTO()); // <<< valid
		repoTO.setRepoId(new byte[] { 0x01, 0x02 }); // <<< valid
		configTO.setMasterKey(createDummyMasterKey()); // <<< valid

		// Set invalid transformer
		TransformerTO invalidTransformerTO = new TransformerTO();
		invalidTransformerTO.setType("cipher");
		invalidTransformerTO.setSettings(new HashMap<String, String>() {
			{
				put("cipherspecs", "1,INVALIDXXXX"); // <<<< INVALID !
			}
		});

		List<TransformerTO> transformers = new ArrayList<TransformerTO>();
		transformers.add(invalidTransformerTO);

		repoTO.setTransformers(transformers);

		// Run!
		try {
			new Config(localDir, configTO, repoTO);
			fail("Transformer should NOT have been able to initialize.");
		}
		catch (ConfigException e) {
			TestAssertUtil.assertErrorStackTraceContains("INVALIDXXXX", e);
		}
	}

	private SaltedSecretKey createDummyMasterKey() {
		return new SaltedSecretKey(
				new SecretKeySpec(
						StringUtil.fromHex("44fda24d53b29828b62c362529bd9df5c8a92c2736bcae3a28b3d7b44488e36e246106aa5334813028abb2048eeb5e177df1c702d93cf82aeb7b6d59a8534ff0"),
						"AnyAlgorithm"
						),
						StringUtil
						.fromHex("157599349e0f1bc713afff442db9d4c3201324073d51cb33407600f305500aa3fdb31136cb1f37bd51a48f183844257d42010a36133b32b424dd02bc63b349bc"));
	}
}
