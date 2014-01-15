package org.syncany.gui.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.syncany.gui.ApplicationResourcesManager;
import org.syncany.gui.SWTResourceManager;
import org.syncany.gui.SyncanyCommandParameters;
import org.syncany.gui.UserInput;
import org.syncany.gui.util.FileUtil;
import org.syncany.util.I18n;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class SelectLocalFolder extends WizardPanelComposite {
	private Text localDir;
	private Label introductionLabel;
	
	public SelectLocalFolder(WizardDialog wizardParentDialog, Composite parent, int style) {
		super(wizardParentDialog, parent, style);
		initComposite();
	}
	
	public void initComposite(){
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(3, false);
		setLayout(gl_composite);
		
		Label introductionTitleLabel = new Label(this, SWT.WRAP);
		introductionTitleLabel.setFont(org.eclipse.wb.swt.SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));
		introductionTitleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		introductionTitleLabel.setText(I18n.getString("dialog.selectLocalFolder.introduction.title"));
		
		introductionLabel = new Label(this, SWT.WRAP);
		introductionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		introductionLabel.setText(I18n.getString("dialog.selectLocalFolder.introduction"));
		
		Label hostLabel = new Label(this, SWT.WRAP);
		GridData gd_hostLabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_hostLabel.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		hostLabel.setLayoutData(gd_hostLabel);
		hostLabel.setText(I18n.getString("dialog.selectLocalFolder.selectLocalFolder", true));
		
		localDir = new Text(this, SWT.BORDER);
		GridData gd_hostText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_hostText.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		gd_hostText.minimumWidth = 200;
		localDir.setLayoutData(gd_hostText);
		
		Button selectFolderButton = new Button(this, SWT.FLAT);
		selectFolderButton.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false, 1, 1));
		selectFolderButton.setImage(SWTResourceManager.getResizedImage("/images/folder-icon.png", 16, 16));
		selectFolderButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog fd = new DirectoryDialog(getShell());
				String selectedFolder = fd.open();
				
				if (selectedFolder != null && selectedFolder.length() > 0){
					localDir.setText(selectedFolder);
				}
			}
		});
	}	

	@Override
	public UserInput getUserSelection() {
		UserInput parameters = new UserInput();
		parameters.put(SyncanyCommandParameters.LOCAL_FOLDER, localDir.getText());
		return parameters;
	}
	
	@Override
	public boolean isValid() {
		String action = getParentWizardDialog().getUserInput().get(SyncanyCommandParameters.COMMAND_ACTION);
		
		switch (action){
			case "watch":
				//test if .syncany folder exists
				return FileUtil.isSyncanyFolder(getShell(), localDir.getText());
			case "create":
				//test if folder exists && is empty
				return FileUtil.isExistingAndEmptyFolder(getShell(), localDir.getText());
			case "connect":
				// test if folder exists only : folder might be full of files
				return FileUtil.isExistingFolder(getShell(), localDir.getText());
		}
		return false;
	}

	@Override
	public boolean hasNextButton() {
		return true;
	}

	@Override
	public boolean hasPreviousButton() {
		return true;
	}

	@Override
	public boolean hasFinishButton() {
		return false;
	}

	@Override
	public void updateData() {
		String action = getParentWizardDialog().getUserInput().get(SyncanyCommandParameters.COMMAND_ACTION);
		if (action.equals("watch")){
			getParentWizardDialog().updateFinishButton(true);
			getParentWizardDialog().updateNextButton(false);
		}
		else{
			getParentWizardDialog().updateFinishButton(false);
			getParentWizardDialog().updateNextButton(true);
		}

		introductionLabel.setText(I18n.getString(String.format("dialog.selectLocalFolder.%sIntroduction", action)));
	}
}
