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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.syncany.gui.ApplicationResourcesManager;
import org.syncany.gui.CommonParameters;
import org.syncany.gui.UserInput;
import org.syncany.gui.WidgetDecorator;
import org.syncany.gui.util.BrowserHelper;
import org.syncany.util.I18n;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class SummaryPanel extends WizardPanelComposite {
	private Label repositoryType;
	private Label localFolder;
	private Label encryptionType;
	private Label summaryIntroductionTitleLabel;
	private ProgressBar progressBar;
	
	private StackLayout stackLayout;
	private Composite stackComposite;
	private Composite successInitComposite;
	private Composite failureComposite;
	private Composite emptyComposite;
	private Label sucessInitLabel;
	private Button initOpenSyncanyFolderButton; 
	private Composite composite;
	private Button initCloseWizardButton;
	private Label summaryIntroductionLabel;
	private Label urlLabel;
	private Label repositoryTypeLabel;
	private Label encryptionTypeLabel;
	
	public SummaryPanel(WizardDialog wizardParentDialog, Composite parent, int style) {
		super(wizardParentDialog, parent, style);
		initComposite();
	}
	
	private void initComposite(){
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.horizontalSpacing = 8;
		setLayout(gridLayout);
		
		summaryIntroductionTitleLabel = new Label(this, SWT.NONE);
		summaryIntroductionTitleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		summaryIntroductionTitleLabel.setText(I18n.getString("dialog.summary.introduction.title"));
		
		summaryIntroductionLabel = new Label(this, SWT.NONE);
		summaryIntroductionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		summaryIntroductionLabel.setText(I18n.getString("dialog.summary.introduction"));
		
		repositoryTypeLabel = new Label(this, SWT.NONE);
		GridData gd_repositoryTypeLabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_repositoryTypeLabel.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		repositoryTypeLabel.setLayoutData(gd_repositoryTypeLabel);
		repositoryTypeLabel.setText(I18n.getString("dialog.summary.repositoryType", true));
		
		repositoryType = new Label(this, SWT.NONE);
		GridData gd_repositoryType = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_repositoryType.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		repositoryType.setLayoutData(gd_repositoryType);
		
		Label localFolderLabel = new Label(this, SWT.NONE);
		localFolderLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		localFolderLabel.setText(I18n.getString("dialog.summary.localFolder", true));
		
		localFolder = new Label(this, SWT.NONE);
		localFolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		
		encryptionTypeLabel = new Label(this, SWT.NONE);
		encryptionTypeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		encryptionTypeLabel.setText(I18n.getString("dialog.summary.encryptionSettings", true));
		
		encryptionType = new Label(this, SWT.NONE);
		encryptionType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		
		progressBar = new ProgressBar(this, SWT.INDETERMINATE);
		GridData gd_progressBar = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_progressBar.verticalIndent = 50;
		progressBar.setLayoutData(gd_progressBar);
		progressBar.setState(SWT.PAUSED);
		
		stackComposite = new Composite(this, SWT.NONE);
		stackLayout = new StackLayout();
		stackComposite.setLayout(stackLayout);
		stackComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		successInitComposite = new Composite(stackComposite, SWT.NONE);
		successInitComposite.setLayout(new GridLayout(1, false));
		
		sucessInitLabel = new Label(successInitComposite, SWT.NONE);
		sucessInitLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));
		sucessInitLabel.setText("Repository sucessfully created");
		
		urlLabel = new Label(successInitComposite, SWT.WRAP);
		urlLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true, 1, 1));
		urlLabel.setText("Click here to copy syncany url into Clipboard");
		urlLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				try{
					Toolkit toolkit = Toolkit.getDefaultToolkit();
					Clipboard clipboard = toolkit.getSystemClipboard();
					StringSelection strSel = new StringSelection(link);
					clipboard.setContents(strSel, null);
				}
				catch (Exception ex){ }
			}
		});
		
		composite = new Composite(successInitComposite, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));
		composite.setLayout(new RowLayout(SWT.HORIZONTAL));
		
		initOpenSyncanyFolderButton = new Button(composite, SWT.NONE);
		initOpenSyncanyFolderButton.setText("Open Syncany Folder");
		
		initCloseWizardButton = new Button(composite, SWT.NONE);
		initCloseWizardButton.setText("Close this windows");
		initCloseWizardButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getParentWizardDialog().safeDispose();
			}
		});
		
		initOpenSyncanyFolderButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getParentWizardDialog().safeDispose();
				String file = getParentWizardDialog().getUserInput().getCommonParameter(CommonParameters.LOCAL_FOLDER);
				BrowserHelper.openFile(file);
			}
		});
		
		successConnectComposite = new Composite(stackComposite, SWT.NONE);
		successConnectComposite.setLayout(new GridLayout(1, false));
		
		successConnectLabel = new Label(successConnectComposite, SWT.NONE);
		successConnectLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));
		successConnectLabel.setText("Repository sucessfully created");
		
		successUrlLabel = new Label(successConnectComposite, SWT.WRAP);
		successUrlLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true, 1, 1));
		successUrlLabel.setText("Click here to copy syncany url into Clipboard");
		
		composite_2 = new Composite(successConnectComposite, SWT.NONE);
		composite_2.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));
		composite_2.setLayout(new RowLayout(SWT.HORIZONTAL));
		
		connectOpenSyncanyFolderButton = new Button(composite_2, SWT.NONE);
		connectOpenSyncanyFolderButton.setText("Open Syncany Folder");
		
		connectCloseWizardButton = new Button(composite_2, SWT.NONE);
		connectCloseWizardButton.setText("Close this windows");
		
		failureComposite = new Composite(stackComposite, SWT.NONE);
		failureComposite.setLayout(new GridLayout(1, false));
		
		Label errorInitLabel = new Label(failureComposite, SWT.NONE);
		errorInitLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true, 1, 1));
		errorInitLabel.setText("An error occured during repository initialisation\r\nClick \"Retry\" to retry repository configuration");
		
		emptyComposite = new Composite(stackComposite, SWT.NONE);
		
		stackLayout.topControl = emptyComposite;
		failureComposite.layout();
		
		WidgetDecorator.normal(
			repositoryType, localFolder, encryptionType, urlLabel, 
			initOpenSyncanyFolderButton, initCloseWizardButton,
			connectOpenSyncanyFolderButton, connectCloseWizardButton
		);
		WidgetDecorator.bold(
			successConnectLabel, errorInitLabel, successConnectLabel,
			summaryIntroductionTitleLabel, 
			encryptionTypeLabel, repositoryTypeLabel, localFolderLabel
		);
	}
	
	public void updateData(){
		final UserInput userInput = getParentWizardDialog().getUserInput();
		
		getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				String type = String.format("%s", userInput.getCommonParameter(CommonParameters.COMMAND_ACTION));
				String encryption = String.format("[%s / %s]", userInput.getCommonParameter(CommonParameters.ENCRYPTION_ALGORITHM), userInput.getCommonParameter(CommonParameters.ENCRYPTION_KEYLENGTH));
				String folder = String.format("%s", userInput.getCommonParameter(CommonParameters.LOCAL_FOLDER));
				repositoryType.setText(type);
				localFolder.setText(folder);
				encryptionType.setText(encryption);
			}
		});
	}	
		
	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public UserInput getUserSelection() {
		return new UserInput();
	}

	@Override
	public boolean hasNextButton() {
		return false;
	}

	@Override
	public boolean hasPreviousButton() {
		return true;
	}

	@Override
	public boolean hasFinishButton() {
		return true;
	}

	public void startIndeterminateProgressBar() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				progressBar.setState(SWT.NORMAL);
			}
		});
		
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				stopIndeterminateProgressBar();
			}
		}, 1000*5);
	}

	public void stopIndeterminateProgressBar() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (!progressBar.isDisposed() && progressBar.getState() == SWT.NORMAL){
					progressBar.setState(SWT.PAUSED);
				}
			}
		});
	}

	public void showErrorMessage(){
		stackLayout.topControl = failureComposite;
		stackComposite.layout();
	}
	
	private String link;
	private Composite successConnectComposite;
	private Label successConnectLabel;
	private Label successUrlLabel;
	private Composite composite_2;
	private Button connectOpenSyncanyFolderButton;
	private Button connectCloseWizardButton;
	public void showSuccessMessage(String link, boolean linkEncrypted){
		this.link = link;
		stackLayout.topControl = successInitComposite;
		stackComposite.layout();
	}
}
