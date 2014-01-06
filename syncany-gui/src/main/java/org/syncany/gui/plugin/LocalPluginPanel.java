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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.syncany.gui.ApplicationResourcesManager;
import org.syncany.gui.panel.PluginPanel;
import org.syncany.util.I18n;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class LocalPluginPanel extends PluginPanel {
	private Text localDir;
	
	public LocalPluginPanel(Composite parent, int style) {
		super(parent, style);
		initComposite();
	}
	
	public void initComposite(){
		Font fontNormal = ApplicationResourcesManager.FONT_NORMAL;
		Font fontBold = ApplicationResourcesManager.FONT_BOLD;
		
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(3, false);
		setLayout(gl_composite);
		
		Label lblNewLabel = new Label(this, SWT.WRAP);
		lblNewLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblNewLabel.setText(I18n.getString("plugin.local.introduction.title"));
		lblNewLabel.setFont(fontBold);
		
		Label lblNewLabel_1 = new Label(this, SWT.WRAP);
		lblNewLabel_1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		lblNewLabel_1.setText(I18n.getString("plugin.local.introduction"));
		
		Label hostLabel = new Label(this, SWT.NONE);
		hostLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		hostLabel.setText(I18n.getString("plugin.local.localFolder", true));
		hostLabel.setFont(fontNormal);
		
		localDir = new Text(this, SWT.BORDER);
		localDir.setFont(fontNormal);
		GridData gd_hostText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_hostText.minimumWidth = 200;
		localDir.setLayoutData(gd_hostText);
		
		Button selectFolderButton = new Button(this, SWT.NONE);
		selectFolderButton.setText("...");
		GridData gd_testFtpButton1 = new GridData(SWT.CENTER, SWT.FILL, false, false, 1, 1);
		gd_testFtpButton1.heightHint = ApplicationResourcesManager.DEFAULT_BUTTON_HEIGHT;
		selectFolderButton.setLayoutData(gd_testFtpButton1);
		selectFolderButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell());
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
		
		testLocalRepositoryButton.setFont(fontNormal);
		testLocalRepositoryButton.setText("Test");
		testLocalRepositoryButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final String folder = localDir.getText();
				Display.getCurrent().syncExec(new Runnable() {
				    public void run() {
				    	if (folder == null || folder.length() == 0){
				    		testResultLabel.setText(I18n.getString("plugin.local.emptyDirectory"));
				    	}
				    	else if (!testLocalDirectory(folder)){
				    		testResultLabel.setText(I18n.getString("plugin.local.notValidDirectory"));
				    	}
				    	else{
				    		testResultLabel.setText(I18n.getString("plugin.local.validDirectory"));
				    	}
				    }
				});
			}
		});
	}	

	@Override
	public Map<String, String> getUserSelection() {
		Map<String, String> parameters = new HashMap<>();
		parameters.put("plugin.local.path", localDir.getText());
		return parameters;
	}
	
	@Override
	public boolean isValid() {
		return testLocalDirectory(localDir.getText());
	}
	
	private boolean testLocalDirectory(String folder){
		File localDirFile = new File(folder);		
		return localDirFile.exists() && localDirFile.isDirectory();
	}
}
