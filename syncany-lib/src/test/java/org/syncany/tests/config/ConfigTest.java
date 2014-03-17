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

import org.junit.Ignore;
import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.config.Config.ConfigException;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.RepoTO;

public class ConfigTest {
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
	
	@Test(expected = ConfigException.class)
	public void testConfigMachineNameInvalidChars() throws Exception {
		File localDir = new File("/some/folder"); 
		ConfigTO configTO = new ConfigTO();
		RepoTO repoTO = new RepoTO();
		
		configTO.setMachineName("invalid machine name");
		
		new Config(localDir, configTO, repoTO);			
	}
	
	@Test(expected = ConfigException.class)
	public void testConfigMachineNameInvalidNull() throws Exception {
		File localDir = new File("/some/folder"); 
		ConfigTO configTO = new ConfigTO();
		RepoTO repoTO = new RepoTO();
		
		configTO.setMachineName(null);
		
		new Config(localDir, configTO, repoTO);			
	}
	
	@Test
	@Ignore	
	public void testConfigCipherTransformers() throws Exception {
		// TODO [medium] Implement TransformerTO test
	}
}
