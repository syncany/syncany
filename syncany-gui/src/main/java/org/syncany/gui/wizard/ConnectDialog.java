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

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.syncany.gui.util.I18n;
import org.syncany.gui.util.SWTResourceManager;
import org.syncany.gui.wizard.core.DefaultWizardPanel;
import org.syncany.gui.wizard.core.WizardAction;
import org.syncany.gui.wizard.core.WizardType;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class ConnectDialog extends DefaultWizardPanel implements ModifyListener {
	private Text folderTextField;
	private Text urlTextField;
	private Label messageLabel;
	private Label urlMessageLabel ;
	private Label folderMessageLabel;
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public ConnectDialog(Shell parent, int style) {
		super(WizardType.CONNECT | WizardType.PREVIOUS, parent, style);
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
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(2, false);
		gl_composite.verticalSpacing = 15;
		composite.setLayout(gl_composite);
		
		Composite folderComposite = new Composite(composite, SWT.NONE);
		folderComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		GridLayout gl_folderComposite = new GridLayout(2, false);
		gl_folderComposite.verticalSpacing = 0;
		folderComposite.setLayout(gl_folderComposite);
		
		Label folderLabel = new Label(folderComposite, SWT.NONE);
		GridData gd_folderLabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_folderLabel.widthHint = 70;
		folderLabel.setLayoutData(gd_folderLabel);
		folderLabel.setText(I18n.getString("ConnectDialog.dialog.localFolder"));
		
		folderTextField = new Text(folderComposite, SWT.BORDER);
		folderTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		folderTextField.setSize(131, 21);
		folderTextField.setText("New Synced Folder");
		folderTextField.addModifyListener(this);
		
		folderMessageLabel = new Label(folderComposite, SWT.NONE);
		folderMessageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		folderMessageLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
		
		Composite urlComposite = new Composite(composite, SWT.NONE);
		urlComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		GridLayout gl_urlComposite = new GridLayout(2, false);
		gl_urlComposite.verticalSpacing = 0;
		urlComposite.setLayout(gl_urlComposite);
		
		Label urlLabel = new Label(urlComposite, SWT.NONE);
		GridData gd_urlLabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_urlLabel.widthHint = 70;
		urlLabel.setLayoutData(gd_urlLabel);
		urlLabel.setText(I18n.getString("ConnectDialog.dialog.url"));
		
		urlTextField = new Text(urlComposite, SWT.BORDER);
		urlTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		urlTextField.setSize(131, 21);
		urlTextField.addModifyListener(this);
		
		urlMessageLabel = new Label(urlComposite, SWT.NONE);
		urlMessageLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		urlMessageLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));

		Composite messageComposite = new Composite(composite, SWT.NONE);
		messageComposite.setLayout(new GridLayout(1, false));
		messageComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		
		messageLabel = new Label(messageComposite, SWT.WRAP);
		messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		messageLabel.setSize(175, 15);
		messageLabel.setText(I18n.getString("ConnectDialog.dialog.baseFolder", (String)System.getProperties().get("user.home")+"\\Syncany"));
		
		return composite;
	}

	@Override
	protected void handleAction(WizardAction action) {
		if (action == WizardAction.CONNECT){
			boolean isFolderValid = validateFolderInput();
			boolean isUrlValid = validateUrlInput();
			
			if (isFolderValid && isUrlValid){
				String folderName = folderTextField.getText();
				String fullFolderPath = (String)System.getProperties().get("user.home") + File.separator + "Syncany" +File.separator + folderName;
				
				File f = new File(fullFolderPath);
				f.mkdir();
				
				shell.dispose();
			}
		}
		else if (action == WizardAction.PREVIOUS){
			this.shell.dispose();
			StartDialog sd = new StartDialog(getParent(), SWT.APPLICATION_MODAL);
			sd.open();
		}
	}

	@Override
	public void modifyText(ModifyEvent e) {
		if (e.widget == urlTextField){
			validateUrlInput();
		}
		else if (e.widget == folderTextField){
			validateFolderInput();
		}
		folderMessageLabel.update();
	}
	
	private boolean validateUrlInput(){
		String url = urlTextField.getText();
		
		if (url == null || url.length() < 5){
			urlMessageLabel.setText("Entered URL is invalid");
			return false;
		}
		else{
			urlMessageLabel.setText("");
			return true;
		}
	}
	
	private boolean validateFolderInput(){
		String folderName = folderTextField.getText();
		String fullFolderPath = (String)System.getProperties().get("user.home") + File.separator + "Syncany" +File.separator + folderName;
		
		if (folderName != null && folderName.length() > 0){
			File f = new File(fullFolderPath);
			if (f.exists()){
				getConnectButton().setEnabled(false);
				folderMessageLabel.setText(I18n.getString("ConnectDialog.dialog.folderAlreadyExists", folderName));
				return false;
			}
			else{
				getConnectButton().setEnabled(true);
				folderMessageLabel.setText("");
				return true;
			}
		}
		return false;
	}
}
