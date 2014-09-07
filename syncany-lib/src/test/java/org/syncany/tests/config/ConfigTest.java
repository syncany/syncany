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
package org.syncany.tests.config;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.syncany.chunk.Chunker;
import org.syncany.chunk.FixedChunker;
import org.syncany.chunk.MimeTypeChunker;
import org.syncany.chunk.TttdChunker;
import org.syncany.config.Config;
import org.syncany.config.ConfigException;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.RepoTO;
import org.syncany.config.to.RepoTO.ChunkerTO;
import org.syncany.config.to.RepoTO.TransformerTO;
import org.syncany.tests.config.ConfigRule.ChunkerTOBuilder;
import org.syncany.tests.util.TestAssertUtil;
import org.syncany.tests.util.TestConfigUtil;

public class ConfigTest {
	
	@Rule
	public ConfigRule configRule = new ConfigRule();
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
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
		assertThat(config.getAppDir().getAbsolutePath(), is(absolutePathOf("/some/folder/.syncany")));
		assertThat(config.getCacheDir().getAbsolutePath(), is(absolutePathOf("/some/folder/.syncany/cache")));
		assertThat(config.getDatabaseDir().getAbsolutePath(), is(absolutePathOf("/some/folder/.syncany/db")));
		assertThat(config.getDatabaseFile().getAbsolutePath(), is(absolutePathOf("/some/folder/.syncany/db/local.db")));

		assertNotNull(config.getChunker());
		assertEquals("FixedChunker", config.getChunker().getClass().getSimpleName());
		assertEquals("SHA1", config.getChunker().getChecksumAlgorithm());
		
		assertNotNull(config.getMultiChunker());
		assertEquals("ZipMultiChunker", config.getMultiChunker().getClass().getSimpleName());

		assertNotNull(config.getTransformer());
		assertEquals("NoTransformer", config.getTransformer().getClass().getSimpleName());
		
		assertNotNull(config.getCache());
	}

	private String absolutePathOf(String path) {
		return Paths.get(path).toAbsolutePath().toString();
	}
	
	@Test
	public void initChunker_default(){
		Config config = configRule.getDefaultConfig();
		assertThat(config.getChunker(), is(instanceOf(FixedChunker.class)));
		assertThat(config.getChunker().getChecksumAlgorithm(), is("SHA1"));
	}
	
	@Test
	public void initChunker_customFixedChunker(){
		final ChunkerTO chunker = new ChunkerTOBuilder()
				.setType(FixedChunker.TYPE)
				.addProperty(Chunker.PROPERTY_SIZE, "4000")
				.build();
		Config config = configRule.getConfigWithCustomChunker(chunker);
		assertThat(config.getChunker(), is(instanceOf(FixedChunker.class)));
		assertThat(config.getChunker().getChecksumAlgorithm(), is("SHA1"));

	}
	
	@Test
	public void initChunker_customMimeTypeChunker(){
		final ChunkerTO chunker = new ChunkerTOBuilder()
				.setType(MimeTypeChunker.TYPE)
				.addProperty(MimeTypeChunker.PROPERTY_SPECIAL_CHUNKER_MIME_TYPES, "image/jpeg")
				.addNestedChunker(TestConfigUtil.createFixedChunkerTO())
				.addNestedChunker(TestConfigUtil.createFixedChunkerTO()).build();
		final Config config = configRule.getConfigWithCustomChunker(chunker);
		assertThat(config.getChunker(), is(instanceOf(MimeTypeChunker.class)));
		assertThat(config.getChunker().getChecksumAlgorithm(), is("SHA1"));
	}
	
	@Test
	public void initChunker_customMimeTypeChunker_multipleMimeTypes(){
		final ChunkerTO chunker = ChunkerTOBuilder.of(MimeTypeChunker.class)
		.setType(MimeTypeChunker.TYPE)
		.addProperty(MimeTypeChunker.PROPERTY_SPECIAL_CHUNKER_MIME_TYPES, "image/jpeg")
		.addProperty(MimeTypeChunker.PROPERTY_SPECIAL_CHUNKER_MIME_TYPES, "text/html")
		.addNestedChunker(TestConfigUtil.createFixedChunkerTO())
		.addNestedChunker(TestConfigUtil.createFixedChunkerTO()).build();
		final Config config = configRule.getConfigWithCustomChunker(chunker);
		assertThat(config.getChunker(), is(instanceOf(MimeTypeChunker.class)));
		assertThat(config.getChunker().getChecksumAlgorithm(), is("SHA1"));
	}
	
	@Test
	public void initChunker_customMimeTypeChunker_tooManyNestedChunkers(){
		thrown.expect(RuntimeException.class);
		thrown.expectCause(isA(ConfigException.class));
		final ChunkerTO chunker = new ChunkerTOBuilder()
		.setType(MimeTypeChunker.TYPE)
		.addProperty(MimeTypeChunker.PROPERTY_SPECIAL_CHUNKER_MIME_TYPES, "image/jpeg")
		.addNestedChunker(TestConfigUtil.createFixedChunkerTO())
		.addNestedChunker(TestConfigUtil.createFixedChunkerTO())
		.addNestedChunker(TestConfigUtil.createFixedChunkerTO()).build();
		configRule.getConfigWithCustomChunker(chunker);
	}
	
	@Test
	public void initChunker_customMimeTypeChunker_notEnoughNestedChunkers(){
		thrown.expect(RuntimeException.class);
		thrown.expectCause(isA(ConfigException.class));
		final ChunkerTO chunker = new ChunkerTOBuilder()
		.setType(MimeTypeChunker.TYPE)
		.addProperty(MimeTypeChunker.PROPERTY_SPECIAL_CHUNKER_MIME_TYPES, "image/jpeg").build();
		configRule.getConfigWithCustomChunker(chunker);
	}
	
	@Test
	public void initChunker_customTttdChunker(){
		final ChunkerTO chunker = new ChunkerTOBuilder()
		.setType(TttdChunker.TYPE)
		.addProperty(TttdChunker.PROPERTY_DIGEST_ALG, TttdChunker.DEFAULT_DIGEST_ALG)
		.addProperty(TttdChunker.PROPERTY_FINGERPRINT_ALG, TttdChunker.DEFAULT_FINGERPRINT_ALG)
		.addProperty(TttdChunker.PROPERTY_WINDOW_SIZE, TttdChunker.DEFAULT_WINDOW_SIZE)
		.addProperty(TttdChunker.PROPERTY_AVG_CHUNK_SIZE, 512*1024).build();
		final Config config = configRule.getConfigWithCustomChunker(chunker);
		assertThat(config.getChunker(), is(instanceOf(TttdChunker.class)));
		assertThat(config.getChunker().getChecksumAlgorithm(), is("SHA1"));
	}
	
	@Test
	public void initChunker_customTttdChunker_avgChunkSizeOnly(){
		final ChunkerTO chunker = new ChunkerTOBuilder()
		.setType(TttdChunker.TYPE)
		.addProperty(TttdChunker.PROPERTY_AVG_CHUNK_SIZE, 512*1024).build();
		final Config config = configRule.getConfigWithCustomChunker(chunker);
		assertThat(config.getChunker(), is(instanceOf(TttdChunker.class)));
		assertThat(config.getChunker().getChecksumAlgorithm(), is("SHA1"));
	}
	
	@Test
	public void initChunker_customTttdChunker_noAvgChunkSize(){
		thrown.expect(RuntimeException.class);
		thrown.expectCause(isA(ConfigException.class));
		final ChunkerTO chunker = new ChunkerTOBuilder().setType(TttdChunker.TYPE).build();
		configRule.getConfigWithCustomChunker(chunker);
	}
	
	@Test
	public void initChunker_customTttdChunker_windowSizeGreaterThanTmin(){
		thrown.expect(RuntimeException.class);
		thrown.expectCause(isA(ConfigException.class));
		final ChunkerTO chunker = new ChunkerTOBuilder()
		.setType(TttdChunker.TYPE)
		.addProperty(TttdChunker.PROPERTY_WINDOW_SIZE, 6000)
		.build();
		configRule.getConfigWithCustomChunker(chunker);
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

		ArrayList<TransformerTO> transformers = new ArrayList<TransformerTO>();
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
		configTO.setMasterKey(TestConfigUtil.createDummyMasterKey()); // <<< valid
		
		// Set invalid transformer
		TransformerTO invalidTransformerTO = new TransformerTO();
		invalidTransformerTO.setType("cipher");
		invalidTransformerTO.setSettings(new HashMap<String, String>() {
			{
				put("cipherspecs", "1,2");
			}
		});

		ArrayList<TransformerTO> transformers = new ArrayList<TransformerTO>();
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
		configTO.setMasterKey(TestConfigUtil.createDummyMasterKey()); // <<< valid
		
		// Set invalid transformer
		TransformerTO invalidTransformerTO = new TransformerTO();
		invalidTransformerTO.setType("cipher");
		invalidTransformerTO.setSettings(new HashMap<String, String>() {
			{
				put("cipherspecs", "1,INVALIDXXXX"); // <<<< INVALID !
			}
		});

		ArrayList<TransformerTO> transformers = new ArrayList<TransformerTO>();
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
	

	
}
