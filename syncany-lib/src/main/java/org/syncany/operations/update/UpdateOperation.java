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
package org.syncany.operations.update;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simpleframework.xml.core.Persister;
import org.syncany.Client;
import org.syncany.config.Config;
import org.syncany.config.LocalEventBus;
import org.syncany.operations.Operation;
import org.syncany.operations.daemon.messages.ConnectToHostExternalEvent;
import org.syncany.operations.update.UpdateOperationResult.UpdateResultCode;
import org.syncany.plugins.Plugins;
import org.syncany.util.EnvironmentUtil;

import com.github.zafarkhaja.semver.Version;

/**
 * This operation manages updates of the application. It currently only 
 * performs update checks, but will likely be extended to automatically
 * update the application. The following actions exist:
 * 
 * <p>The 'check' action checks if a new application version is available. 
 * It queries the Syncany API and outputs whether the local copy of the
 * application is up-to-date. If it is not, it outputs the newest version
 * and a download URL.
 * 
 * @see <a href="https://github.com/syncany/syncany-website">Syncany Website/API</a>
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
public class UpdateOperation extends Operation {
	private static final Logger logger = Logger.getLogger(UpdateOperation.class.getSimpleName());

	private static final String GUI_PLUGIN_ID = "gui";
	private static final String API_DEFAULT_ENDPOINT_URL = "https://api.syncany.org/v3";
	private static final String API_APP_LIST_REQUEST_FORMAT = "%s/app?dist=%s&type=%s&snapshots=%s&os=%s&arch=%s";

	private UpdateOperationOptions options;
	private UpdateOperationResult result;

	private LocalEventBus eventBus;

	public UpdateOperation(Config config, UpdateOperationOptions options) {
		super(config);

		this.options = options;
		this.result = new UpdateOperationResult();

		this.eventBus = LocalEventBus.getInstance();
	}

	@Override
	public UpdateOperationResult execute() throws Exception {
		result.setAction(options.getAction());

		switch (options.getAction()) {
		case CHECK:
			return executeCheck();

		default:
			throw new Exception("Unknown action: " + options.getAction());
		}
	}

	private UpdateOperationResult executeCheck() throws Exception {
		Version localAppVersion = Version.valueOf(Client.getApplicationVersion());

		String appInfoResponseStr = getAppInfoResponseStr();
		AppInfoResponse appInfoResponse = new Persister().read(AppInfoResponse.class, appInfoResponseStr);

		ArrayList<AppInfo> appInfoList = appInfoResponse.getAppInfoList();

		if (appInfoList.size() > 0) {
			AppInfo remoteAppInfo = appInfoList.get(0);
			Version remoteAppVersion = Version.valueOf(remoteAppInfo.getAppVersion());

			boolean newVersionAvailable = remoteAppVersion.greaterThan(localAppVersion);

			result.setResultCode(UpdateResultCode.OK);
			result.setAppInfo(remoteAppInfo);
			result.setNewVersionAvailable(newVersionAvailable);

			return result;
		}
		else {
			result.setResultCode(UpdateResultCode.NOK);
			return result;
		}
	}

	private String getAppInfoResponseStr() throws Exception {
		boolean hasGuiPlugin = Plugins.get(GUI_PLUGIN_ID) != null;

		String typeStr = determineType(hasGuiPlugin);
		String distStr = determineDist(hasGuiPlugin, typeStr);
		String snapshotsEnabled = (options.isSnapshots()) ? "true" : "false";
		String osStr = EnvironmentUtil.getOperatingSystemDescription();
		String archStr = EnvironmentUtil.getArchDescription();

		String apiEndpointUrl = (options.getApiEndpoint() != null) ? options.getApiEndpoint() : API_DEFAULT_ENDPOINT_URL;
		URL appListUrl = new URL(String.format(API_APP_LIST_REQUEST_FORMAT, apiEndpointUrl, distStr, typeStr, snapshotsEnabled,
				osStr, archStr));

		logger.log(Level.INFO, "Querying " + appListUrl + " ...");
		eventBus.post(new ConnectToHostExternalEvent(appListUrl.getHost()));

		URLConnection urlConnection = appListUrl.openConnection();
		urlConnection.setConnectTimeout(2000);
		urlConnection.setReadTimeout(2000);

		BufferedReader urlStreamReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
		StringBuilder responseStringBuilder = new StringBuilder();

		String line;
		while ((line = urlStreamReader.readLine()) != null) {
			responseStringBuilder.append(line);
		}

		String responseStr = responseStringBuilder.toString();
		logger.log(Level.INFO, "Response from api.syncany.org: " + responseStr);

		return responseStr;
	}

	private String determineType(boolean hasGuiPlugin) {
		if (EnvironmentUtil.isWindows()) {
			return "exe";
		}
		else if (EnvironmentUtil.isMacOSX()) {
			return (hasGuiPlugin) ? "app.zip" : "zip";
		}
		else if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			return (EnvironmentUtil.isDebianBased()) ? "deb" : "tar.gz";
		}

		return "zip";
	}

	private String determineDist(boolean hasGuiPlugin, String type) {
		boolean packageWithGuiExists = type.equals("exe") || type.equals("app.zip");
		return (hasGuiPlugin && packageWithGuiExists) ? "gui" : "cli";
	}
}
