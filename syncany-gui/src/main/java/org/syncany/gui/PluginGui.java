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

import org.syncany.gui.panel.plugin.InitLocalOperationPanel;

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
		List<PluginGui> list = new ArrayList<>();

		list.add(new PluginGui("ftp", "org.syncany.gui.panel.plugin.InitFtpOperationPanel"));
		list.add(new PluginGui("amazon s3","org.syncany.gui.panel.plugin.InitAmazonS3OperationPanel"));
		list.add(new PluginGui("local", InitLocalOperationPanel.class.getCanonicalName()));
		
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
