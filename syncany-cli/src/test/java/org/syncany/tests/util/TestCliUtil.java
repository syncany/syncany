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
package org.syncany.tests.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.cli.CommandLineClient;
import org.syncany.config.Config;
import org.syncany.config.ConfigException;
import org.syncany.config.to.ConfigTO;
import org.syncany.util.StringUtil;

public class TestCliUtil {
	private static final Logger logger = Logger.getLogger(TestCliUtil.class.getSimpleName());

	public static Map<String, String> createLocalTestEnv(String machineName, Map<String, String> connectionProperties) throws Exception {
		Map<String, String> clientSettings = new HashMap<String, String>();

		File tempLocalDir = TestFileUtil.createTempDirectoryInSystemTemp(TestConfigUtil.createUniqueName("client-" + machineName,
				connectionProperties));
		tempLocalDir.mkdirs();

		// Client settings 
		clientSettings.put("machinename", machineName);
		clientSettings.put("localdir", tempLocalDir.getAbsolutePath());
		clientSettings.put("repopath", connectionProperties.get("path"));

		return clientSettings;
	}

	public static Map<String, String> createLocalTestEnvAndInit(String machineName, Map<String, String> connectionProperties) throws Exception {
		Map<String, String> client = createLocalTestEnv(machineName, connectionProperties);

		// Init
		String[] initArgs = new String[] {
				"--localdir", client.get("localdir"),
				"init",
				"--no-daemon",
				"--plugin", "local",
				"--plugin-option", "path=" + client.get("repopath"),
				"--no-encryption",
				"--no-compression",
		};

		logger.log(Level.INFO, "Running syncany with argument: " + StringUtil.join(initArgs, " "));
		new CommandLineClient(initArgs).start();

		fixMachineName(client);

		return client;
	}

	public static Map<String, String> createLocalTestEnvAndConnect(String machineName, Map<String, String> connectionProperties) throws Exception {
		Map<String, String> client = createLocalTestEnv(machineName, connectionProperties);

		// Init
		String[] connectArgs = new String[] {
				"--localdir", client.get("localdir"),
				"connect",
				"--no-daemon",
				"--plugin", "local",
				"--plugin-option", "path=" + client.get("repopath")
		};

		logger.log(Level.INFO, "Running syncany with argument: " + StringUtil.join(connectArgs, " "));
		new CommandLineClient(connectArgs).start();

		fixMachineName(client);

		return client;
	}

	public static String[] runAndCaptureOutput(CommandLineClient cli) throws Exception {
		ByteArrayOutputStream bufferedCliOut = new ByteArrayOutputStream();

		cli.setOut(new SplitOutputStream(bufferedCliOut, System.out));
		cli.start();

		logger.log(Level.INFO, "CLI output: ");
		logger.log(Level.INFO, toString(bufferedCliOut));

		return toStringArray(bufferedCliOut);
	}

	private static void fixMachineName(Map<String, String> client) throws Exception {
		File configFile = new File(client.get("localdir") + "/" + Config.DIR_APPLICATION + "/" + Config.FILE_CONFIG);
		Serializer serializer = new Persister();

		ConfigTO configTO = serializer.read(ConfigTO.class, configFile);
		configTO.setMachineName(client.get("machinename"));

		serializer.write(configTO, configFile);
	}

	public static void deleteTestLocalConfigAndData(Map<String, String> clientSettings) throws ConfigException {
		if (clientSettings.get("localdir") != null)
			TestFileUtil.deleteDirectory(new File(clientSettings.get("localdir")));
		if (clientSettings.get("cachedir") != null)
			TestFileUtil.deleteDirectory(new File(clientSettings.get("cachedir")));
		if (clientSettings.get("databasedir") != null)
			TestFileUtil.deleteDirectory(new File(clientSettings.get("databasedir")));
		if (clientSettings.get("configfile") != null)
			TestFileUtil.deleteDirectory(new File(clientSettings.get("configfile")));
		if (clientSettings.get("appdir") != null)
			TestFileUtil.deleteDirectory(new File(clientSettings.get("appdir")));
		if (clientSettings.get("repopath") != null)
			TestFileUtil.deleteDirectory(new File(clientSettings.get("repopath")));
	}

	public static String toString(ByteArrayOutputStream bos) {
		return new String(bos.toByteArray());
	}

	public static String[] toStringArray(ByteArrayOutputStream bos) {
		return toString(bos).split("[\\r\\n]|[\\n\\r]|[\\n]");
	}

	public static boolean setCurrentDirectory(File newDirectory) {
		boolean result = false;
		File directory = newDirectory.getAbsoluteFile();

		if (directory.exists() || directory.mkdirs()) {
			result = (System.setProperty("user.dir", directory.getAbsolutePath()) != null);
		}

		return result;
	}
}
