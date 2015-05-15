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
package org.syncany.plugins.transfer;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.syncany.plugins.Plugin;
import com.google.common.base.CaseFormat;

/**
 * Helper class for {@link TransferPlugin}s, using to retrieve
 * the required transfer plugin classes -- namely {@link TransferSettings},
 * {@link TransferManager} and {@link TransferPlugin}.
 *
 * @author Christian Roth <christian.roth@port17.de>
 */
public abstract class TransferPluginUtil {
	private static final String PLUGIN_PACKAGE_NAME_REGEX = "^" + Plugin.class.getPackage().getName().replace(".", "\\.") + "\\.([a-z0-9_]+)$";
	private static final Pattern PLUGIN_PACKAGE_NAME_PATTERN = Pattern.compile(PLUGIN_PACKAGE_NAME_REGEX);

	private static final String PLUGIN_PACKAGE_NAME = Plugin.class.getPackage().getName() + ".{0}.";
	private static final String PLUGIN_TRANSFER_SETTINGS_CLASS_NAME = PLUGIN_PACKAGE_NAME + "{1}" + TransferSettings.class.getSimpleName();
	private static final String PLUGIN_TRANSFER_MANAGER_CLASS_NAME = PLUGIN_PACKAGE_NAME + "{1}" + TransferManager.class.getSimpleName();
	private static final String PLUGIN_TRANSFER_PLUGIN_CLASS_NAME = PLUGIN_PACKAGE_NAME + "{1}" + TransferPlugin.class.getSimpleName();

	/**
	 * Determines the {@link TransferSettings} class for a given
	 * {@link TransferPlugin} class.
	 */
	public static Class<? extends TransferSettings> getTransferSettingsClass(Class<? extends TransferPlugin> transferPluginClass) {
		String pluginNameIdentifier = TransferPluginUtil.getPluginPackageName(transferPluginClass);

		if (pluginNameIdentifier == null) {
			throw new RuntimeException("There are no valid transfer settings attached to that plugin (" + transferPluginClass.getName() + ")");
		}
		else {
			try {
				String pluginPackageIdentifier = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, pluginNameIdentifier);
				String transferSettingsClassName = MessageFormat.format(PLUGIN_TRANSFER_SETTINGS_CLASS_NAME, pluginPackageIdentifier,
						pluginNameIdentifier);
				
				return Class.forName(transferSettingsClassName).asSubclass(TransferSettings.class);
			}
			catch (Exception e) {
				throw new RuntimeException("Cannot find matching transfer settings class for plugin (" + transferPluginClass.getName() + ")");
			}
		}
	}

	/**
	 * Determines the {@link TransferManager} class for a given
	 * {@link TransferPlugin} class.
	 */
	public static Class<? extends TransferManager> getTransferManagerClass(Class<? extends TransferPlugin> transferPluginClass) {
		String pluginNameIdentifier = TransferPluginUtil.getPluginPackageName(transferPluginClass);

		if (pluginNameIdentifier == null) {
			throw new RuntimeException("There are no valid transfer manager attached to that plugin (" + transferPluginClass.getName() + ")");
		}
		else {
			try {
				String pluginPackageIdentifier = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, pluginNameIdentifier);
				String transferManagerClassName = MessageFormat.format(PLUGIN_TRANSFER_MANAGER_CLASS_NAME, pluginPackageIdentifier,
						pluginNameIdentifier);
				return Class.forName(transferManagerClassName).asSubclass(TransferManager.class);
			}
			catch (Exception e) {
				throw new RuntimeException("Cannot find matching transfer manager class for plugin (" + transferPluginClass.getName() + ")");
			}
		}
	}

	/**
	 * Determines the {@link TransferPlugin} class for a given
	 * {@link TransferSettings} class.
	 */
	public static Class<? extends TransferPlugin> getTransferPluginClass(Class<? extends TransferSettings> transferSettingsClass) {
		String pluginNameIdentifier = TransferPluginUtil.getPluginPackageName(transferSettingsClass);

		if (pluginNameIdentifier == null) {
			throw new RuntimeException("The transfer settings are orphan (" + transferSettingsClass.getName() + ")");
		}
		else {
			try {
				String pluginPackageIdentifier = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, pluginNameIdentifier);
				String transferPluginClassName = MessageFormat.format(PLUGIN_TRANSFER_PLUGIN_CLASS_NAME, pluginPackageIdentifier,
						pluginNameIdentifier);
				return Class.forName(transferPluginClassName).asSubclass(TransferPlugin.class);
			}
			catch (Exception e) {
				throw new RuntimeException("Cannot find matching transfer plugin class for plugin settings (" + transferSettingsClass.getName() + ")");
			}
		}
	}

	private static String getPluginPackageName(Class<?> clazz) {
		Matcher matcher = PLUGIN_PACKAGE_NAME_PATTERN.matcher(clazz.getPackage().getName());

		if (matcher.matches()) {
			String pluginPackageName = matcher.group(1);
			return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, pluginPackageName);
		}

		return null;
	}
}
