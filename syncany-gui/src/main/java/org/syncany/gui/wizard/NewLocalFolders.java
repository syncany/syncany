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

import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.syncany.util.I18n;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class NewLocalFolders extends DefaultWizardPanel {
	private static final Logger log = Logger.getLogger(NewLocalFolders.class.getSimpleName());
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public NewLocalFolders(Map<String, Object> params, Shell parent, int style) {
		super(params, WizardType.NEXT | WizardType.PREVIOUS, parent, style);
	}
	
	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		log.fine("creating NewLocalFolders wizard panel");
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
	private List list;
	
	protected Composite createComposite(Shell shell){
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(1, false);
		gl_composite.verticalSpacing = 15;
		composite.setLayout(gl_composite);
		
		list = new List(composite, SWT.BORDER | SWT.V_SCROLL);
		list.setItems(I18n.getString(NewLocalFolders.class.getSimpleName()+".dialog.foldersList").split(","));
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Composite composite_1 = new Composite(composite, SWT.NONE);
		composite_1.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		composite_1.setLayout(new RowLayout(SWT.HORIZONTAL));
		
		Button removeButton = new Button(composite_1, SWT.NONE);
		removeButton.setText(I18n.getString(NewLocalFolders.class.getSimpleName()+".dialog.removeFolder"));
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int idx = list.getSelectionIndex();
				list.remove(idx);
			}
		});
		
		Button addButton = new Button(composite_1, SWT.NONE);
		addButton.setText(I18n.getString(NewLocalFolders.class.getSimpleName()+".dialog.addFolder"));
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				NewAddLocalFolders na = new NewAddLocalFolders(getParent(), SWT.APPLICATION_MODAL);
				String name = (String)na.open();
				
				if (name != null && name.length() > 0) {
					list.add(name);
				}
			}
		});
		
		return composite;
	}

	@Override
	protected void handleAction(WizardAction action) {
		if (action == WizardAction.NEXT){
			this.shell.dispose();
			NewSummaryDialog dialog = new NewSummaryDialog(getWizardParameters(), getParent(), SWT.APPLICATION_MODAL);
			dialog.open();
		}
		else if (action == WizardAction.PREVIOUS){
			this.shell.dispose();
			String pluginClassName = (String)getWizardParameters().get("pluginGuiClassName");
			NewPluginDialog sd = new NewPluginDialog(getWizardParameters(), getParent(), SWT.APPLICATION_MODAL);
			sd.open();
		}
	}

	@Override
	protected boolean validate() {
		// TODO Auto-generated method stub
		return false;
	}
}
