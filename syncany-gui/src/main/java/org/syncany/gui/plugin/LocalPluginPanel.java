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
package org.syncany.gui.plugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.syncany.gui.ApplicationResourcesManager;
import org.syncany.gui.SWTResourceManager;
import org.syncany.gui.UserInput;
import org.syncany.gui.panel.PluginPanel;
import org.syncany.gui.util.FileUtil;
import org.syncany.util.I18n;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class LocalPluginPanel extends PluginPanel {
	private Text localDir;
	
	public LocalPluginPanel(Composite parent, int style){
		super(parent, style);
		initComposite();
	}
	
	public void initComposite(){
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(3, false);
		setLayout(gl_composite);
		
		Label introductionTitleLabel = new Label(this, SWT.WRAP);
		introductionTitleLabel.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));
		introductionTitleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		introductionTitleLabel.setText(I18n.getString("plugin.local.introduction.title"));
		
		Label introductionLabel = new Label(this, SWT.WRAP);
		introductionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		introductionLabel.setText(I18n.getString("plugin.local.introduction"));
		
		Label hostLabel = new Label(this, SWT.NONE);
		GridData gd_hostLabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_hostLabel.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		hostLabel.setLayoutData(gd_hostLabel);
		hostLabel.setText(I18n.getString("plugin.local.localFolder", true));
		
		localDir = new Text(this, SWT.BORDER);
		GridData gd_hostText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_hostText.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		gd_hostText.minimumWidth = 200;
		localDir.setLayoutData(gd_hostText);
		
		Button selectFolderButton = new Button(this, SWT.NONE);
		selectFolderButton.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false, 1, 1));
		selectFolderButton.setImage(SWTResourceManager.getResizedImage("/images/folder-icon.png", 16, 16));
		selectFolderButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog fd = new DirectoryDialog(getShell());
				
				String selectedFolder = fd.open();
				
				if (selectedFolder != null && selectedFolder.length() > 0)
					localDir.setText(selectedFolder);
			}
		});
		
		Composite buttonComposite = new Composite(this, SWT.NONE);
		GridLayout gl_buttonComposite = new GridLayout(2, false);
		gl_buttonComposite.horizontalSpacing = 0;
		gl_buttonComposite.verticalSpacing = 0;
		gl_buttonComposite.marginWidth = 0;
		gl_buttonComposite.marginHeight = 0;
		buttonComposite.setLayout(gl_buttonComposite);
		GridData gd_buttonComposite = new GridData(SWT.FILL, SWT.BOTTOM, true, false, 4, 1);
		gd_buttonComposite.minimumHeight = 30;
		buttonComposite.setLayoutData(gd_buttonComposite);
		
		final Label testResultLabel = new Label(buttonComposite, SWT.NONE);
		testResultLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		testResultLabel.setAlignment(SWT.CENTER);
		
		final Button testLocalRepositoryButton = new Button(buttonComposite, SWT.NONE);

		GridData gd_testFtpButton = new GridData(SWT.CENTER, SWT.FILL, false, false, 1, 1);
		gd_testFtpButton.heightHint = ApplicationResourcesManager.DEFAULT_BUTTON_HEIGHT;
		gd_testFtpButton.widthHint = ApplicationResourcesManager.DEFAULT_BUTTON_WIDTH;
		testLocalRepositoryButton.setLayoutData(gd_testFtpButton);
		
		testLocalRepositoryButton.setText("Test");
		testLocalRepositoryButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				Display.getCurrent().syncExec(new Runnable() {
				    public void run() {
				    	boolean isValid = isValid();
				    	
				    	if (isValid){
				    		testResultLabel.setText(I18n.getString("plugin.local.validDirectory"));
				    	}
				    	else{
				    		testResultLabel.setText(I18n.getString("plugin.local.notValidDirectory"));
				    	}
					}
				});
			}
		});
	}	

	@Override
	public UserInput getUserSelection() {
		UserInput parameters = new UserInput();
		parameters.put(SyncanyLocalParameter.LOCAL_FOLDER, localDir.getText());
		return parameters;
	}
	
	@Override
	public boolean isValid() {
		String folder = localDir.getText();
		String action = getAction();
    	
    	if (folder == null || folder.length() == 0){
    		return false;
    	}
    	
    	if (action.equals("connect")){
    		if (!FileUtil.isExistingFolder(getShell(), folder)){
    			return false;
    		}
    		else {
    			return true;
    		}
    	}
    	else if (action.equals("create")){
    		if (!FileUtil.isExistingAndEmptyFolder(getShell(), folder)){
    			return false;
    		}
    		else {
    			return true;
    		}
    	}
    	return false;
	}
}
