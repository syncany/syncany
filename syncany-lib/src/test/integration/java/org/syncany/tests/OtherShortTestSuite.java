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
package org.syncany.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.syncany.tests.integration.plugins.OAuthTokenWebListenerTest;
import org.syncany.tests.integration.plugins.PluginOptionsTest;
import org.syncany.tests.integration.plugins.PluginsTest;
import org.syncany.tests.integration.plugins.TransferSettingsTest;
import org.syncany.tests.integration.plugins.local.LocalTransferManagerPluginTest;
import org.syncany.tests.integration.plugins.unreliable_local.CleanupInterruptedTest;
import org.syncany.tests.integration.plugins.unreliable_local.UploadInterruptedTest;
import org.syncany.tests.unit.chunk.FixedOffsetChunkerTest;
import org.syncany.tests.unit.chunk.FrameworkCombinationTest;
import org.syncany.tests.unit.chunk.MultiChunkerTest;
import org.syncany.tests.unit.chunk.TTTDChunkerTest;
import org.syncany.tests.unit.config.CacheTest;
import org.syncany.tests.unit.config.ConfigHelperTest;
import org.syncany.tests.unit.config.ConfigTest;
import org.syncany.tests.unit.crypto.AesGcmWithBcInputStreamTest;
import org.syncany.tests.unit.crypto.CipherSessionTest;
import org.syncany.tests.unit.crypto.CipherSpecsTest;
import org.syncany.tests.unit.crypto.CipherUtilTest;
import org.syncany.tests.unit.crypto.MultiCipherStreamsTest;
import org.syncany.tests.util.SqlRunnerTest;

@RunWith(Suite.class)
@SuiteClasses({
		// Util
		SqlRunnerTest.class,

		// Crypto
		CipherSpecsTest.class,
		CipherUtilTest.class,
		MultiCipherStreamsTest.class,
		CipherSessionTest.class,
		AesGcmWithBcInputStreamTest.class,

		// Chunking Framework
		MultiChunkerTest.class,
		FixedOffsetChunkerTest.class,
		TTTDChunkerTest.class,
		FrameworkCombinationTest.class,

		// Connection
		PluginsTest.class,
		LocalTransferManagerPluginTest.class,
		UploadInterruptedTest.class,
		CleanupInterruptedTest.class,
		TransferSettingsTest.class,
		PluginOptionsTest.class,
		OAuthTokenWebListenerTest.class,

		// Config
		CacheTest.class,
		ConfigHelperTest.class,
		ConfigTest.class
})
public class OtherShortTestSuite {
	// This class executes all tests
}
