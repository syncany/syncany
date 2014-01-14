package org.syncany.gui.wizard;

import java.io.File;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.syncany.gui.ApplicationResourcesManager;
import org.syncany.gui.SWTResourceManager;
import org.syncany.gui.SyncanyCommandParameters;
import org.syncany.gui.UserInput;
import org.syncany.util.I18n;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class SelectLocalFolder extends WizardPanelComposite {
	private Text localDir;
	
	public SelectLocalFolder(WizardDialog wizardParentDialog, Composite parent, int style) {
		super(wizardParentDialog, parent, style);
		initComposite();
	}
	
	public void initComposite(){
		Font fontNormal = ApplicationResourcesManager.FONT_NORMAL;
		Font fontBold = ApplicationResourcesManager.FONT_BOLD;
		
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(3, false);
		setLayout(gl_composite);
		
		Label introductionTitleLabel = new Label(this, SWT.WRAP);
		introductionTitleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		introductionTitleLabel.setText(I18n.getString("dialog.selectLocalFolder.introduction.title"));
		introductionTitleLabel.setFont(fontBold);
		
		Label introductionLabel = new Label(this, SWT.WRAP);
		introductionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		introductionLabel.setText(I18n.getString("dialog.selectLocalFolder.introduction"));
		introductionLabel.setFont(fontNormal);
		
		Label hostLabel = new Label(this, SWT.NONE);
		GridData gd_hostLabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_hostLabel.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		hostLabel.setLayoutData(gd_hostLabel);
		hostLabel.setText(I18n.getString("dialog.selectLocalFolder.selectLocalFolder", true));
		hostLabel.setFont(fontNormal);
		
		localDir = new Text(this, SWT.BORDER);
		localDir.setFont(fontNormal);
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
		return testLocalDirectory(localDir.getText());
	}
	
	private boolean testLocalDirectory(String folder){
		if (folder.length() == 0) return false;
		
		try {
			new File(folder).getCanonicalPath();
		}
		catch (IOException e) {
			return false;
		}
		
		File localDirFile = new File(folder);			

		if (localDirFile.exists()){
			if (localDirFile.list().length > 0){
				MessageBox dialog = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.CANCEL);
				dialog.setText("Warning");
				dialog.setMessage("Folder is not empty, please choose an empty folder");
				dialog.open();
				
				localDir.setText(localDirFile.getAbsolutePath());
				return false;
			}
			return true;
		}
		else{
			MessageBox dialog = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);
			dialog.setText("Create Folder");
			dialog.setMessage(String.format("Would you like to create the folder [%s]?", localDirFile.getAbsolutePath()));
			
			int ret = dialog.open();
			
			if (ret == SWT.OK){
				boolean mkdirSuccess = localDirFile.mkdir();
				
				if (mkdirSuccess){
					localDir.setText(localDirFile.getAbsolutePath());
					return true;
				}
				return false;
			}
			return	false;
		}
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
		
	}
}
