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
package org.syncany.gui.wizard;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.syncany.gui.panel.PluginPanel;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class RepositoryPluginPanel extends WizardPanelComposite {
	private static final Logger log = Logger.getLogger(RepositoryPluginPanel.class.getSimpleName());
	private PluginPanel composite = null;
	private String className;

	public RepositoryPluginPanel(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));
	}
	
	/**
	 * @param className the className to set
	 */
	public void setClassName(String className) {
		this.className = className;
		
		initComposite();
		layout();
	}

	private void initComposite() {
		if (composite != null){
			composite.dispose();
		}
		try {
			Class<?>[] type = { Composite.class, int.class };
			Class<?> classDefinition = Class.forName(className);
			Constructor<?> cons = classDefinition.getConstructor(type);
			Object[] obj = { this, SWT.NONE };
			
			composite = (PluginPanel) cons.newInstance(obj);
		}
		catch (Exception e) {
			log.warning("Unable to instanciate plugin gui panel " + className);
		}
	}

	@Override
	public boolean isValid() {
		return composite != null && composite.isValid();
	}

	@Override
	public Map<String, String> getUserSelection() {
		return composite.getParameters();
	}
}
