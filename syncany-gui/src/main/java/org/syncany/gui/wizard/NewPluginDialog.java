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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.syncany.gui.panel.PluginPanel;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class NewPluginDialog extends DefaultWizardPanel {
	private PluginPanel composite = null;
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public NewPluginDialog(Map<String, Object> params, Shell parent, int style) {
		super(params, WizardType.NEXT | WizardType.PREVIOUS, parent, style);
	}
	
	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		super.createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return null;
	}
	
	protected Composite createComposite(Shell shell){
		
		String className = (String)getWizardParameters().get("pluginGuiClassName");
		try{
			Class<?>[] type = { Composite.class, int.class};
			Class<?> classDefinition = Class.forName(className); 
			Constructor<?> cons = classDefinition.getConstructor(type);
			Object[] obj = { shell, SWT.NONE};
			composite = (PluginPanel)cons.newInstance(obj);
		}
		catch (Exception e){
			
		}
		
		return composite;
	}

	@Override
	protected void handleAction(WizardAction action) {
		if (action == WizardAction.NEXT){
			if (composite != null){
				getWizardParameters().put("pluginParameters", composite.getParameters());
			}
			this.shell.dispose();
			NewLocalFolders sd = new NewLocalFolders(getWizardParameters(), getParent(), SWT.APPLICATION_MODAL);
			sd.open();
		}
		else if (action == WizardAction.PREVIOUS){
			this.shell.dispose();
			NewDialog sd = new NewDialog(getWizardParameters(), getParent(), SWT.APPLICATION_MODAL);
			sd.open();
		}
	}
}
