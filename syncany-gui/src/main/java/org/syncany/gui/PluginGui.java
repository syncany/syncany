/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.gui;

import java.util.ArrayList;
import java.util.List;

import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.gui.panel.InitAmazonS3OperationPanel;
import org.syncany.gui.panel.InitFtpOperationPanel;
import org.syncany.gui.panel.InitLocalOperationPanel;

/**
 * @author vincent
 *
 */
public class PluginGui {
	private String code;
	private String className;
	
	public PluginGui(String code, String className){
		this.code = code;
		this.className = className;
	}
	
	public static List<PluginGui> getAvailablePlugins() {
		List<Plugin> plugins = Plugins.list();
		List<PluginGui> list = new ArrayList<>();

		for (Plugin p : plugins){
			switch (p.getName().toLowerCase()){
				case "ftp":
					list.add(new PluginGui(p.getName(), InitFtpOperationPanel.class.getCanonicalName()));
					break;
				case "local":
					list.add(new PluginGui(p.getName(), InitLocalOperationPanel.class.getCanonicalName()));
					break;
				case "amazon s3":
					list.add(new PluginGui(p.getName(), InitAmazonS3OperationPanel.class.getCanonicalName()));
					break;					
			}
		}
		
		return list;
	}

	public String getCode() {
		return code;
	}

	public String getClassName() {
		return className;
	}
	
	@Override
	public String toString() {
		return code;
	}
}
