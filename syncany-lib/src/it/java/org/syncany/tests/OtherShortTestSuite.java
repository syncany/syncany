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
import org.syncany.chunk.FixedOffsetChunkerTest;
import org.syncany.chunk.FrameworkCombinationTest;
import org.syncany.chunk.MultiChunkerTest;
import org.syncany.chunk.TTTDChunkerTest;
import org.syncany.config.CacheTest;
import org.syncany.config.ConfigHelperTest;
import org.syncany.config.ConfigTest;
import org.syncany.crypto.AesGcmWithBcInputStreamTest;
import org.syncany.crypto.CipherSessionTest;
import org.syncany.crypto.CipherSpecsTest;
import org.syncany.crypto.CipherUtilTest;
import org.syncany.crypto.MultiCipherStreamsTest;
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
