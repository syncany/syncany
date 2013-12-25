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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.syncany.gui.messaging.ClientCommandFactory;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class NewSummaryDialog extends DefaultWizardPanel {
	private static final Logger log = Logger.getLogger(NewSummaryDialog.class.getSimpleName());
	
	private Label contentLabel;
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public NewSummaryDialog(Map<String, Object> params, Shell parent, int style) {
		super(params, WizardType.CREATE | WizardType.PREVIOUS, parent, style);
	}
	
	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		log.fine("creating NewSummaryDialog wizard panel");
		super.createContents();
		
		updateContent();
		
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
	
	private void updateContent() {
		StringBuilder sb = new StringBuilder();
		
		for (String key : getWizardParameters().keySet()){
			sb.append(key+"="+getWizardParameters().get(key));
			sb.append("\n");
		}
		
		contentLabel.setText(sb.toString());
	}

	protected Composite createComposite(Shell shell){
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		
		contentLabel = new Label(composite, SWT.WRAP);
		contentLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		contentLabel.setText("New Label");
		
		return composite;
	}

	@Override
	protected void handleAction(WizardAction action) {
		if (action == WizardAction.CREATE){
			this.shell.dispose();	
			ClientCommandFactory.create(getWizardParameters());
		}
		else if (action == WizardAction.PREVIOUS){
			this.shell.dispose();
			NewLocalFolders dialog = new NewLocalFolders(getWizardParameters(), getParent(), SWT.APPLICATION_MODAL);
			dialog.open();
		}
	}
	
	@Override
	protected boolean validate() {
		return true;
	}
}
