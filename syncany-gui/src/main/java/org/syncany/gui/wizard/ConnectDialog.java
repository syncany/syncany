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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.syncany.gui.command.ClientCommandFactory;
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
		
		urlMessageLabel = new Label(urlComposite, SWT.WRAP);
		urlMessageLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
		urlMessageLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));

		Composite messageComposite = new Composite(composite, SWT.NONE);
		GridLayout gl_messageComposite = new GridLayout(1, false);
		gl_messageComposite.marginWidth = 0;
		gl_messageComposite.marginHeight = 0;
		gl_messageComposite.verticalSpacing = 0;
		messageComposite.setLayout(gl_messageComposite);
		messageComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		messageLabel = new Label(messageComposite, SWT.BORDER | SWT.WRAP);
		messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		messageLabel.setSize(175, 15);
		messageLabel.setText(I18n.getString("ConnectDialog.dialog.baseFolder", getBaseFolder()));
		
		return composite;
	}

	@Override
	protected void handleAction(WizardAction action) {
		if (action == WizardAction.CONNECT){
			boolean isFolderValid = validateFolderInput();
			boolean isUrlValid = validateUrlInput();
			
			if (isFolderValid && isUrlValid){
				String fullFolderPath = getFolderName();
				ClientCommandFactory.connect(urlTextField.getText(), fullFolderPath);
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
	
	
	private static final Pattern LINK_PATTERN = Pattern.compile("^syncany://storage/1/(?:(not-encrypted/)(.+)|([^-]+-(.+)))$");
	
	private boolean validateUrlInput(){
		String url = urlTextField.getText();
		
		Matcher linkMatcher = LINK_PATTERN.matcher(url);
		
		if (!linkMatcher.matches()) {
			urlMessageLabel.setText("Invalid link provided, must start with syncany:// and match link pattern.");
			return false;
		}
		else{
			urlMessageLabel.setText("");
			return true;
		}
	}
	
	private String getFolderName(){
		String folderName = folderTextField.getText();
		return getBaseFolder() + File.separator + folderName;
	}
	
	private String getBaseFolder(){
		return (String)System.getProperties().get("user.home") + File.separator + "Syncany" +File.separator;
	}
	
	private boolean validateFolderInput(){
		String folderName = folderTextField.getText();
		String fullFolderPath = getFolderName();
		
		if (folderName != null && folderName.length() > 0){
			File f = new File(fullFolderPath);
			if (f.exists()){
				getConnectButton().setEnabled(false);
				folderMessageLabel.setText(I18n.getString("ConnectDialog.dialog.folderAlreadyExists", folderName));
				getParent().layout();
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
