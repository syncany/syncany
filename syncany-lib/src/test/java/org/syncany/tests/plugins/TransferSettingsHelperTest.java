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
import org.junit.Before;
import org.junit.Test;
import org.syncany.config.to.ConfigTO;
import org.syncany.plugins.PluginOptionSpecs;
import org.syncany.plugins.StorageException;
import org.syncany.plugins.TransferSettingsHelper;
import org.syncany.plugins.annotations.Option;
import org.syncany.plugins.transfer.TransferSettings;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TransferSettingsHelperTest {

  private final Map<String, String> settings = Maps.newHashMap();

  @Before
  public void clean() {
    settings.clear();
  }

	@Test
	public void testRestore() throws Exception {

    final ConfigTO.ConnectionTO connection = new ConfigTO.ConnectionTO();

    settings.put("foo", "bar");
    settings.put("baz", "inga");
    settings.put("number", "1");

    connection.setType("test");
    connection.setSettings(settings);

    TransferSettingsDummy tsd = TransferSettingsHelper.restore(connection, TransferSettingsDummy.class);
    assertEquals(tsd.foo, "bar");
    assertEquals(tsd.baz, "inga");
    assertEquals(tsd.number, 1);

	}

  @Test(expected = StorageException.class)
  public void testMissingMandatory() throws Exception {

    final ConfigTO.ConnectionTO connection = new ConfigTO.ConnectionTO();

    settings.put("baz", "bar");

    connection.setType("test");
    connection.setSettings(settings);

    // exception because foo is missing
    TransferSettingsHelper.restore(connection, TransferSettingsDummy.class);

  }

  @Test(expected = StorageException.class)
  public void testInvalidType() throws Exception {

    final ConfigTO.ConnectionTO connection = new ConfigTO.ConnectionTO();

    connection.setType("test");
    connection.setSettings(settings);

    // exception because foo is missing
    TransferSettingsHelper.restore(connection, TransferSettingsInvalidDummy.class);

  }

  public static class TransferSettingsDummy extends TransferSettings {

    @Option(mandatory = true, encrypted = false)
    public String foo;

    @Option(name = "baz", mandatory = false)
    public String baz;

    @Option(name = "number")
    public int number;

    @Override
    public PluginOptionSpecs getOptionSpecs() {
      return null;
    }

    @Override
    public void init(Map<String, String> optionValues) throws StorageException {
      TransferSettingsHelper.restore(null, this.getClass());
    }
  }

  public static class TransferSettingsInvalidDummy extends TransferSettings {

    @Option(mandatory = true, encrypted = false)
    public char foo;

    @Override
    public PluginOptionSpecs getOptionSpecs() {
      return null;
    }

    @Override
    public void init(Map<String, String> optionValues) throws StorageException {
      TransferSettingsHelper.restore(null, this.getClass());
    }
  }

}
