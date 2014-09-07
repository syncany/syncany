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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.rules.TemporaryFolder;
import org.syncany.chunk.Chunker;
import org.syncany.chunk.MimeTypeChunker;
import org.syncany.config.Config;
import org.syncany.config.ConfigException;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.RepoTO;
import org.syncany.config.to.RepoTO.ChunkerTO;
import org.syncany.config.to.RepoTO.MultiChunkerTO;
import org.syncany.config.to.RepoTO.TransformerTO;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.tests.util.TestConfigUtil;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * The config rule allows creation of fully or partially initialized {@code Config} instances. It further contains
 * different builders for different transfer objects; for example {@link ChunkerTOBuilder}.
 * 
 * <p> Each created Config object is initialized with a new local folder as defined by {@link TemporaryFolder#newFolder()}.
 * 
 * @author Gregor Trefs <Gregor.Trefs@gmail.com>
 */
public class ConfigRule extends TemporaryFolder {
	
	private File configFolder;
	
	@Override
	protected void before() throws Throwable {
		super.before();
		configFolder = newFolder();
	}
	
	public Config getDefaultConfig(){
		final ConfigTO configTO = createConfigTO();
		final RepoTO repoTO = createRepoTO();
		return new ConfigBuilder().setConfigTO(configTO).setRepoTO(repoTO).setLocalDir(configFolder).build();
	}

	private RepoTO createRepoTO() {
		return new RepoTOBuilder().setChunkerTO(TestConfigUtil.createFixedChunkerTO())
				.setMultiChunkerTO(TestConfigUtil.createZipMultiChunkerTO())
				.setRepoId(new byte[] { 0x01, 0x02 })
				.setTransformersTO(null).build();
	}

	private ConfigTO createConfigTO() {
		return new ConfigTOBuilder().setMachineName("somevalidmachinename").build();
	}
	
	public Config getConfigWithCustomChunker(ChunkerTO chunker){
		final ConfigTO configTO = createConfigTO();
		final RepoTO repoTO = createRepoTO();
		repoTO.setChunkerTO(chunker);
		return new ConfigBuilder().setConfigTO(configTO).setRepoTO(repoTO).setLocalDir(configFolder).build();
	}
	
	public static class ConfigBuilder{
		private ConfigTO configTO;
		private RepoTO repoTO;
		private File localDir;
		
		public ConfigBuilder setConfigTO(ConfigTO configTO) {
			this.configTO = configTO;
			return this;
		}
		
		public ConfigBuilder setRepoTO(RepoTO repoTO) {
			this.repoTO = repoTO;
			return this;
		}
		
		public ConfigBuilder setLocalDir(File localDir) {
			this.localDir = localDir;
			return this;
		}

		public Config build(){
			try {
				return new Config(localDir, configTO, repoTO);
			} catch(ConfigException ce){
				throw Throwables.propagate(ce);
			}
		}
	}
	
	public static class RepoTOBuilder{
		private byte[] repoId;
		private ChunkerTO chunker;
		private MultiChunkerTO multiChunker;
		private ArrayList<TransformerTO> transformers;
		
		public RepoTOBuilder setRepoId(byte[] repoId) {
			this.repoId = repoId;
			return this;
		}
		
		public RepoTOBuilder setChunkerTO(ChunkerTO chunker) {
			this.chunker = chunker;
			return this;
		}
		
		public RepoTOBuilder setMultiChunkerTO(MultiChunkerTO multiChunker) {
			this.multiChunker = multiChunker;
			return this;
		}
		
		public RepoTOBuilder setTransformersTO(ArrayList<TransformerTO> transformers) {
			this.transformers = transformers;
			return this;
		}
		
		public RepoTO build(){
			RepoTO repoTO = new RepoTO();
			repoTO.setChunkerTO(chunker);
			repoTO.setMultiChunker(multiChunker);
			repoTO.setRepoId(repoId);
			repoTO.setTransformers(transformers);
			return repoTO;
		}
	}
	
	public static class ConfigTOBuilder{
		private String machineName;
		private String displayName;
		private SaltedSecretKey masterKey;
		
		public ConfigTOBuilder setMachineName(String machineName) {
			this.machineName = machineName;
			return this;
		}
		
		public ConfigTOBuilder setDisplayName(String displayName) {
			this.displayName = displayName;
			return this;
		}
		
		public ConfigTOBuilder setMasterKey(SaltedSecretKey masterKey) {
			this.masterKey = masterKey;
			return this;
		}
		
		public ConfigTO build(){
			ConfigTO configTO = new ConfigTO();
			configTO.setDisplayName(displayName);
			configTO.setMachineName(machineName);
			if(masterKey == null){
				masterKey = TestConfigUtil.createDummyMasterKey();
			}
			configTO.setMasterKey(masterKey);
			return configTO;
		}

	}
	
	public static class ChunkerTOBuilder {
		private final List<ChunkerTO> nestedChunkers;
		protected final Map<String, String> settings;
		private String type;
		
		public ChunkerTOBuilder() {
			nestedChunkers = Lists.newArrayList();
			settings = Maps.newHashMap();
		}
		
		public static <T extends Chunker> ChunkerTOBuilder of(Class<T> chunkerClass){
			if(chunkerClass.equals(MimeTypeChunker.class)){
				return new MimeTypeChunkerTOBuilder();
			}
			return new ChunkerTOBuilder();
		}
		
		public ChunkerTOBuilder addNestedChunker(ChunkerTO nestedChunker){
			nestedChunkers.add(nestedChunker);
			return this;
		}
		
		public ChunkerTOBuilder addProperty(String key, String value){
			settings.put(key, value);
			return this;
		}
		
		public ChunkerTOBuilder addProperty(String key, Integer value){
			return addProperty(key, String.valueOf(value));
		}

		public ChunkerTOBuilder setType(String type) {
			this.type = type;
			return this;
		}
		
		public ChunkerTO build(){
			final ChunkerTO chunkerTO = new ChunkerTO();
			chunkerTO.setSettings(settings);
			chunkerTO.setNestedChunkers(nestedChunkers);
			chunkerTO.setType(type);
			return chunkerTO;
		}
	}
	
	public static class MimeTypeChunkerTOBuilder extends ChunkerTOBuilder {
		@Override
		public ChunkerTOBuilder addProperty(String key, String value) {
			if(key.equals(MimeTypeChunker.PROPERTY_SPECIAL_CHUNKER_MIME_TYPES)){
				final String previousValue = Optional.fromNullable(settings.get(key)).or("");
				final String nextValue = (previousValue + " " + value).trim();
				return super.addProperty(key, nextValue);
			} else {
				return super.addProperty(key, value);
			}
		}
	}
	
}
