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
package org.syncany.plugins.transfer;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for {@link TransferPlugin}s, using to retrieve
 * the required transfer plugin classes -- namely {@link TransferSettings},
 * {@link TransferManager} and {@link TransferPlugin}. <br/>
 * <br/>
 * <i>Plugins have to follow convention</i>
 *
 * @author Christian Roth <christian.roth@port17.de>
 */
public abstract class TransferPluginUtil {
	private static final Pattern PLUGIN_PACKAGE_NAME_PATTERN = Pattern.compile("^org\\.syncany\\.plugins\\.([a-z]+)$");

	private static final String PLUGIN_PACKAGE_NAME = "org.syncany.plugins.{0}.";
	private static final String PLUGIN_SETTINGS_CLASS_NAME = PLUGIN_PACKAGE_NAME + "{1}TransferSettings";
	private static final String PLUGIN_MANAGER_CLASS_NAME = PLUGIN_PACKAGE_NAME + "{1}TransferManager";
	private static final String PLUGIN_PLUGIN_CLASS_NAME = PLUGIN_PACKAGE_NAME + "{1}TransferPlugin";

	/**
	 * Determines the {@link TransferSettings} class for a given
	 * {@link TransferPlugin} class using the corresponding
	 * {@link PluginSettings} annotation.
	 */
	public static Class<? extends TransferSettings> getTransferSettingsClass(Class<? extends TransferPlugin> transferPluginClass) {
		String pluginName = TransferPluginUtil.getPluginPackageName(transferPluginClass);

		if (pluginName != null) {
			try {
				Class<?> pluginClass = Class.forName(MessageFormat.format(PLUGIN_SETTINGS_CLASS_NAME, pluginName.toLowerCase(), pluginName));

				if (TransferSettings.class.isAssignableFrom(pluginClass)) {
					return (Class<? extends TransferSettings>) pluginClass;
				}
			}
			catch (ClassNotFoundException e) {
				// fall through
			}
		}

		throw new RuntimeException("There are no valid transfer settings attached to that plugin (" + transferPluginClass.getName() + ")");
	}

	/**
	 * Determines the {@link TransferManager} class for a given
	 * {@link TransferPlugin} class using the corresponding
	 * {@link PluginManager} annotation.
	 */
	public static Class<? extends TransferManager> getTransferManagerClass(Class<? extends TransferPlugin> transferPluginClass) {
		String pluginName = TransferPluginUtil.getPluginPackageName(transferPluginClass);

		if (pluginName != null) {
			try {
				Class<?> pluginClass = Class.forName(MessageFormat.format(PLUGIN_MANAGER_CLASS_NAME, pluginName.toLowerCase(), pluginName));

				if (TransferManager.class.isAssignableFrom(pluginClass)) {
					return (Class<? extends TransferManager>) pluginClass;
				}
			}
			catch (ClassNotFoundException e) {
				// fall through
			}
		}

		throw new RuntimeException("There are no valid transfer manager attached to that plugin (" + transferPluginClass.getName() + ")");
	}

	/**
	 * Determines the {@link TransferPlugin} class for a given
	 * {@link TransferSettings} class.
	 */
	public static Class<? extends TransferPlugin> getTransferPluginClass(Class<? extends TransferSettings> transferSettingsClass) {
		String pluginName = TransferPluginUtil.getPluginPackageName(transferSettingsClass);

		if (pluginName != null) {
			try {
				Class<?> pluginClass = Class.forName(MessageFormat.format(PLUGIN_PLUGIN_CLASS_NAME, pluginName.toLowerCase(), pluginName));

				if (TransferPlugin.class.isAssignableFrom(pluginClass)) {
					return (Class<? extends TransferPlugin>) pluginClass;
				}
			}
			catch (ClassNotFoundException e) {
				// fall through
			}
		}

		throw new RuntimeException("The transfer settings are orphan (" + transferSettingsClass.getName() + ")");
	}

	private static String getPluginPackageName(Class<?> clazz) {
		Matcher matcher = PLUGIN_PACKAGE_NAME_PATTERN.matcher(clazz.getPackage().getName());

		if (matcher.matches()) {
			String pluginPackageName = matcher.group(1);
			return Character.toUpperCase(pluginPackageName.charAt(0)) + pluginPackageName.substring(1);
		}

		return null;
	}
}
