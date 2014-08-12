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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.syncany.plugins.PluginOptionSpec;
import org.syncany.plugins.PluginOptionSpec.OptionValidationResult;
import org.syncany.plugins.PluginOptionSpec.ValueType;

/**
 * Tests the PluginOptionSpec class on integrity.
 *
 * @author Pim Otte
 */
public class PluginOptionSpecTest {
	
	@Test
	public void nonSetMandatoryTest() {
		PluginOptionSpec pluginOptionSpec = new PluginOptionSpec("id", "desc", ValueType.STRING, true, true, null);
		OptionValidationResult validationResult = pluginOptionSpec.validateInput(null);
		
		assertEquals(validationResult,OptionValidationResult.INVALID_NOT_SET);
	}
	
	@Test
	public void invalidIntegerTest() {
		PluginOptionSpec pluginOptionSpec = new PluginOptionSpec("id", "desc", ValueType.INT, true, true, null);
		OptionValidationResult validationResult = pluginOptionSpec.validateInput("this is in no way an integer");
		
		assertEquals(validationResult,OptionValidationResult.INVALID_TYPE);
	}
	
	@Test
	public void validOptionTest() {
		PluginOptionSpec pluginOptionSpec = new PluginOptionSpec("id", "desc", ValueType.BOOLEAN, true, true, null);
		OptionValidationResult validationResult = pluginOptionSpec.validateInput("booleans are fairly flexible");
		
		assertEquals(validationResult,OptionValidationResult.VALID);
	}
	
	@Test
	public void defaultValueTest() {
		PluginOptionSpec pluginOptionSpec = new PluginOptionSpec("id", "desc", ValueType.INT, false, false, "21");
		String port = pluginOptionSpec.getValue("");
		
		assertEquals(port, "21");
	}
}
