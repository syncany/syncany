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
package org.syncany.gui.panel;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager.StorageTestResult;


/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public abstract class PluginPanel extends Composite implements UserParametersChecker {
	public enum PluginPanelPurpose {
		CREATE,
		CONNECT;
	}
	
	protected PluginPanelPurpose purpose;
	
	/**
	 * @param parent
	 * @param style
	 */
	public PluginPanel(Composite parent, int style) {
		super(parent, style);
	}
	
	public PluginPanelPurpose getPurpose() {
		return purpose;
	}
	public void setPurpose(PluginPanelPurpose purpose) {
		this.purpose = purpose;
	}
	
	public StorageTestResult testPluginConnection() throws StorageException{
		Map<String, String> params = getUserSelection().getPluginParameters();
		Connection connection = Plugins.get(getPluginId()).createConnection();
		connection.init(params);
		return connection.createTransferManager().test();
	}
	
	// Anstract Methods
	
	/**
	 * Returns <code>Plugin</code> id
	 */
	public abstract String getPluginId();
}
